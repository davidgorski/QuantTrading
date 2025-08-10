package com.quantTrading.alphaVantage.daily

import java.time.Instant

case class DailyResponse(
  instant: Instant,
  dailyOhlcvBars: List[DailyOhlcv]
)
