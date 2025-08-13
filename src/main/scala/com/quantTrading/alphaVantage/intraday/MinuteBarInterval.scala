package com.quantTrading.alphaVantage.intraday

import org.scalactic.anyvals.PosInt

sealed abstract class MinuteBarInterval(val minutes: PosInt) {

  override def toString: String = s"${minutes.value}min"

  /**
   * This is the response string in the alphavantage json output
   * @return
   */
  def getResponseKey: String = s"Time Series (${this.toString})"
}


case object MinuteBarInterval01 extends MinuteBarInterval(PosInt(1))
case object MinuteBarInterval05 extends MinuteBarInterval(PosInt(5))
case object MinuteBarInterval15 extends MinuteBarInterval(PosInt(15))
