package com.quantTrading.dateUtils

import java.time.LocalDate
import scala.collection.immutable.SortedMap


case class MarketCalendar(sortedMap: SortedMap[LocalDate, MarketTimes]) {
  def getTradingDates: List[LocalDate] = sortedMap.keys.toList
}

