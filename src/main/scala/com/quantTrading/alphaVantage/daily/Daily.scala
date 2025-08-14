package com.quantTrading.alphaVantage.daily

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.quantTrading.Utils
import com.quantTrading.config.Config
import com.quantTrading.symbols.Symbol
import org.scalactic.anyvals.{PosDouble, PosZDouble, PosZInt}
import scalaz.Validation
import ujson.Value.Value
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}
import scala.collection.immutable.{Map => ImmutableMap}
import scala.concurrent.{ExecutionContext, Future}


object Daily {

  def getFlow(
    symbols: List[Symbol],
    config: Config,
    nRetries: PosZInt
  )(
    implicit
    actorSystem: ActorSystem,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Flow[Instant, Validation[String, List[DailyOhlcv]], NotUsed] = {

    val queryFlow: Flow[Instant, List[Validation[String, List[DailyOhlcv]]], NotUsed] =
      Flow[Instant]
        .mapAsyncUnordered(10) { _ =>
          Future.traverse(symbols)(symbol => queryApi(symbol, nRetries, config))
        }

    val queryCollectFlow: Flow[List[Validation[String, List[DailyOhlcv]]], Validation[String, List[DailyOhlcv]], NotUsed] =
      Flow[List[Validation[String, List[DailyOhlcv]]]]
        .map { (listOfValidations: List[Validation[String, List[DailyOhlcv]]]) =>
          val fStrings = listOfValidations.collect { case scalaz.Failure(fStr: String) => fStr }
          if (fStrings.nonEmpty)
            scalaz.Failure(fStrings.mkString("\n"))
          else {
            val dailyResponses: List[DailyOhlcv] = listOfValidations.flatMap {
              case scalaz.Success(dailyResponse: List[DailyOhlcv]) => dailyResponse
            }
            scalaz.Success(dailyResponses)
          }
        }

    val flow: Flow[Instant, Validation[String, List[DailyOhlcv]], NotUsed] =
      Flow[Instant]
        .via(queryFlow)
        .via(queryCollectFlow)
        .mapMaterializedValue(_ => NotUsed)

    flow
  }

  private def queryApi(
    symbol: Symbol,
    nRetries: PosZInt,
    config: Config,
  )(
    implicit
    actorSystem: ActorSystem,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Validation[String, List[DailyOhlcv]]] = {

    val uriBase: String = "https://www.alphavantage.co/query"
    val uriParams: ImmutableMap[String, String] = ImmutableMap[String, String](
      "function" -> "TIME_SERIES_DAILY_ADJUSTED",
      "symbol" -> symbol.alphaVantage,
      "apikey" -> config.awsSecrets.alphaVantageApiKey,
      "outputsize" -> "full",
      "datatype" -> "json",
      "entitlement" -> "realtime"
    )
    val uri: Uri = Uri(uriBase).withQuery(Uri.Query(uriParams))
    val apiResponseMaybe = queryApiInner(uri, symbol, nRetries)
    apiResponseMaybe
  }

  private def queryApiInner(
    uri: Uri,
    symbol: Symbol,
    nRetries: PosZInt,
  )(
    implicit
    actorSystem: ActorSystem,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[Validation[String, List[DailyOhlcv]]] = {
    val httpResponseFut: Future[HttpResponse] = Http().singleRequest(HttpRequest(method = HttpMethods.GET, uri = uri))
    val parsedResponseMaybe =
      httpResponseFut.flatMap { (httpResponse: HttpResponse) =>
        httpResponse.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).flatMap { (byteString: ByteString)  =>
          val raw: String = byteString.utf8String
          if (httpResponse.status == StatusCodes.OK) {
            val dailyResponseMaybe: Validation[String, List[DailyOhlcv]] = parseString(raw, symbol)
            val dailyResponse: Future[Validation[String, List[DailyOhlcv]]] = dailyResponseMaybe match {
              case scalaz.Success(_) =>
                Future.successful(dailyResponseMaybe)
              case scalaz.Failure(_) =>
                if (nRetries > 0) queryApiInner(uri, symbol, PosZInt.from(nRetries - 1).get)
                else Future.successful(scalaz.Failure(s"Error parsing: $symbol $uri $dailyResponseMaybe"))
            }
            dailyResponse
          } else {
            if (nRetries > 0)
              queryApiInner(uri, symbol, PosZInt.from(nRetries - 1).get)
            else {
              val failureStr = s"Symbol: $symbol | StatusCode: ${httpResponse.status.intValue()} | Reason: ${httpResponse.status.reason()} | Url: ${uri.toString()}"
              Future.successful[Validation[String, List[DailyOhlcv]]](Validation.failure(failureStr))
            }
          }
        }
      }
      .recoverWith {
        case ex: Throwable =>
          if (nRetries > 0)
            queryApiInner(uri, symbol, PosZInt.from(nRetries - 1).get)
          else {
            val failureStr = s"Symbol: $symbol | Exception: ${ex.getMessage} | Url: ${uri.toString()}"
            Future.successful[Validation[String, List[DailyOhlcv]]](Validation.failure(failureStr))
          }
      }

    parsedResponseMaybe
  }

  private def extractDouble(v: Value, field: String): Double = {
    v(field).value.toString.toDouble
  }

  private def parseString(s: String, symbol: Symbol): Validation[String, List[DailyOhlcv]] = {
    try {
      val json: Value = ujson.read(s)
      json.obj.get("Time Series (Daily)") match {
        case None =>
          scalaz.Failure("Time Series (Daily) not found in json")
        case Some(values) =>
          val dailyOhlcvBars: List[DailyOhlcv] =
            values.obj.map { kv =>
              val k: String = kv._1
              val v: Value = kv._2
              val adjRatio = extractDouble(v, "5. adjusted close") / extractDouble(v, "4. close")
              val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
              val date: LocalDate = LocalDate.parse(k, formatter)
              // round when adjusting so we don't have pesky floating point precision issues
              val open: PosDouble = PosDouble.from(Utils.round(adjRatio * extractDouble(v, "1. open"), 4)).get
              val high: PosDouble = PosDouble.from(Utils.round(adjRatio * extractDouble(v, "2. high"), 4)).get
              val low: PosDouble = PosDouble.from(Utils.round(adjRatio * extractDouble(v, "3. low"), 4)).get
              val close: PosDouble = PosDouble.from(Utils.round(extractDouble(v, "5. adjusted close"), 4)).get
              val volume: Option[PosZDouble] = Some(PosZDouble.from(extractDouble(v, "6. volume") / adjRatio).get)
              val dailyOhlcv: DailyOhlcv = DailyOhlcv(date, symbol, open, high, low, close, volume)
              dailyOhlcv
            }
            .toList
          scalaz.Success(dailyOhlcvBars)
      }

    } catch {
      case e: Throwable =>
        scalaz.Failure(s"Unhandled exception when parsing: ${e.getMessage}")
    }
  }
}
