package com.quantTrading.infra

import com.quantTrading.symbols.QtSymbol
import org.scalactic.anyvals.{PosDouble, PosZDouble}
import java.time.LocalDate


trait OhlcvBase {

  def date: LocalDate
  def symbol: QtSymbol
  def open: PosDouble
  def high: PosDouble
  def low: PosDouble
  def close: PosDouble
  def volume: Option[PosZDouble]

  require(high >= open)
  require(high >= close)
  require(high >= low)
  require(low <= open)
  require(low <= close)
  require(low <= high)
}


case class Ohlcv(
  date: LocalDate,
  symbol: QtSymbol,
  open: PosDouble,
  high: PosDouble,
  low: PosDouble,
  close: PosDouble,
  volume: Option[PosZDouble]
) extends OhlcvBase
