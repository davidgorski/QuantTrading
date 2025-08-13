package com.quantTrading.alphaVantage.intraday

import com.quantTrading.config.{Config, ConfigQa}
import com.quantTrading.symbols.Symbol
import org.scalactic.anyvals.{PosInt, PosZInt}
import scalaz.Validation

import java.time.{LocalDate, LocalDateTime, ZoneId}


object ExampleIntraday {

  def main(args: Array[String]): Unit = {

    val symbols = Symbol.symbols
    val zoneId = ZoneId.of("America/New_York")
    val sd: LocalDate = LocalDate.of(2000, 1, 1)
    val ed: LocalDate = LocalDate.now(zoneId)
    val minuteBarInterval: MinuteBarInterval = MinuteBarInterval05
    val nRetries: PosZInt = PosZInt(3)
    val nConcurrentQueriesAlphaVantage: PosInt = PosInt(14)
    val nConcurrentQueriesS3: PosInt = PosInt(30)
    val config: Config = ConfigQa()

    println(LocalDateTime.now(zoneId))
    // todo sleep so you don't hit max counter of 150/min
    val result: Validation[String, List[IntradayOhlcv]] = HistoryLoadCache.queryApiRange(
      symbols,
      sd,
      ed,
      minuteBarInterval,
      zoneId,
      nRetries,
      nConcurrentQueriesS3,
      nConcurrentQueriesAlphaVantage,
      config
    )
    println(LocalDateTime.now(zoneId))

    result match {
      case scalaz.Failure(e) => println(s"Failure! $e")
      case scalaz.Success(s) =>
        println(s"Success! found ${s.size} rows")
        println(s.take(10))
    }
  }
}
