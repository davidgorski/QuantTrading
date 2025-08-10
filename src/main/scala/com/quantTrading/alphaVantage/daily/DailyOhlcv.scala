package com.quantTrading.alphaVantage.daily

import com.quantTrading.symbols.Symbol
import org.scalactic.anyvals.{PosDouble, PosZDouble}

import java.time.LocalDate

case class DailyOhlcv(
  date: LocalDate,
  symbol: Symbol,
  open: PosDouble,
  high: PosDouble,
  low: PosDouble,
  close: PosDouble,
  volume: Option[PosZDouble]
)
