package com.quantTrading.dateUtils

import java.time.{LocalDate, ZonedDateTime}
import java.time.format.DateTimeFormatter
import scala.collection.immutable.SortedMap
import scala.collection.mutable.{Map => MutableMap}

object DateUtils {

  case class MarketTimes(open: ZonedDateTime, close: ZonedDateTime) {
    require(open.toLocalDate.isEqual(close.toLocalDate))
    require(close.isAfter(open))
  }

  case class MarketCalendar(sortedMap: SortedMap[LocalDate, MarketTimes]) {

    def getTradingDates: List[LocalDate] = sortedMap.keys.toList

    def isTradingDate(date: LocalDate): Boolean = sortedMap.contains(date)
  }

  def getNyseCalendar(): MarketCalendar = {
    val bufferedSource = io.Source.fromFile("src/main/resources/nyse_calendar.csv")
    val mutMap: MutableMap[LocalDate, MarketTimes] = MutableMap[LocalDate, MarketTimes]()
    for (line <- bufferedSource.getLines) {
      val cols = line.split(",").map(_.trim)
      val zdtOpen = ZonedDateTime.parse(cols(0), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      val zdtClose = ZonedDateTime.parse(cols(1), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      val marketTimes = MarketTimes(zdtOpen, zdtClose)
      mutMap += (zdtOpen.toLocalDate -> marketTimes)
    }
    bufferedSource.close
    val sortedMap = SortedMap[LocalDate, MarketTimes](mutMap.toArray:_*)(Ordering.by(_.toEpochDay))
    MarketCalendar(sortedMap)
  }

}
