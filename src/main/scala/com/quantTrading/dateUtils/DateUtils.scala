package com.quantTrading.dateUtils

import com.typesafe.scalalogging.StrictLogging
import java.time.{LocalDate, ZoneId}
import java.time.format.DateTimeFormatter
import scala.collection.immutable.SortedMap
import scala.io.Source
import spray.json._
import DefaultJsonProtocol._


object DateUtils extends StrictLogging {

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

  def getMostRecentBizdayIncludingToday(marketCalendar: MarketCalendar, zoneId: ZoneId): LocalDate = {
    val tday = LocalDate.now(zoneId)
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

  def loadNyseCalendar(): MarketCalendar = {
    val stream = getClass.getResourceAsStream("/nyse_calendar.json")
    if (stream == null)
      throw new RuntimeException("Could not get /nyse_calendar.json from the resource stream")
    val jsonStr = Source.fromInputStream(stream).mkString
    stream.close()

    val marketTimes: List[MarketTimes] = jsonStr.parseJson.convertTo[List[MarketTimes]]
    val map: Map[LocalDate, MarketTimes] =
      marketTimes.map { (marketTime: MarketTimes) =>
        val dt = marketTime.open.toLocalDate
        dt -> marketTime
      }.toMap
    val sortedMap = SortedMap[LocalDate, MarketTimes](map.toArray*)(Ordering.by(_.toEpochDay))
    MarketCalendar(sortedMap)
  }
}
