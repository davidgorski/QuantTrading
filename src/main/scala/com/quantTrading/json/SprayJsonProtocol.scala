package com.quantTrading.json

import org.scalactic.anyvals.{PosDouble, PosZDouble}
import spray.json.{JsNumber, JsString, JsValue, JsonFormat, deserializationError}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import com.quantTrading.symbols.QtSymbol

object SprayJsonProtocol {

  implicit val posZDoubleFormat: JsonFormat[PosZDouble] = new JsonFormat[PosZDouble] {

    override def read(json: JsValue): PosZDouble = json match {
      case JsNumber(value) => PosZDouble.from(value.doubleValue).get
      case other => deserializationError(s"Expected PosZDouble as JsNumber, got $other")
    }

    override def write(obj: PosZDouble): JsValue = JsNumber(obj.value)
  }

  implicit val zonedDateTimeFormat: JsonFormat[ZonedDateTime] = new JsonFormat[ZonedDateTime] {

    private val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

    override def read(json: JsValue): ZonedDateTime = json match {
        case JsString(str) => ZonedDateTime.parse(str, formatter)
        case other => deserializationError(s"Expected ZonedDateTime as JsString, got $other")
    }

    override def write(zonedDateTime: ZonedDateTime): JsValue = JsString(zonedDateTime.format(formatter))
  }

  implicit val posDoubleFormat: JsonFormat[PosDouble] = new JsonFormat[PosDouble] {

    override def read(json: JsValue): PosDouble = json match {
      case JsNumber(value) => PosDouble.from(value.doubleValue).get
      case other => deserializationError(s"Expected PosDouble as JsNumber, got $other")
    }

    override def write(obj: PosDouble): JsValue = JsNumber(obj.value)
  }

  implicit val symbolFormat: JsonFormat[QtSymbol] = new JsonFormat[QtSymbol] {

    override def read(json: JsValue): QtSymbol = json match {
      case JsString(str) => QtSymbol.apply(str)
      case other => deserializationError(s"Expected Symbol as JsString, got $other")
    }

    override def write(obj: QtSymbol): JsValue = JsString(obj.symbol)
  }
}
