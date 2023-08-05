package com.quantTrading.signals

import com.quantTrading.infra.{BacktestIterator, OhlcvBase, ParamsBase, StateBase}
import com.quantTrading.scalaUtils.FiniteQueue
import org.scalactic.anyvals.PosLong

case class RetNParams(lookback: PosLong) extends ParamsBase

case class RetNState(
  priorValues: FiniteQueue[Double] = new FiniteQueue[Double]()
) extends StateBase

case class RetN(
  params: RetNParams,
  state: RetNState = RetNState(),
) extends BacktestIterator[OhlcvBase, Option[Double], RetN, RetNParams, RetNState] {

  override def valueMaybe: Option[Double] = {
    if (state.priorValues.size >= params.lookback.value + 1) {
      Some(state.priorValues.get(state.priorValues.size - 1) / state.priorValues.get(0) - 1)
    } else {
      None
    }
  }

  override def onData(ohlcv: OhlcvBase): RetN = {
    val retNStateNew: RetNState = RetNState(state.priorValues.enqueueFinite(ohlcv.close, (params.lookback + 1).toInt))
    RetN(params, retNStateNew)
  }
}
