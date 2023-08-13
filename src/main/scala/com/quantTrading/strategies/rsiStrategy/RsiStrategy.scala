package com.quantTrading.strategies.rsiStrategy

import com.quantTrading.infra.{BacktestIterator, OhlcvBase, QtyBySymbolByDate, Strategy, StrategyParamsBase, StrategyState}
import com.quantTrading.signals.{RetN, RetNParams, Rsi, RsiParams}
import com.quantTrading.symbols.Symbol
import com.quantTrading.strategies
import org.scalactic.anyvals.{PosDouble, PosLong}

import scala.collection.immutable.{Map => ImmutableMap}


case class RsiStrategyParams(
  override val name: String,
  lookback: PosDouble,
  symbol: Symbol,
  rsiUb: Double,
  override val scaling: PosDouble
) extends StrategyParamsBase


case class RsiStrategyState(
  retN: RetN,
  rsi: Rsi,
  override val qtyBySymbolByDate: QtyBySymbolByDate,
) extends StrategyState


case class RsiStrategy(
  override val params: RsiStrategyParams,
  override val state: RsiStrategyState
) extends Strategy[RsiStrategyParams, RsiStrategyState] {

  override def onData(ohlcBySymbol: ImmutableMap[Symbol, OhlcvBase]): RsiStrategy = {
    val date = ohlcBySymbol.values.head.date

    ohlcBySymbol.get(params.symbol) match {

      case None =>
        val notionalBySymbol = ImmutableMap[Symbol, Double]()
        val qtyBySymbolByDateNew = state.qtyBySymbolByDate.updateNotionals(date, ohlcBySymbol, notionalBySymbol)
        val rsiStrategyStateNew = this.state.copy(qtyBySymbolByDate = qtyBySymbolByDateNew)
        val rsiStrategyNew = RsiStrategy(params, rsiStrategyStateNew)
        rsiStrategyNew

      case Some(ohlcv: OhlcvBase) =>
        val retNNew: RetN = state.retN.onData(ohlcv)
        val rsiNew =
          retNNew.valueMaybe match {
            case None =>
              state.rsi
            case Some(r1: Double) =>
              state.rsi.onData(r1)
          }
        val qtyBySymbolByDateNew =
          rsiNew.valueMaybe match {
            case None =>
              val notionalBySymbol = ImmutableMap[Symbol, Double]()
              state.qtyBySymbolByDate.updateNotionals(ohlcv.date, ohlcBySymbol, notionalBySymbol)
            case Some(rsi: Double) =>
              val notional = if (rsi < params.rsiUb) 1.0 * params.scaling.value else 0.0
              val notionalBySymbol = ImmutableMap[Symbol, Double](params.symbol -> notional)
              state.qtyBySymbolByDate.updateNotionals(ohlcv.date, ohlcBySymbol, notionalBySymbol)
          }
        val rsiStrategyStateNew = RsiStrategyState(retNNew, rsiNew, qtyBySymbolByDateNew)
        val rsiStrategyNew = RsiStrategy(params, rsiStrategyStateNew)
        rsiStrategyNew
    }
  }
}


object RsiStrategy {

  def apply(
    params: RsiStrategyParams
  ): RsiStrategy = {
    val retN: RetN = RetN(RetNParams(PosLong(1L)))
    val rsi: Rsi = Rsi(RsiParams(params.lookback))
    val qtyBySymbolByDate = QtyBySymbolByDate()
    val state = RsiStrategyState(retN, rsi, qtyBySymbolByDate)
    RsiStrategy(
      params,
      state
    )
  }
}
