package com.quantTrading

import com.quantTrading.dateUtils.DateUtils
import com.quantTrading.dateUtils.DateUtils.MarketCalendar

import java.time.ZoneId
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration


object Settings {

  val LOOP_TIME: FiniteDuration = FiniteDuration(5, TimeUnit.SECONDS)

  val TIMEZONE: ZoneId = ZoneId.of("America/New_York")

  val ALPHAVANTAGE_API_KEY: String = "TODO"

  val NYSE_CALENDAR: MarketCalendar = DateUtils.getNyseCalendar()

}
