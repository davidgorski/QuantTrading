package com.quantTrading.signals

import com.quantTrading.infra.{BacktestIterator, ParamsBase, StateBase}
import org.scalactic.anyvals.PosDouble


case class EmaParams(lookback: PosDouble) extends ParamsBase


case class EmaState(valueMaybe: Option[Double] = None) extends StateBase


case class Ema(
  params: EmaParams,
  state: EmaState = EmaState(),
) extends BacktestIterator[Double, Option[Double], Ema, EmaParams, EmaState] {

  override def onData(input: Double): Ema = {
    val emaStateNew: EmaState = {
      state.valueMaybe match {
        case None =>
          EmaState(Some(input))
        case Some(value: Double) =>
          val lambda = 2.0 / (1.0 + params.lookback.value)
          EmaState(Some(input * lambda + value * (1 - lambda)))
      }
    }
    val emaNew: Ema = Ema(params, emaStateNew)
    emaNew
  }

  override def valueMaybe: Option[Double] = state.valueMaybe
}
