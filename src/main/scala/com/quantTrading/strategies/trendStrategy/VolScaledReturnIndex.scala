package com.quantTrading.strategies.trendStrategy

import com.quantTrading.infra.{BacktestIterator, OhlcvBase, ParamsBase, StateBase}
import com.quantTrading.signals.{RetN, RetNParams, Vol, VolParams}
import org.scalactic.anyvals.{PosDouble, PosLong}


case class VolScaledReturnIndexParams(lookback: PosDouble) extends ParamsBase


case class VolScaledReturnIndexState(
  ret1: RetN,
  vol: Vol,
  valueMaybe: Option[Double] = None
) extends StateBase


case class VolScaledReturnIndex(
  params: VolScaledReturnIndexParams,
  state: VolScaledReturnIndexState,
) extends BacktestIterator[OhlcvBase, Option[Double], VolScaledReturnIndex, VolScaledReturnIndexParams, VolScaledReturnIndexState] {

  override def onData(ohlcv: OhlcvBase): VolScaledReturnIndex = {
    val ret1New = this.state.ret1.onData(ohlcv)
    val volNew = this.state.vol.onData(ohlcv)
    val volOld = this.state.vol.valueMaybe

    val valueMaybe =
      (this.state.valueMaybe, ret1New.valueMaybe, volOld) match {
        case (Some(valueInner: Double), Some(ret1: Double), Some(volOldInner: Double)) =>
          Some(valueInner + ret1 / volOldInner)
        case (None, Some(ret1: Double), Some(volOldInner: Double)) =>
          Some(ret1 / volOldInner)
        case _ =>
          None
      }
    val volScaledReturnState: VolScaledReturnIndexState = VolScaledReturnIndexState(ret1New, volNew, valueMaybe)
    VolScaledReturnIndex(params, volScaledReturnState)
  }

  override def valueMaybe: Option[Double] = state.valueMaybe
}

object VolScaledReturnIndex {
  def apply(volScaledReturnParams: VolScaledReturnIndexParams): VolScaledReturnIndex = {
    VolScaledReturnIndex(
      volScaledReturnParams,
      VolScaledReturnIndexState(
        RetN(RetNParams(PosLong(1L))),
        Vol(VolParams(PosDouble.from(volScaledReturnParams.lookback).get))
      )
    )
  }
}
