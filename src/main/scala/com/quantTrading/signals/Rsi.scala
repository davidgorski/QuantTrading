package com.quantTrading.signals

import com.quantTrading.infra._
import org.scalactic.anyvals.PosDouble

case class RsiParams(lookback: PosDouble) extends ParamsBase


case class RsiState(emaUp: Ema, emaDn: Ema) extends StateBase


case class Rsi(
  params: RsiParams,
  state: RsiState
) extends BacktestIterator[Double, Option[Double], RsiParams, RsiState] {

  override def onData(r1: Double): Rsi = {
    val emaUpNew = state.emaUp.onData(math.max(0.0, r1))
    val emaDnNew = state.emaDn.onData(math.abs(math.min(0.0, r1)))
    val rsiStateNew = RsiState(emaUpNew, emaDnNew)
    Rsi(params, rsiStateNew)
  }

  override def valueMaybe: Option[Double] = {
    val rsi = {
      (state.emaDn.valueMaybe, state.emaUp.valueMaybe) match {
        case (Some(0.0), _) => None
        case (_, Some(0.0)) => None
        case (None, _) => None
        case (_, None) => None
        case (Some(emaDn: Double), Some(emaUp: Double)) =>
          Some(1.0 - 1.0 / (1.0 + emaUp / emaDn))
      }
    }
    rsi
  }
}


object Rsi {

  def apply(rsiParams: RsiParams): Rsi = {
    Rsi(
      rsiParams,
      RsiState(
        Ema(EmaParams(PosDouble.from(rsiParams.lookback).get)),
        Ema(EmaParams(PosDouble.from(rsiParams.lookback).get)),
      )
    )
  }
}