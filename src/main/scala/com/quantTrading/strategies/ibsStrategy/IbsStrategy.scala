package com.quantTrading.strategies.ibsStrategy

import com.quantTrading.infra.{BacktestIterator, OhlcvBase, QtyBySymbolByDate, Strategy, StrategyParamsBase, StrategyState}
import com.quantTrading.signals.{Ibs, IbsParams}
import com.quantTrading.symbols.Symbol
import org.scalactic.anyvals.{PosDouble, PosLong, PosZDouble}

import scala.collection.immutable.{Map => ImmutableMap}


case class IbsStrategyParams(
  override val name: String,
  lookback: PosLong,
  symbol: Symbol,
  ibsUb: Double,
  override val scaling: PosDouble
) extends StrategyParamsBase


case class IbsStrategyState(
  ibs: Ibs,
  override val qtyBySymbolByDate: QtyBySymbolByDate,
) extends StrategyState


case class IbsStrategy(
  override val params: IbsStrategyParams,
  override val state: IbsStrategyState
) extends Strategy[IbsStrategy, IbsStrategyParams, IbsStrategyState] {

  override def onData(ohlcBySymbol: ImmutableMap[Symbol, OhlcvBase]): IbsStrategy = {
    val date = ohlcBySymbol.values.head.date

    ohlcBySymbol.get(params.symbol) match {

      case None =>
        val notionalBySymbol = ImmutableMap[Symbol, Double]()
        val qtyBySymbolByDateNew = state.qtyBySymbolByDate.updateNotionals(date, ohlcBySymbol, notionalBySymbol)
        val ibsStrategyStateNew = this.state.copy(qtyBySymbolByDate = qtyBySymbolByDateNew)
        val ibsStrategyNew = IbsStrategy(params, ibsStrategyStateNew)
        ibsStrategyNew

      case Some(ohlcv: OhlcvBase) =>
        val ibsNew = this.state.ibs.onData(ohlcv)
        val notionalBySymbol: ImmutableMap[Symbol, Double] =
          ibsNew.valueMaybe match {
            case None =>
              ImmutableMap[Symbol, Double]()

            case Some(ibs: PosZDouble) =>
              if (ibs < this.params.ibsUb) {
                ImmutableMap[Symbol, Double](this.params.symbol -> this.params.scaling.value)
              } else {
                ImmutableMap[Symbol, Double]()
              }
          }
        val qtyBySymbolByDateNew = state.qtyBySymbolByDate.updateNotionals(date, ohlcBySymbol, notionalBySymbol)
        val ibsStrategyStateNew = this.state.copy(qtyBySymbolByDate = qtyBySymbolByDateNew)
        val ibsStrategyNew = IbsStrategy(params, ibsStrategyStateNew)
        ibsStrategyNew
    }
  }
}


object IbsStrategy {

  def apply(
    params: IbsStrategyParams
  ): IbsStrategy = {
    val state = IbsStrategyState(
      Ibs(IbsParams(params.lookback)),
      QtyBySymbolByDate()
    )
    IbsStrategy(
      params,
      state
    )
  }
}
