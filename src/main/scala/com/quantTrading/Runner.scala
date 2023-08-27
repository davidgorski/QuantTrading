package com.quantTrading

import com.quantTrading.alphaVantage.AlphaVantage
import com.quantTrading.alphaVantage.AlphaVantage.AlphaVantageOhlcvBase
import com.quantTrading.infra.{OhlcvBase, Strategy, StrategyParamsBase, StrategyResultSerializable, StrategyState}
import com.quantTrading.strategies.StrategyMap
import com.quantTrading.symbols.Symbol
import com.typesafe.scalalogging.Logger
import scalaz.{Failure, Success, Validation}

import java.time.{LocalDate, ZonedDateTime}
import scala.collection.immutable.SortedMap
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.concurrent.duration.{Duration, FiniteDuration, MILLISECONDS}
import scala.collection.immutable.{Map => ImmutableMap}
import scala.collection.mutable.{Map => MutableMap}

object Runner {

  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.fromExecutor(
    new java.util.concurrent.ForkJoinPool(5) // Allow 10 requests to be executing at once
  )

  private def runStrategies(): ImmutableMap[String, StrategyResultSerializable] = {

    val futures: Future[List[Validation[String, List[AlphaVantageOhlcvBase]]]] =
      Future.sequence(Symbol.symbols.map(symbol => Future(AlphaVantage.queryAndCheckAlphaVantage(symbol))))

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
    for (date <- ohlcByDateBySymbol.keys) {
      strategyMap.foreach { kv =>
        val strategyName: String = kv._1
        val strategy: Strategy[StrategyParamsBase, StrategyState] = kv._2
        val strategyNew: Strategy[StrategyParamsBase, StrategyState] = strategy.onData(ohlcByDateBySymbol(date))
        strategyMap(strategyName) = strategyNew
      }
    }

    // print results and store them
    val result = MutableMap[String, StrategyResultSerializable]()
    strategyMap.keys.foreach { strategyName =>
      val strategyResult = strategyMap(strategyName).state.getStrategyResult(ohlcByDateBySymbol)
      val strategyResultSerializable: StrategyResultSerializable = StrategyResultSerializable(strategyResult)
      result(strategyName) = strategyResultSerializable

      println(strategyName)
      println(strategyResult)
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

    result.toMap
  }

  def main(args: Array[String]): Unit = {
    val runTimes = Settings.RUN_TIMES
    val marketClose: ZonedDateTime = Settings.NYSE_CALENDAR.sortedMap(Settings.TODAY).close

    while (ZonedDateTime.now(Settings.TIMEZONE).isBefore(marketClose)) {

      // get the next run time
      val nextRunTime: ZonedDateTime = runTimes.filter(_.isAfter(ZonedDateTime.now(Settings.TIMEZONE))).head
      val millisUntilNextRunTime: Long = nextRunTime.toInstant.toEpochMilli - ZonedDateTime.now(Settings.TIMEZONE).toInstant.toEpochMilli
      Thread.sleep(millisUntilNextRunTime)

      // store the results in mongoDB
      val runResult: ImmutableMap[String, StrategyResultSerializable] = runStrategies()
    }
  }
}
