package com.quantTrading.dateUtils

import java.time.ZonedDateTime


case class MarketTimes(open: ZonedDateTime, close: ZonedDateTime) {
  require(open.toLocalDate.isEqual(close.toLocalDate))
  require(close.isAfter(open))
}


object MarketTimes {

  import spray.json._
  import com.quantTrading.json.SprayJsonProtocol._
  import spray.json.DefaultJsonProtocol._

  implicit val marketTimesFormat: JsonFormat[MarketTimes] = jsonFormat2(MarketTimes.apply)
}