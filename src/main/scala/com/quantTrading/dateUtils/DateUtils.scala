package com.quantTrading.dateUtils

import com.quantTrading.Settings

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
  }

  def addBizdays(date: LocalDate, nBizDays: Int, marketCalendar: MarketCalendar): LocalDate = {
    val busdays = marketCalendar.getTradingDates
    val dateIndex = busdays.indexOf(date)

    if (dateIndex < 0)
      throw new RuntimeException(s"addBizdays must be called from a starting business day but ${date.format(DateTimeFormatter.ISO_DATE)} is not a business date")

    if (dateIndex + nBizDays < 0)
      throw new RuntimeException(s"${date.format(DateTimeFormatter.ISO_DATE)} + $nBizDays is before the earliest business day in the calendar")

    if (dateIndex + nBizDays >= busdays.size)
      throw new RuntimeException(s"${date.format(DateTimeFormatter.ISO_DATE)} + $nBizDays is after the latest business day in the calendar")

    busdays(dateIndex + nBizDays)
  }

  def getMostRecentBizdayIncludingToday(marketCalendar: MarketCalendar): LocalDate = {
    val tday = LocalDate.now(Settings.TIMEZONE)
    val tradingDates = marketCalendar.getTradingDates
    if (tradingDates.contains(tday))
      tday
    else {
      var dt = tday
      while (!tradingDates.contains(dt)) {
        dt = dt.minusDays(1L)
      }
      dt
    }
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
