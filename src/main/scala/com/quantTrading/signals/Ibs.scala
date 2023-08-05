package com.quantTrading.signals

import com.quantTrading.infra._
import com.quantTrading.scalaUtils.FiniteQueue
import org.scalactic.anyvals.{PosDouble, PosLong, PosZDouble}

case class IbsParams(lookback: PosLong) extends ParamsBase


case class IbsState(
  highs: FiniteQueue[PosDouble],
  lows: FiniteQueue[PosDouble],
  closeMaybe: Option[PosDouble]
) extends StateBase


case class Ibs(
  params: IbsParams,
  state: IbsState
) extends BacktestIterator[OhlcvBase, Option[PosZDouble], Ibs, IbsParams, IbsState] {

  override def onData(ohlcv: OhlcvBase): Ibs = {
    val highsNew = this.state.highs.enqueueFinite(ohlcv.high, this.params.lookback.toInt)
    val lowsNew = this.state.lows.enqueueFinite(ohlcv.low, this.params.lookback.toInt)
    val ibsStateNew = IbsState(highsNew, lowsNew, Some(ohlcv.close))
    Ibs(params, ibsStateNew)
  }

  override def valueMaybe: Option[PosZDouble] = {
    state.closeMaybe match {
      case Some(close: PosDouble) =>
        if (state.highs.size > 0 && state.lows.size > 0) {
          val hh = state.highs.toList.max
          val ll = state.lows.toList.min
          if (math.abs(hh - ll) < 0.0001)
            None
          else
            Some(PosZDouble.from((close.value - ll.value) / (hh.value - ll.value)).get)
        } else {
          None
        }
      case _ =>
        None
    }
  }
}


object Ibs {

  def apply(ibsParams: IbsParams): Ibs = {
    Ibs(
      ibsParams,
      IbsState(
        FiniteQueue[PosDouble](),
        FiniteQueue[PosDouble](),
        None
      )
    )
  }
}