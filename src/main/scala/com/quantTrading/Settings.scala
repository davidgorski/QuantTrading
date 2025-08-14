package com.quantTrading

import com.quantTrading.dateUtils.{DateUtils, MarketCalendar, MarketTimes}

import java.time.{LocalDate, ZoneId, ZonedDateTime}
import scala.collection.mutable.{SortedSet => MutableSortedSet}
import scala.collection.immutable.{SortedSet => ImmutableSortedSet}


object Settings {


  val TIMEZONE: ZoneId = ZoneId.of("America/New_York")

  val ALPHAVANTAGE_API_KEY: String = sys.env("ALPHAVANTAGE_API_KEY")

  val IS_PROD: Boolean = !System.getProperty("os.name").startsWith("Windows")

  val NYSE_CALENDAR: MarketCalendar = DateUtils.loadNyseCalendar()

  val TODAY: LocalDate = DateUtils.getMostRecentBizdayIncludingToday(NYSE_CALENDAR)

  val RUN_TIMES: ImmutableSortedSet[ZonedDateTime] = getRunTimes(TODAY)

  private def getRunTimes(localDate: LocalDate): ImmutableSortedSet[ZonedDateTime] = {
    val marketTimes: MarketTimes = NYSE_CALENDAR.sortedMap(localDate)
    val open: ZonedDateTime = marketTimes.open
    val close: ZonedDateTime = marketTimes.close

    val runTimes: MutableSortedSet[ZonedDateTime] = MutableSortedSet[ZonedDateTime]()(Ordering.by(_.toEpochSecond))

    // every thirty minutes until the close
    var t: ZonedDateTime = open.plusMinutes(30L)
    while (t.isBefore(close)) {
      runTimes += t
      t = t.plusMinutes(30L)
    }

    // every five minutes before the close beginning 30 minutes before
    t = close.minusMinutes(30L)
    while (t.isBefore(close)) {
      runTimes += t
      t = t.plusMinutes(5L)
    }

    // every 1 minute from -20 to close
    t = close.minusMinutes(20L)
    while (t.isBefore(close)) {
      runTimes += t
      t = t.plusMinutes(1L)
    }

    val result: ImmutableSortedSet[ZonedDateTime] = (
      ImmutableSortedSet[ZonedDateTime]()(Ordering.by(_.toEpochSecond))
        ++ runTimes
    )

    result
  }
}
