package com.quantTrading.signals

import com.quantTrading.infra.{BacktestIterator, OhlcvBase, ParamsBase, StateBase}
import org.scalactic.anyvals.{PosDouble, PosLong}


case class VolParams(lookback: PosDouble) extends ParamsBase


case class VolState(
  ret1: RetN,
  emaVariance: Ema
) extends StateBase


case class Vol(
  params: VolParams,
  state: VolState,
) extends BacktestIterator[OhlcvBase, Option[Double], VolParams, VolState] {

  override def onData(ohlcv: OhlcvBase): Vol = {
    val ret1New = this.state.ret1.onData(ohlcv)
    val emaVarianceNew = ret1New.valueMaybe match {
      case None => this.state.emaVariance
      case Some(ret1: Double) => this.state.emaVariance.onData(ret1 * ret1)
    }
    val volStateNew = VolState(ret1New, emaVarianceNew)
    val volNew: Vol = Vol(params, volStateNew)
    volNew
  }

  override def valueMaybe: Option[Double] = {
    state.emaVariance.valueMaybe match {
      case None => None
      case Some(emaVariance: Double) => Some(math.sqrt(emaVariance))
    }
  }
}

object Vol {
  def apply(volParams: VolParams): Vol = {
    Vol(
      volParams,
      VolState(
        RetN(RetNParams(lookback = PosLong(1L))),
        Ema(EmaParams(lookback = volParams.lookback))
      )
    )
  }
}
