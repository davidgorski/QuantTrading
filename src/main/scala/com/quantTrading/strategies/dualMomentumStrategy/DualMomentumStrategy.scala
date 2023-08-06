package com.quantTrading.strategies.dualMomentumStrategy

import com.quantTrading.infra.{BacktestIterator, OhlcvBase, QtyBySymbolByDate, SidedQuantity, Strategy, StrategyParamsBase, StrategyState}
import com.quantTrading.signals._
import com.quantTrading.symbols.Symbol
import com.quantTrading.infra.Side.SideBuy
import org.scalactic.anyvals.{PosDouble, PosZDouble}

import java.time.LocalDate
import scala.collection.immutable.{Map => ImmutableMap}
import scala.collection.mutable.{Map => MutableMap}


case class DualMomentumStrategyParams(
  override val name: String,
  symbolGroups: List[List[Symbol]],
  emaLookback: PosDouble,
  override val scaling: PosDouble
) extends StrategyParamsBase {

  def symbols: List[Symbol] = symbolGroups.flatten
}


case class DualMomentumStrategyState(
  emaBySymbol: ImmutableMap[Symbol, Ema],
  counter: Counter,
  qtyBySymbolByDate: QtyBySymbolByDate,
) extends StrategyState


case class DualMomentumStrategy(
  override val params: DualMomentumStrategyParams,
  override val state: DualMomentumStrategyState
) extends Strategy[DualMomentumStrategy, DualMomentumStrategyParams, DualMomentumStrategyState] {

  override def onData(ohlcBySymbol: ImmutableMap[Symbol, OhlcvBase]): DualMomentumStrategy = {
    val emaBySymbolNewMut: MutableMap[Symbol, Ema] = MutableMap[Symbol, Ema]()
    val date = ohlcBySymbol.values.head.date

    for (symbol <- this.params.symbols) {
      ohlcBySymbol.get(symbol) match {
        case Some(ohlcv: OhlcvBase) =>
          if (!this.state.emaBySymbol.contains(symbol))
            emaBySymbolNewMut(symbol) = Ema(EmaParams(this.params.emaLookback)).onData(ohlcv.close)
          else
            emaBySymbolNewMut(symbol) = this.state.emaBySymbol(symbol).onData(ohlcv.close)

        case None =>
      }
    }

    val emaBySymbolNew: ImmutableMap[Symbol, Ema] = emaBySymbolNewMut.toMap

    val counterNew: Counter =
      if (this.params.symbols.forall(symbol => ohlcBySymbol.contains(symbol) && emaBySymbolNew(symbol).valueMaybe.isDefined))
        this.state.counter.onData(())
      else
        this.state.counter

    // wait for all symbols to have all data
    val qtyBySymbolNew = MutableMap[Symbol, SidedQuantity]()
    if (counterNew.valueMaybe.value > 0.5 * this.params.emaLookback
      && this.params.symbols.forall(symbol => ohlcBySymbol.contains(symbol) && emaBySymbolNew(symbol).valueMaybe.isDefined))
    {
      for (symbolGroup <- this.params.symbolGroups) {
        val maxReturn: Double = symbolGroup.map(symbol => ohlcBySymbol(symbol).close / emaBySymbolNew(symbol).valueMaybe.get).max
        val maxReturnSymbols: List[Symbol] = symbolGroup.filter(symbol => math.abs(maxReturn - ohlcBySymbol(symbol).close / emaBySymbolNew(symbol).valueMaybe.get ) < 0.0001)
        maxReturnSymbols.foreach(maxReturnSymbol => {
          val notionalIncremental: Double = 1.0 * params.scaling.value / this.params.symbolGroups.size / maxReturnSymbols.size
          val qtyIncremental: PosZDouble = PosZDouble.from(notionalIncremental / ohlcBySymbol(maxReturnSymbol).close).get
          val sidedQtyIncremental = SidedQuantity(qtyIncremental, SideBuy)
          val sidedQtyNew =
            if (qtyBySymbolNew.contains(maxReturnSymbol)) {
              qtyBySymbolNew(maxReturnSymbol) + sidedQtyIncremental
            } else {
              sidedQtyIncremental
            }
          qtyBySymbolNew(maxReturnSymbol) = sidedQtyNew
        })
      }
    }

    // set the new position
    val qtyBySymbolByDateNew: QtyBySymbolByDate =
      if (params.symbols.forall(symbol => ohlcBySymbol.contains(symbol)))
        this.state.qtyBySymbolByDate.updateQuantities(date, qtyBySymbolNew)
      else
        this.state.qtyBySymbolByDate

    DualMomentumStrategy(
      this.params,
      DualMomentumStrategyState(
        emaBySymbolNew,
        counterNew,
        qtyBySymbolByDateNew
      )
    )
  }
}


object DualMomentumStrategy {

  def apply(
    params: DualMomentumStrategyParams
  ): DualMomentumStrategy = {
    val emaBySymbol: ImmutableMap[Symbol, Ema] = ImmutableMap[Symbol, Ema]()
    val qtyBySymbolByDate = QtyBySymbolByDate()
    val counter = Counter(CounterParams())
    val state = DualMomentumStrategyState(emaBySymbol, counter, qtyBySymbolByDate)
    DualMomentumStrategy(
      params,
      state
    )
  }
}
