package com.quantTrading.alphaVantage


import com.quantTrading.infra.{OhlcvBase, Strategy, StrategyParamsBase, StrategyResult, StrategyResultSerializable, StrategyState}
import com.quantTrading.strategies.StrategyMap
import scala.collection.immutable.{SortedMap, Map => ImmutableMap}
import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import com.quantTrading.symbols.Symbol
import requests.Response
import com.quantTrading.{Settings, Utils}
import com.typesafe.scalalogging.{Logger, StrictLogging}
import org.scalactic.anyvals.{PosDouble, PosLong, PosZDouble}
import scalaz.{Failure, Success, Validation}
import ujson.Value.Value
import java.time.LocalDate


object AlphaVantage extends StrictLogging {

  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.fromExecutor(
    new java.util.concurrent.ForkJoinPool(5) // Allow 10 requests to be executing at once
  )

  case class AlphaVantageOhlcvBase(
    date: LocalDate,
    symbol: Symbol,
    open: PosDouble,
    high: PosDouble,
    low: PosDouble,
    close: PosDouble,
    volume: Option[PosZDouble]
  ) extends OhlcvBase

  private def queryAndCheckAlphaVantage(
    symbol: Symbol
  ): Validation[String, List[AlphaVantageOhlcvBase]] = {

    val queryResultsValidation: Validation[String, List[AlphaVantageOhlcvBase]] = queryAlphaVantage(symbol)
    queryResultsValidation match {
      case Failure(s: String) =>
        Failure(s)

      case Success(ohlcvList) =>
        val ohlcvDates: Set[LocalDate] = ohlcvList.map(_.date).toSet
        val minDate: LocalDate = ohlcvDates.minBy(_.toEpochDay)
        val maxDate: LocalDate = ohlcvDates.maxBy(_.toEpochDay)
        val nyseDates: Set[LocalDate] = Settings.NYSE_CALENDAR.getTradingDates.filter(dt => !dt.isBefore(minDate) && !dt.isAfter(maxDate)).toSet
        val inNyseNotInResult: Set[LocalDate] = nyseDates.diff(ohlcvDates)
        if (inNyseNotInResult.nonEmpty)
          return Failure(s"Found dates in nyse which were missing from query result for ${symbol}: [${inNyseNotInResult.mkString(",")}]")
        val inResultNotInNyse: Set[LocalDate] = ohlcvDates.diff(nyseDates)
        if (inResultNotInNyse.nonEmpty)
          return Failure(s"Found dates in query result which were missing from nyse for ${symbol}: [${inResultNotInNyse.mkString(",")}]")
        Success(ohlcvList)
    }
  }

  private def queryAlphaVantage(
    symbol: Symbol,
    readTimeoutMillis: Int = 10000,
    connectTimeoutMillis: Int = 5000,
  ): Validation[String, List[AlphaVantageOhlcvBase]] = {

    def extractDouble(v: Value, field: String) = v(field).value.toString.toDouble

    val urlBase: String = "https://www.alphavantage.co/query"
    val urlParams: ImmutableMap[String, String] = ImmutableMap[String, String](
      "function" -> "TIME_SERIES_DAILY_ADJUSTED",
      "symbol" -> symbol.symbol,
      "apikey" -> Settings.ALPHAVANTAGE_API_KEY,
      "outputsize" -> "full",
      "datatype" -> "json",
    )
    val response: Response = requests.get(
      urlBase,
      params = urlParams,
      readTimeout = readTimeoutMillis,
      connectTimeout = connectTimeoutMillis
    )
    if (response.statusCode != 200)
      return Failure(s"statusCode=${response.statusCode}; statusMessage=${response.statusMessage}")

    try {
      val json: Value = ujson.read(response.text)
      val ohlcvList: List[AlphaVantageOhlcvBase] =
        json("Time Series (Daily)")
          .obj
          .map(kv => {
            val k: String = kv._1
            val v: Value = kv._2
            val adjRatio = extractDouble(v, "5. adjusted close") / extractDouble(v, "4. close")
            val date: LocalDate = LocalDate.parse(k)
            // round when adjusting so we don't have pesky floating point precision issues
            val open: PosDouble = PosDouble.from(Utils.round(adjRatio * extractDouble(v, "1. open"), 4)).get
            val high: PosDouble = PosDouble.from(Utils.round(adjRatio * extractDouble(v, "2. high"), 4)).get
            val low: PosDouble = PosDouble.from(Utils.round(adjRatio * extractDouble(v, "3. low"), 4)).get
            val close: PosDouble = PosDouble.from(Utils.round(extractDouble(v, "5. adjusted close"), 4)).get
            val volume: Option[PosZDouble] = Some(PosZDouble.from(extractDouble(v, "6. volume") / adjRatio).get)
            AlphaVantageOhlcvBase(date, symbol, open, high, low, close, volume)
          })
          .toList

      Success(ohlcvList)

    } catch {

      case e: Throwable =>
        Failure(e.toString)
    }
  }

  def main(args: Array[String]): Unit = {

    val t0 = System.nanoTime()

    val t1 = System.nanoTime()

    val futures: Future[List[Validation[String, List[AlphaVantageOhlcvBase]]]] =
      Future.sequence(Symbol.symbols.map(symbol => Future(queryAndCheckAlphaVantage(symbol))))

    val queryResultFutures = Await.result(futures, Duration.Inf)
    if (!queryResultFutures.forall(_.isSuccess)) {
      val failures: List[String] = queryResultFutures.collect { case Failure(failureStr: String) => failureStr }
      val failuresStr: String = failures.mkString(" | ")
      throw new RuntimeException(failuresStr)
    }
    val queryResults: List[List[AlphaVantageOhlcvBase]] = queryResultFutures.collect { case Success(listOfAlphaVantageOhlcv: List[AlphaVantageOhlcvBase]) => listOfAlphaVantageOhlcv }

    val logger: Logger = Logger(getClass.getName)
    queryResults.foreach(queryResult => {
      val ohlcvFirst = queryResult.minBy(_.date.toEpochDay)
      logger.info(s"First ohlcv: ${ohlcvFirst.symbol} => ${ohlcvFirst.date}")
    })

    val ohlcByDateBySymbolMutable: MutableMap[LocalDate, MutableMap[Symbol, OhlcvBase]] = MutableMap[LocalDate, MutableMap[Symbol, OhlcvBase]]()
    for (queryResult <- queryResults.flatten) {
      if (!ohlcByDateBySymbolMutable.contains(queryResult.date))
        ohlcByDateBySymbolMutable(queryResult.date) = MutableMap[Symbol, OhlcvBase]()
      ohlcByDateBySymbolMutable(queryResult.date)(queryResult.symbol) = queryResult
    }
    val ohlcByDateBySymbol: SortedMap[LocalDate, ImmutableMap[Symbol, OhlcvBase]] =
      SortedMap[LocalDate, ImmutableMap[Symbol, OhlcvBase]]()(Ordering.by(_.toEpochDay)) ++ ohlcByDateBySymbolMutable.map(kv => (kv._1, kv._2.toMap)).toMap

    // instantiate strategies
    val strategyMap = StrategyMap.getStrategyMap

    // run strategies
    val t2 = System.nanoTime()
    for (date <- ohlcByDateBySymbol.keys) {
      strategyMap.foreach { kv =>
        val strategyName: String = kv._1
        val strategy: Strategy[StrategyParamsBase, StrategyState] = kv._2
        val strategyNew: Strategy[StrategyParamsBase, StrategyState] = strategy.onData(ohlcByDateBySymbol(date))
        strategyMap(strategyName) = strategyNew
      }
    }
    val t3 = System.nanoTime()

    // print results and store them
    strategyMap.keys.foreach { strategyName =>
      val strategyResult = strategyMap(strategyName).state.getStrategyResult(ohlcByDateBySymbol)
      val strategyResultSerializable: StrategyResultSerializable = StrategyResultSerializable(strategyResult)

      println(strategyName)
      println(strategyResult)
      println(strategyResultSerializable)
      println()
    }

    /*
    val codecRegistry = fromRegistries(fromProviders(classOf[StrategyResultSerializable]), DEFAULT_CODEC_REGISTRY)
    //XIT0VXT4RZKRUEHFbeyond
    val mongoUri: String = "mongodb+srv://dgorski:XJNLm37RUtsasmEA@cluster0.wkwbxcv.mongodb.net/?retryWrites=true&w=majority"
    val mongoClient: MongoClient = MongoClient(mongoUri)
    val database: MongoDatabase = mongoClient.getDatabase("QuantLibWeb").withCodecRegistry(codecRegistry)
    val collection: MongoCollection[StrategyResultSerializable] = database.getCollection("person")


    val x: Seq[InsertOneResult] = collection.insertOne(strategyResultSerializable).results()


    val y = 1
    // val x: SortedMap[LocalDate, ImmutableMap[Symbol, Ohlcv]] =

    // val f1 = Future(slowFunction(123))
    //val f2 = Future(slowFunction(456))
    //println(Await.result(f1, Duration.Inf))
    //println(Await.result(f2, Duration.Inf))
    val t4 = System.nanoTime()
    */

    println("time: " + ((t1 - t0) / 1e9).toString)
    println("time: " + ((t2 - t1) / 1e9).toString)
    println("time: " + ((t3 - t2) / 1e9).toString)
  }
}

