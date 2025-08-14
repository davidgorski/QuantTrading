//package com.quantTrading.alphaVantage
//
//
//import com.quantTrading.infra.{OhlcvBase, Strategy, StrategyParamsBase}
//import com.quantTrading.strategies.StrategyMap
//import scala.collection.immutable.{SortedMap, Map => ImmutableMap}
//import scala.collection.mutable.{Map => MutableMap}
//import scala.concurrent.duration.Duration
//import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
//import com.quantTrading.symbols.Symbol
//import requests.Response
//import com.quantTrading.Utils
//import com.typesafe.scalalogging.{Logger, StrictLogging}
//import org.scalactic.anyvals.{PosDouble, PosLong, PosZDouble}
//import scalaz.{Failure, Success, Validation}
//import ujson.Value.Value
//import java.time.LocalDate
//
//
//object AlphaVantage extends StrictLogging {
//
//  case class AlphaVantageOhlcvBase(
//    date: LocalDate,
//    symbol: Symbol,
//    open: PosDouble,
//    high: PosDouble,
//    low: PosDouble,
//    close: PosDouble,
//    volume: Option[PosZDouble]
//  ) extends OhlcvBase
//
//  def queryAndCheckAlphaVantage(
//    symbol: Symbol
//  ): Validation[String, List[AlphaVantageOhlcvBase]] = {
//
//    val queryResultsValidation: Validation[String, List[AlphaVantageOhlcvBase]] = queryAlphaVantage(symbol)
//    queryResultsValidation match {
//      case Failure(s: String) =>
//        Failure(s)
//
//      case Success(ohlcvList) =>
//        val ohlcvDates: Set[LocalDate] = ohlcvList.map(_.date).toSet
//        val minDate: LocalDate = ohlcvDates.minBy(_.toEpochDay)
//        val maxDate: LocalDate = ohlcvDates.maxBy(_.toEpochDay)
//        val nyseDates: Set[LocalDate] = Settings.NYSE_CALENDAR.getTradingDates.filter(dt => !dt.isBefore(minDate) && !dt.isAfter(maxDate)).toSet
//        val inNyseNotInResult: Set[LocalDate] = nyseDates.diff(ohlcvDates)
//        if (inNyseNotInResult.nonEmpty)
//          return Failure(s"Found dates in nyse which were missing from query result for ${symbol}: [${inNyseNotInResult.mkString(",")}]")
//        val inResultNotInNyse: Set[LocalDate] = ohlcvDates.diff(nyseDates)
//        if (inResultNotInNyse.nonEmpty)
//          return Failure(s"Found dates in query result which were missing from nyse for ${symbol}: [${inResultNotInNyse.mkString(",")}]")
//        Success(ohlcvList)
//    }
//  }
//
//  private def queryAlphaVantage(
//    symbol: Symbol,
//    readTimeoutMillis: Int = 10000,
//    connectTimeoutMillis: Int = 5000,
//  ): Validation[String, List[AlphaVantageOhlcvBase]] = {
//
//    def extractDouble(v: Value, field: String) = v(field).value.toString.toDouble
//
//    val urlBase: String = "https://www.alphavantage.co/query"
//    val urlParams: ImmutableMap[String, String] = ImmutableMap[String, String](
//      "function" -> "TIME_SERIES_DAILY_ADJUSTED",
//      "symbol" -> symbol.symbol,
//      "apikey" -> Settings.ALPHAVANTAGE_API_KEY,
//      "outputsize" -> "full",
//      "datatype" -> "json",
//    )
//    val response: Response = requests.get(
//      urlBase,
//      params = urlParams,
//      readTimeout = readTimeoutMillis,
//      connectTimeout = connectTimeoutMillis
//    )
//    if (response.statusCode != 200)
//      return Failure(s"statusCode=${response.statusCode}; statusMessage=${response.statusMessage}")
//
//    try {
//      val json: Value = ujson.read(response.text)
//      val ohlcvList: List[AlphaVantageOhlcvBase] =
//        json("Time Series (Daily)")
//          .obj
//          .map(kv => {
//            val k: String = kv._1
//            val v: Value = kv._2
//            val adjRatio = extractDouble(v, "5. adjusted close") / extractDouble(v, "4. close")
//            val date: LocalDate = LocalDate.parse(k)
//            // round when adjusting so we don't have pesky floating point precision issues
//            val open: PosDouble = PosDouble.from(Utils.round(adjRatio * extractDouble(v, "1. open"), 4)).get
//            val high: PosDouble = PosDouble.from(Utils.round(adjRatio * extractDouble(v, "2. high"), 4)).get
//            val low: PosDouble = PosDouble.from(Utils.round(adjRatio * extractDouble(v, "3. low"), 4)).get
//            val close: PosDouble = PosDouble.from(Utils.round(extractDouble(v, "5. adjusted close"), 4)).get
//            val volume: Option[PosZDouble] = Some(PosZDouble.from(extractDouble(v, "6. volume") / adjRatio).get)
//            AlphaVantageOhlcvBase(date, symbol, open, high, low, close, volume)
//          })
//          .toList
//
//      Success(ohlcvList)
//
//    } catch {
//
//      case e: Throwable =>
//        Failure(e.toString)
//    }
//  }
//}
//
