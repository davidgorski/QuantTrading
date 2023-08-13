package com.quantTrading.infra

import com.quantTrading.infra.Side.{Side, SideBuy, SideSell}
import org.scalactic.anyvals.{PosDouble, PosZDouble}

case class SidedNotional(notional: PosZDouble, side: Side) {
  def toSignedDouble: Double = {
    side match {
      case SideBuy => notional.value
      case SideSell => -1 * notional.value
    }
  }
}

object SidedNotional {

  def apply(sidedQuantity: SidedQuantity, close: PosDouble): SidedNotional = {
    SidedNotional(
      PosZDouble.from(sidedQuantity.qty.value * close).get,
      sidedQuantity.side
    )
  }

  def apply(notional: Double): SidedNotional = {
    SidedNotional(
      PosZDouble.from(notional.abs).get,
      if (notional >= 0) SideBuy else SideSell
    )
  }
}