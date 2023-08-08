package com.quantTrading.alphaVantage


import com.quantTrading.infra.{OhlcvBase, Strategy, StrategyParamsBase, StrategyResult, StrategyResultSerializable, StrategyState}
import com.quantTrading.strategies.compositeStrategy.CompositeStrategy
import com.quantTrading.strategies.compositeStrategy.{CompositeStrategy, CompositeStrategyParams}
import com.quantTrading.strategies.ibsStrategy.{IbsStrategy, IbsStrategyParams}
import com.quantTrading.strategies.rsiStrategy.{RsiStrategy, RsiStrategyParams}
import com.quantTrading.strategies.trendStrategy.{TrendStrategy, TrendStrategyParams}
import com.quantTrading.strategies.dualMomentumStrategy.{DualMomentumStrategy, DualMomentumStrategyParams}
import com.quantTrading.strategies.protectiveMomentumStrategy.{ProtectiveMomentumStrategy, ProtectiveMomentumStrategyParams}
import com.quantTrading.strategies.xassetStrategy.{XassetStrategy, XassetStrategyParams}

import scala.collection.immutable.{SortedMap, Map => ImmutableMap}
import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import com.quantTrading.symbols.Symbol
import requests.Response
import com.quantTrading.{Settings, Utils}
import com.typesafe.scalalogging.{Logger, StrictLogging}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.result.InsertOneResult
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}
import org.scalactic.anyvals.{PosDouble, PosLong, PosZDouble}
import scalaz.{Failure, Success, Validation}
import ujson.Value.Value

import java.time.LocalDate
import upickle.default._
import org.mongodb.scala.bson.codecs.Macros._


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

  def queryAndCheckAlphaVantage(
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

  def queryAlphaVantage(
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
    var rsiStrategy: RsiStrategy = RsiStrategy(RsiStrategyParams(
      "RSI SPY",
      PosDouble(5.0),
      Symbol.SPY,
      0.20,
      PosDouble(1062575.05)
    ))
    var trendStrategy: TrendStrategy = TrendStrategy(TrendStrategyParams(
      "Trend Strategy",
      List[Symbol](Symbol.SPY, Symbol.IEF, Symbol.GLD).map((_, PosDouble.from(252).get)).toMap,
      List[Symbol](Symbol.SPY, Symbol.IEF, Symbol.GLD).map((_, PosDouble.from(22).get)).toMap,
      PosDouble(4068.348)
    ))
    var dualMomentumStrategy: DualMomentumStrategy = DualMomentumStrategy(DualMomentumStrategyParams(
      "Dual Momentum",
      List[List[Symbol]](
        List[Symbol](Symbol.SPY, Symbol.QQQ, Symbol.BIL),
        List[Symbol](Symbol.TLT, Symbol.IEF, Symbol.BIL),
        List[Symbol](Symbol.HYG, Symbol.LQD, Symbol.BIL),
      ),
      PosDouble(252.0),
      PosDouble(1428571.43)
    ))
    var protectiveMomentumStrategy: ProtectiveMomentumStrategy = ProtectiveMomentumStrategy(ProtectiveMomentumStrategyParams(
      "Protective Momentum",
      List[Symbol](Symbol.BIL, Symbol.IEF),
      List[Symbol](Symbol.SPY, Symbol.QQQ, Symbol.IWM, Symbol.EZU, Symbol.EWJ, Symbol.EEM, Symbol.GLD, Symbol.HYG, Symbol.LQD, Symbol.TLT),
      PosDouble(252.0),
      PosDouble(2.0),
      PosLong(6L),
      PosLong(1L),
      PosDouble(1250000.0)
    ))
    var ibsStrategySpy: IbsStrategy = IbsStrategy(IbsStrategyParams("IBS SPY", PosLong(5L), Symbol.SPY, 0.25, PosDouble(909090.91)))
    var ibsStrategyQqq: IbsStrategy = IbsStrategy(IbsStrategyParams("IBS QQQ", PosLong(10L), Symbol.QQQ, 0.10, PosDouble(833333.33)))
    var ibsStrategyTlt: IbsStrategy = IbsStrategy(IbsStrategyParams("IBS TLT", PosLong(5L), Symbol.TLT, 0.10, PosDouble(2000000.00)))
    var ibsStrategyIef: IbsStrategy = IbsStrategy(IbsStrategyParams("IBS IEF", PosLong(5L), Symbol.IEF, 0.10, PosDouble(5000000.00)))
    var xAssetStrategySpyTlt: XassetStrategy = XassetStrategy(XassetStrategyParams("Xasset SPY TLT", PosDouble(5.0), Symbol.SPY, Symbol.TLT, PosDouble(1111111.11)))
    var xAssetStrategySpyIef: XassetStrategy = XassetStrategy(XassetStrategyParams("Xasset SPY IEF", PosDouble(5.0), Symbol.SPY, Symbol.IEF, PosDouble(1111111.11)))
    var xAssetStrategyQqqTlt: XassetStrategy = XassetStrategy(XassetStrategyParams("Xasset QQQ TLT", PosDouble(5.0), Symbol.QQQ, Symbol.TLT, PosDouble(1000000.00)))
    var xAssetStrategyQqqIef: XassetStrategy = XassetStrategy(XassetStrategyParams("Xasset QQQ IEF", PosDouble(5.0), Symbol.QQQ, Symbol.IEF, PosDouble(1000000.00)))
    var xAssetStrategySpyHyg: XassetStrategy = XassetStrategy(XassetStrategyParams("Xasset SPY HYG", PosDouble(5.0), Symbol.SPY, Symbol.HYG, PosDouble(3333333.33)))
    var xAssetStrategyQqqHyg: XassetStrategy = XassetStrategy(XassetStrategyParams("Xasset QQQ HYG", PosDouble(5.0), Symbol.QQQ, Symbol.HYG, PosDouble(2500000.00)))


    val strategyList: List[Strategy[_, StrategyParamsBase, StrategyState]] = List[Strategy[_, StrategyParamsBase, StrategyState]](
      rsiStrategy,
      trendStrategy,
      dualMomentumStrategy,
      protectiveMomentumStrategy,
      ibsStrategySpy,
      ibsStrategyQqq,
      ibsStrategyTlt,
      ibsStrategyIef,
      xAssetStrategySpyTlt,
      xAssetStrategySpyIef,
      xAssetStrategyQqqTlt,
      xAssetStrategyQqqIef,
      xAssetStrategySpyHyg,
      xAssetStrategyQqqHyg
    )

    var compositeStrategy: CompositeStrategy = CompositeStrategy(CompositeStrategyParams(
      "CompositeStrategy",
      strategyList,
      PosDouble(1000000.0)
    ))

    // run strategies
    val t2 = System.nanoTime()
    for (date <- ohlcByDateBySymbol.keys) {
      compositeStrategy = compositeStrategy.onData(ohlcByDateBySymbol(date))
      /*
      rsiStrategy = rsiStrategy.onData(ohlcByDateBySymbol(date))
      trendStrategy = trendStrategy.onData(ohlcByDateBySymbol(date))
      ibsStrategySpy = ibsStrategySpy.onData(ohlcByDateBySymbol(date))
      ibsStrategyQqq = ibsStrategyQqq.onData(ohlcByDateBySymbol(date))
      ibsStrategyTlt = ibsStrategyTlt.onData(ohlcByDateBySymbol(date))
      ibsStrategyIef = ibsStrategyIef.onData(ohlcByDateBySymbol(date))
      dualMomentumStrategy = dualMomentumStrategy.onData(ohlcByDateBySymbol(date))
      protectiveMomentumStrategy = protectiveMomentumStrategy.onData(ohlcByDateBySymbol(date))
      xAssetStrategySpyTlt = xAssetStrategySpyTlt.onData(ohlcByDateBySymbol(date))
      xAssetStrategySpyIef = xAssetStrategySpyIef.onData(ohlcByDateBySymbol(date))
      xAssetStrategyQqqTlt = xAssetStrategyQqqTlt.onData(ohlcByDateBySymbol(date))
      xAssetStrategyQqqIef = xAssetStrategyQqqIef.onData(ohlcByDateBySymbol(date))
      xAssetStrategySpyHyg = xAssetStrategySpyHyg.onData(ohlcByDateBySymbol(date))
      xAssetStrategyQqqHyg = xAssetStrategyQqqHyg.onData(ohlcByDateBySymbol(date))
      */
    }
    val t3 = System.nanoTime()

    println("calculating results")
    val compositeStrategyResult: StrategyResult = compositeStrategy.state.getStrategyResult(ohlcByDateBySymbol)
    val t4 = System.nanoTime()
    println(compositeStrategyResult)

    /*
    val rsiStrategyResult: StrategyResult = rsiStrategy.state.getStrategyResult(ohlcByDateBySymbol)
    val trendStrategyResult: StrategyResult = trendStrategy.state.getStrategyResult(ohlcByDateBySymbol)
    val ibsStrategySpyResult: StrategyResult = ibsStrategySpy.state.getStrategyResult(ohlcByDateBySymbol)
    val ibsStrategyQqqResult: StrategyResult = ibsStrategyQqq.state.getStrategyResult(ohlcByDateBySymbol)
    val ibsStrategyTltResult: StrategyResult = ibsStrategyTlt.state.getStrategyResult(ohlcByDateBySymbol)
    val ibsStrategyIefResult: StrategyResult = ibsStrategyIef.state.getStrategyResult(ohlcByDateBySymbol)
    val dualMomentumStrategyResult: StrategyResult = dualMomentumStrategy.state.getStrategyResult(ohlcByDateBySymbol)
    val protectiveMomentumStrategyResult: StrategyResult = protectiveMomentumStrategy.state.getStrategyResult(ohlcByDateBySymbol)
    val xAssetStrategySpyTltResult: StrategyResult = xAssetStrategySpyTlt.state.getStrategyResult(ohlcByDateBySymbol)
    val xAssetStrategySpyIefResult: StrategyResult = xAssetStrategySpyIef.state.getStrategyResult(ohlcByDateBySymbol)
    val xAssetStrategyQqqTltResult: StrategyResult = xAssetStrategyQqqTlt.state.getStrategyResult(ohlcByDateBySymbol)
    val xAssetStrategyQqqIefResult: StrategyResult = xAssetStrategyQqqIef.state.getStrategyResult(ohlcByDateBySymbol)
    val xAssetStrategySpyHygResult: StrategyResult = xAssetStrategySpyHyg.state.getStrategyResult(ohlcByDateBySymbol)
    val xAssetStrategyQqqHygResult: StrategyResult = xAssetStrategyQqqHyg.state.getStrategyResult(ohlcByDateBySymbol)

    println(rsiStrategyResult)
    println(trendStrategyResult)
    println(ibsStrategySpyResult)
    println(ibsStrategyQqqResult)
    println(ibsStrategyTltResult)
    println(ibsStrategyIefResult)
    println(dualMomentumStrategyResult)
    println(protectiveMomentumStrategyResult)
    println(xAssetStrategySpyTltResult)
    println(xAssetStrategySpyIefResult)
    println(xAssetStrategyQqqTltResult)
    println(xAssetStrategyQqqIefResult)
    println(xAssetStrategySpyHygResult)
    println(xAssetStrategyQqqHygResult)
    */
    /*
    val strategyResultSerializable: StrategyResultSerializable = StrategyResultSerializable(rsiStrategyResult)

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
    println("time: " + ((t4 - t3) / 1e9).toString)
  }
}

