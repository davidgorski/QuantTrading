package com.quantTrading.alphaVantage.daily

import com.quantTrading.symbols.QtSymbol
import org.scalactic.anyvals.{PosDouble, PosZDouble}

import java.time.LocalDate

case class DailyOhlcv(
  date: LocalDate,
  symbol: QtSymbol,
  open: PosDouble,
  high: PosDouble,
  low: PosDouble,
  close: PosDouble,
  volume: Option[PosZDouble]
)
