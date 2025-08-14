package com.quantTrading.runner

import com.quantTrading.alphaVantage.daily.DailyOhlcv
import com.quantTrading.alphaVantage.intraday.IntradayOhlcv
import com.quantTrading.scala.{Choice1, Choice2, Choice3, ChoiceType3}

import scala.collection.mutable.ListBuffer

final case class TickingData(
  intra5MinBars: List[IntradayOhlcv],
  dailyBars: List[DailyOhlcv],
  other: Unit
)

object TickingData {

  def fromSeqOfChoiceType(
    listOfChoiceTypes: List[ChoiceType3[scalaz.Validation[String, List[IntradayOhlcv]], scalaz.Validation[String, List[DailyOhlcv]], scalaz.Validation[String, Unit]]]
  ): scalaz.Validation[String, TickingData] = {

    val failures: ListBuffer[String] = ListBuffer[String]() ++ listOfChoiceTypes.collect {
      case Choice1(scalaz.Failure(s: String)) => s
      case Choice2(scalaz.Failure(s: String)) => s
      case Choice3(scalaz.Failure(s: String)) => s
    }

    val intra5MinBarsSuccesses: List[List[IntradayOhlcv]] = listOfChoiceTypes.collect { case Choice1(scalaz.Success(listOfIntradayOhlcv: List[IntradayOhlcv])) => listOfIntradayOhlcv }
    val dailyBarsSuccesses: List[List[DailyOhlcv]] = listOfChoiceTypes.collect { case Choice2(scalaz.Success(listOfDailyOhlcv: List[DailyOhlcv])) => listOfDailyOhlcv }
    val otherSuccesses: List[Unit] = listOfChoiceTypes.collect { case Choice3(scalaz.Success(unit: Unit)) => unit }

    if (intra5MinBarsSuccesses.size != 1)
      failures.append(s"Found intra5MinBarsSuccesses.size=${intra5MinBarsSuccesses.size}; expected exactly 1")
    if (dailyBarsSuccesses.size != 1)
      failures.append(s"Found dailyBarsSuccesses.size=${dailyBarsSuccesses.size}; expected exactly 1")
    if (otherSuccesses.size != 1)
      failures.append(s"Found otherSuccesses.size=${otherSuccesses.size}; expected exactly 1")

    if (failures.isEmpty) {
      val tickingData = TickingData(intra5MinBarsSuccesses.head, dailyBarsSuccesses.head, otherSuccesses.head)
      scalaz.Success(tickingData)
    } else {
      scalaz.Failure(failures.mkString(" | "))
    }
  }
}
