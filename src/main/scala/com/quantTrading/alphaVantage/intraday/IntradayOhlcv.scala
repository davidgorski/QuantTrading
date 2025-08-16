package com.quantTrading.alphaVantage.intraday

import com.quantTrading.symbols.QtSymbol
import org.scalactic.anyvals.{PosDouble, PosZDouble}
import spray.json.JsonFormat
import java.time.ZonedDateTime


case class IntradayOhlcv(
  zonedDateTime: ZonedDateTime,
  symbol: QtSymbol,
  open: PosDouble,
  high: PosDouble,
  low: PosDouble,
  close: PosDouble,
  volume: Option[PosZDouble]
)


object IntradayOhlcv {

  import spray.json.DefaultJsonProtocol._
  import com.quantTrading.json.SprayJsonProtocol._

  implicit val intradayOhlcvFormat: JsonFormat[IntradayOhlcv] = jsonFormat7(IntradayOhlcv.apply)
}
