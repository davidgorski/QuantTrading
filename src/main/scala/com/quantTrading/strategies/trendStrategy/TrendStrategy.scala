package com.quantTrading.strategies.trendStrategy

import com.quantTrading.infra.{BacktestIterator, OhlcvBase, QtyBySymbolByDate, Side, SidedQuantity, Strategy, StrategyParamsBase, StrategyState}
import com.quantTrading.signals._
import com.quantTrading.symbols.Symbol
import org.scalactic.anyvals.{PosDouble, PosZDouble}

import scala.collection.immutable.{Map => ImmutableMap}
import scala.collection.mutable.{Map => MutableMap}


case class TrendStrategyParams(
  override val name: String,
  symbolToTrendLookback: ImmutableMap[Symbol, PosDouble],
  symbolToVolLookback: ImmutableMap[Symbol, PosDouble],
  override val scaling: PosDouble
) extends StrategyParamsBase {

  require(symbolToTrendLookback.keys.toSet.equals(symbolToVolLookback.keys.toSet))

  def symbols: List[Symbol] = symbolToTrendLookback.keys.toList
}


case class TrendStrategyState(
  volScaledReturnBySymbol: ImmutableMap[Symbol, VolScaledReturnIndex],
  volScaledReturnEmaBySymbol: ImmutableMap[Symbol, Ema],
  qtyBySymbolByDate: QtyBySymbolByDate,
) extends StrategyState


case class TrendStrategy(
  override val params: TrendStrategyParams,
  override val state: TrendStrategyState
) extends Strategy[TrendStrategy, TrendStrategyParams, TrendStrategyState] {

  override def onData(ohlcBySymbol: ImmutableMap[Symbol, OhlcvBase]): TrendStrategy = {
    val volScaledReturnBySymbolNew: MutableMap[Symbol, VolScaledReturnIndex] = MutableMap[Symbol, VolScaledReturnIndex]()
    val volScaledReturnEmaBySymbolNew: MutableMap[Symbol, Ema] = MutableMap[Symbol, Ema]()
    val qtyBySymbol = MutableMap[Symbol, SidedQuantity]()
    val date = ohlcBySymbol.values.head.date

    for (symbol <- this.params.symbols) {
      ohlcBySymbol.get(symbol) match {
        case Some(ohlcv: OhlcvBase) =>
          if (!this.state.volScaledReturnBySymbol.contains(symbol)) {
            // initialize volScaledReturnBySymbolNew for symbol
            volScaledReturnBySymbolNew(symbol) = VolScaledReturnIndex(VolScaledReturnIndexParams(this.params.symbolToVolLookback(symbol))).onData(ohlcv)

            // initialize volScaledReturnEmaBySymbolNew for symbol
            volScaledReturnEmaBySymbolNew(symbol) = volScaledReturnBySymbolNew(symbol).valueMaybe match {
              case Some(value: Double) =>
                Ema(EmaParams(this.params.symbolToTrendLookback(symbol))).onData(value)
              case None =>
                Ema(EmaParams(this.params.symbolToTrendLookback(symbol)))
            }

          } else {
            // update the vol scaled return
            volScaledReturnBySymbolNew(symbol) = this.state.volScaledReturnBySymbol(symbol).onData(ohlcv)

            // update the ema
            volScaledReturnEmaBySymbolNew(symbol) =
              volScaledReturnBySymbolNew(symbol).valueMaybe match {
                case Some(value: Double) =>
                  this.state.volScaledReturnEmaBySymbol(symbol).onData(value)
                case _ =>
                  this.state.volScaledReturnEmaBySymbol(symbol)
              }

            (volScaledReturnBySymbolNew(symbol).valueMaybe, volScaledReturnEmaBySymbolNew(symbol).valueMaybe) match {
              case (Some(volScaledReturn: Double), Some(volScaledReturnEma: Double)) =>
                if (volScaledReturn > volScaledReturnEma)
                  qtyBySymbol(symbol) = SidedQuantity(PosZDouble.from(1.0 * params.scaling.value / volScaledReturnBySymbolNew(symbol).state.vol.valueMaybe.get / ohlcv.close).get, Side.SideBuy)
              case _ =>
            }
          }

        case None =>
      }
    }

    val qtyBySymbolByDateNew: QtyBySymbolByDate =
      if (params.symbols.forall(symbol => ohlcBySymbol.contains(symbol)))
        this.state.qtyBySymbolByDate.updateQuantities(date, qtyBySymbol)
      else
        this.state.qtyBySymbolByDate

    TrendStrategy(
      this.params,
      TrendStrategyState(
        volScaledReturnBySymbolNew.toMap,
        volScaledReturnEmaBySymbolNew.toMap,
        qtyBySymbolByDateNew
      )
    )
  }
}


object TrendStrategy {

  def apply(
    params: TrendStrategyParams
  ): TrendStrategy = {
    val volScaledReturnBySymbol: ImmutableMap[Symbol, VolScaledReturnIndex] = ImmutableMap[Symbol, VolScaledReturnIndex]()
    val volScaledReturnEmaBySymbol: ImmutableMap[Symbol, Ema] = ImmutableMap[Symbol, Ema]()
    val qtyBySymbolByDate = QtyBySymbolByDate()
    val state = TrendStrategyState(volScaledReturnBySymbol, volScaledReturnEmaBySymbol, qtyBySymbolByDate)
    TrendStrategy(
      params,
      state
    )
  }
}
