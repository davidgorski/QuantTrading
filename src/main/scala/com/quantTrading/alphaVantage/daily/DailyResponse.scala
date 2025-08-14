package com.quantTrading.alphaVantage.daily

import java.time.Instant

case class DailyResponse(
  dailyOhlcvBars: List[DailyOhlcv]
)
