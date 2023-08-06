package com.quantTrading.infra

import com.quantTrading.infra.Side.{Side, SideBuy, SideSell}
import org.scalactic.anyvals.PosZDouble

case class SidedQuantity(qty: PosZDouble, side: Side) {

  def +(that: SidedQuantity): SidedQuantity = {
    val qtySignedNew: Double = this.qtySigned + that.qtySigned
    if (qtySignedNew >= 0) {
      SidedQuantity(PosZDouble.from(qtySignedNew).get, SideBuy)
    } else {
      SidedQuantity(PosZDouble.from(-1 * qtySignedNew).get, SideSell)
    }
  }

  def qtySigned: Double = {
    this.side match {
      case SideBuy => qty.value
      case SideSell => -1 * qty.value
    }
  }
}

case object SidedQuantityEmpty extends SidedQuantity(PosZDouble(0.0), SideBuy)
