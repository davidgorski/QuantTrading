package com.quantTrading.alphaVantage.intraday


import akka.http.scaladsl.model._
import com.quantTrading.aws.S3
import com.quantTrading.config.Config
import com.quantTrading.symbols.QtSymbol
import com.quantTrading.Utils
import com.typesafe.scalalogging.StrictLogging
import org.scalactic.anyvals.{PosDouble, PosInt, PosZDouble, PosZInt}
import requests.Response
import scalaz.Validation
import ujson.Value.Value
import spray.json._
import spray.json.DefaultJsonProtocol._
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, ZoneId, ZonedDateTime}
import java.util.concurrent.{Executors, TimeUnit}
import scala.collection.immutable.{Map => ImmutableMap}
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext, Future}


object HistoryLoadCache extends StrictLogging {

  case class QueryParams(
    symbol: QtSymbol,
    monthString: String,
    minuteBarInterval: MinuteBarInterval,
    zoneId: ZoneId,
    nRetries: PosZInt
  ) {
    def getKey: String = s"${symbol.symbol}_${monthString}_${minuteBarInterval.toString}.json"
  }

  def queryApiRange(
    symbols: Set[QtSymbol],
    sd: LocalDate,
    ed: LocalDate,
    minuteBarInterval: MinuteBarInterval,
    zoneId: ZoneId,
    nRetries: PosZInt,
    nConcurrentQueriesS3: PosInt,
    nConcurrentQueriesAlphaVantage: PosInt,
    config: Config
  ): Validation[String, List[IntradayOhlcv]] = {

    // don't query for this month
    val firstDayOfMonth = LocalDate.now(config.zoneId).withDayOfMonth(1) // get the first day of today's month
    require(ed.isBefore(firstDayOfMonth), s"ed should be before the first day of this month (don't put this month in intraday history)")

    // what do we need to query for
    val queryParamSets: Set[QueryParams] =
      symbols.flatMap { symbol =>
        getMonthStrings(symbol, sd, ed).map { monthString =>
          QueryParams(symbol, monthString, minuteBarInterval, zoneId, nRetries)
        }
      }

    // =================================================================================================================
    // try to get the data from s3 first
    val s3Client = new S3(config.awsRegion)
    val keysNeeded: Set[String] = queryParamSets.map(_.getKey)
    val keysInBucket: Set[String] = s3Client.getAllKeysInBucket(config.awsS3Bucket)
    val missingKeys: Set[String] = keysNeeded.diff(keysInBucket)
    val missingQueryParams: Set[QueryParams] = queryParamSets.filter(queryParams => missingKeys.contains(queryParams.getKey))
    val foundKeys: List[String] = keysNeeded.intersect(keysInBucket).toList

    val fixedThreadPoolS3 = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(nConcurrentQueriesS3.value))
    val listOfFuturesS3: Future[List[Validation[String, List[IntradayOhlcv]]]] =
      Future.traverse(foundKeys) { (foundKey: String) =>
        Future {
          val jsonForKey: Validation[String, String] = s3Client.readJson(config.awsS3Bucket, foundKey)
          val result: Validation[String, List[IntradayOhlcv]] = jsonForKey match {
            case scalaz.Failure(s: String) =>
              scalaz.Failure(s)
            case scalaz.Success(jsonStr: String) =>
              try {
                scalaz.Success(jsonStr.parseJson.convertTo[List[IntradayOhlcv]])
              } catch {
                case _: Throwable => scalaz.Failure("Could not convert s3 json files into IntradayOhlcv")
              }
          }
          result
        } (fixedThreadPoolS3)
      } (implicitly, fixedThreadPoolS3)
    val allResultsS3: List[Validation[String, List[IntradayOhlcv]]] =
      Await.result(listOfFuturesS3, FiniteDuration(10, TimeUnit.MINUTES))
    val allResultsS3Failures = allResultsS3.collect { case scalaz.Failure(fString: String) => fString }
    val s3Successes: List[IntradayOhlcv] = {
      if (allResultsS3Failures.nonEmpty) {
        val failureString = allResultsS3Failures.mkString(" | ")
        return scalaz.Failure(failureString) // terminate early
      } else {
        val successes: List[IntradayOhlcv] = allResultsS3.flatMap { case scalaz.Success(intradayOhlcv: List[IntradayOhlcv]) => intradayOhlcv }
        successes
      }
    }
    logger.info(s"Loaded ${s3Successes.size} rows from s3")

    // =================================================================================================================
    // if not in s3, get from alphavantage
    val fixedThreadPoolAlphaVantage = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(nConcurrentQueriesAlphaVantage.value))

    // (1) we can run up to [nConcurrentQueries] queries concurrently (due to alphavantage)
    // (2) we can run up to 150/minute (per our alphavantage subscription)
    // use fixedThreadPoolAlphaVantage to handle (1) and use thread blocking to handle (2)
    // nQueriesPerMin = nConcurrentQueries * queryPerSec * 60 sec/min
    // solve for queryPerSec s.t. nQueriesPerMin is 150
    // queryPerSec = nQueriesPerMin / nConcurrentQueries / 60
    // millisecPerQuery = 1000/queryPerSec

    val queriesPerSec = 150.0 / nConcurrentQueriesAlphaVantage / 60.0
    val sleepTimeMilli: Int = math.round(1000 / queriesPerSec).toInt

    logger.info(s"Loading ${missingQueryParams.size} rows from alphaVantage")
    val listOfFuturesAlphaVantage: Future[Set[(QueryParams, Validation[String, List[IntradayOhlcv]])]] =
      Future.traverse(missingQueryParams) { (queryParams: QueryParams) =>
        Future {
          val result: Validation[String, List[IntradayOhlcv]] = queryApi(queryParams, config)
          logger.info(s"$queryParams")
          Thread.sleep(sleepTimeMilli)
          (queryParams, result)
        } (fixedThreadPoolAlphaVantage)
      } (implicitly, fixedThreadPoolAlphaVantage)

    val allResultsAlphaVantage: Set[(QueryParams, Validation[String, List[IntradayOhlcv]])] =
      Await.result(listOfFuturesAlphaVantage, FiniteDuration(2, TimeUnit.HOURS))

    val alphaVantageFailures: Set[String] = allResultsAlphaVantage.collect { case (_, scalaz.Failure(fString: String)) => fString }
    val alphaVantageSuccessesByKey: Set[(QueryParams, List[IntradayOhlcv])] =
      if (alphaVantageFailures.nonEmpty) {
        val failureString = alphaVantageFailures.mkString(" | ")
        return scalaz.Failure(failureString)
      } else {
        val alphaVantageSuccesses: Set[(QueryParams, List[IntradayOhlcv])] = allResultsAlphaVantage.map {
          case (queryParams: QueryParams, scalaz.Success(intradayOhlcv: List[IntradayOhlcv])) => (queryParams, intradayOhlcv)
        }
        alphaVantageSuccesses
      }
    val alphaVantageSuccesses: Set[IntradayOhlcv] = alphaVantageSuccessesByKey.flatMap(_._2)
    logger.info(s"Done - loaded ${alphaVantageSuccesses.size} rows from alphaVantage")

    // =================================================================================================================
    // write alphavantage to s3
    alphaVantageSuccessesByKey.foreach { case (queryParams: QueryParams, intradayOhlcvList: List[IntradayOhlcv]) =>
      val jsonStr: String = intradayOhlcvList.toJson.compactPrint
      s3Client.writeJson(config.awsS3Bucket, queryParams.getKey, jsonStr)
    }
    logger.info(s"Stored ${alphaVantageSuccessesByKey.size} rows from alphaVantage to s3")

    // =================================================================================================================
    // combine and return
    val successes = alphaVantageSuccesses ++ s3Successes
    scalaz.Success(successes.toList)
  }

  /**
   * Get all the month strings like 2020-01, 2020-02, ....
   * This gets passed into the api url
   *
   * @return
   */
  private def getMonthStrings(symbol: QtSymbol, sd: LocalDate, ed: LocalDate): List[String] = {
    require(sd.isBefore(ed), "Start date must be before end date")

    val buffer = ListBuffer[String]()
    var d = if (symbol.startDate.isAfter(sd)) symbol.startDate else sd
    d = LocalDate.of(d.getYear, d.getMonth, 1)
    while (!d.isAfter(ed)) {
      buffer += d.format(DateTimeFormatter.ofPattern("yyyy-MM"))
      d = d.plusMonths(1)
    }
    buffer.toSet.toList.sorted
  }

  def queryApi(
    queryParams: QueryParams,
    config: Config,
  ): Validation[String, List[IntradayOhlcv]] = {
    val uriBase: String = "https://www.alphavantage.co/query"
    val uriParams: ImmutableMap[String, String] = ImmutableMap[String, String](
      "function" -> "TIME_SERIES_INTRADAY",
      "symbol" -> queryParams.symbol.alphaVantage,
      "apikey" -> config.awsSecrets.alphaVantageApiKey,
      "extended_hours" -> "false",
      "outputsize" -> "full",
      "interval" -> queryParams.minuteBarInterval.toString,
      "month" -> queryParams.monthString,
      "adjusted" -> "false",
      "datatype" -> "json",
      "entitlement" -> "realtime"
    )
    val uri: Uri = Uri(uriBase).withQuery(Uri.Query(uriParams))
    val apiResponseMaybe =
      queryApiInner(
        uri,
        queryParams.symbol,
        queryParams.monthString,
        queryParams.zoneId,
        queryParams.minuteBarInterval,
        queryParams.nRetries
      )
    apiResponseMaybe
  }

  private def queryApiInner(
    uri: Uri,
    symbol: QtSymbol,
    monthString: String,
    zoneId: ZoneId,
    minuteBarInterval: MinuteBarInterval,
    nRetries: PosZInt,
    readTimeoutMillis: Int = 20000,
    connectTimeoutMillis: Int = 5000
  ): Validation[String, List[IntradayOhlcv]] = {

    val response: Response =
      try {
        requests.get(
          uri.toString(),
          readTimeout = readTimeoutMillis,
          connectTimeout = connectTimeoutMillis
        )
      } catch {
        case e: Throwable =>
          if (nRetries > 0)
            return queryApiInner(uri, symbol, monthString, zoneId, minuteBarInterval, PosZInt.from(nRetries - 1).get, readTimeoutMillis, connectTimeoutMillis)
          else
            return scalaz.Failure(s"($symbol, $monthString), unexpected failure: ${e.getMessage}")
      }

    if (response.statusCode != 200) {
      if (nRetries > 0)
        queryApiInner(uri, symbol, monthString, zoneId, minuteBarInterval, PosZInt.from(nRetries - 1).get, readTimeoutMillis, connectTimeoutMillis)
      else
        scalaz.Failure(s"($symbol, $monthString), statusCode=${response.statusCode}; statusMessage=${response.statusMessage}")
    } else {
      try {
        parseString(response.text(), symbol, zoneId, minuteBarInterval)
          .fold[Validation[String, List[IntradayOhlcv]]](
            x => scalaz.Failure(s"$x $uri"),
            x => scalaz.Success(x)
          )
      } catch {
        case e: Throwable =>
          scalaz.Failure(s"($symbol, $monthString), error=${e.getMessage} uri=$uri")
      }
    }
  }

  private def extractDouble(v: Value, field: String): Double = {
    v(field).value.toString.toDouble
  }

  private def parseString(
    s: String,
    symbol: QtSymbol,
    zoneId: ZoneId,
    minuteBarInterval: MinuteBarInterval
  ): Validation[String, List[IntradayOhlcv]] = {
    try {
      val json: Value = ujson.read(s)
      json.obj.get(minuteBarInterval.getResponseKey) match {
        case None =>
          scalaz.Failure(s"${minuteBarInterval.getResponseKey} not found in json for ($symbol):\n\n$json")
        case Some(values) =>
          val intradayOhlcvBars: List[IntradayOhlcv] =
            values.obj.map { kv =>
              val k: String = kv._1
              val v: Value = kv._2

              val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
              val localDateTime: LocalDateTime = LocalDateTime.parse(k, formatter)
              val zonedDateTimeRaw: ZonedDateTime = ZonedDateTime.of(localDateTime, zoneId)
              val zonedDateTime: ZonedDateTime = zonedDateTimeRaw.plusMinutes(minuteBarInterval.minutes)

              // round when adjusting so we don't have pesky floating point precision issues
              val open: PosDouble = PosDouble.from(Utils.round(extractDouble(v, "1. open"), 4)).get
              val high: PosDouble = PosDouble.from(Utils.round(extractDouble(v, "2. high"), 4)).get
              val low: PosDouble = PosDouble.from(Utils.round(extractDouble(v, "3. low"), 4)).get
              val close: PosDouble = PosDouble.from(Utils.round(extractDouble(v, "4. close"), 4)).get
              val volume: Option[PosZDouble] = Some(PosZDouble.from(extractDouble(v, "5. volume")).get)

              val intradayOhlcv: IntradayOhlcv = IntradayOhlcv(zonedDateTime, symbol, open, high, low, close, volume)
              intradayOhlcv
            }
            .toList
          scalaz.Success(intradayOhlcvBars)
      }

    } catch {
      case e: Throwable =>
        scalaz.Failure(s"Unhandled exception when parsing: ${e.getMessage}")
    }
  }
}
