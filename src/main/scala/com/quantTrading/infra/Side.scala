package com.quantTrading.infra

object Side {
  trait Side {
    def toInt: Int
  }

  case object SideBuy extends Side {
    override def toInt: Int = 1
  }

  case object SideSell extends Side {
    override def toInt: Int = -1
  }
}
