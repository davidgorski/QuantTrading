package com.quantTrading.strategies.protectiveMomentumStrategy

import com.quantTrading.infra.{BacktestIterator, OhlcvBase, QtyBySymbolByDate, SidedQuantity, Strategy, StrategyParamsBase, StrategyState}
import com.quantTrading.signals._
import com.quantTrading.symbols.Symbol
import com.quantTrading.infra.Side.SideBuy
import org.scalactic.anyvals.{PosDouble, PosLong, PosZDouble}

import scala.collection.immutable.{Map => ImmutableMap}
import scala.collection.mutable.{Map => MutableMap}


case class ProtectiveMomentumStrategyParams(
  override val name: String,
  safeSymbols: List[Symbol],
  riskySymbols: List[Symbol],
  emaLookback: PosDouble,
  protectionFactor: PosDouble,
  nTopRiskySymbols: PosLong,
  nTopSafeSymbols: PosLong,
  override val scaling: PosDouble
) extends StrategyParamsBase {

  require(riskySymbols.forall(!safeSymbols.contains(_)))
  require(safeSymbols.forall(!riskySymbols.contains(_)))

  def symbols: List[Symbol] = safeSymbols ++ riskySymbols
}


case class ProtectiveMomentumStrategyState(
  emaBySymbol: ImmutableMap[Symbol, Ema],
  counter: Counter,
  override val qtyBySymbolByDate: QtyBySymbolByDate,
) extends StrategyState


case class ProtectiveMomentumStrategy(
  override val params: ProtectiveMomentumStrategyParams,
  override val state: ProtectiveMomentumStrategyState
) extends Strategy[ProtectiveMomentumStrategyParams, ProtectiveMomentumStrategyState] {

  override def onData(ohlcBySymbol: ImmutableMap[Symbol, OhlcvBase]): ProtectiveMomentumStrategy = {
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
      && this.params.symbols.forall(symbol => ohlcBySymbol.contains(symbol) && emaBySymbolNew(symbol).valueMaybe.isDefined)) {
      val riskySymbolsReturns: ImmutableMap[Symbol, Double] =
        this.params.riskySymbols.map(symbol => symbol -> (ohlcBySymbol(symbol).close / this.state.emaBySymbol(symbol).valueMaybe.get - 1.0)).toMap
      val safeSymbolsReturns: ImmutableMap[Symbol, Double] =
        this.params.safeSymbols.map(symbol => symbol -> (ohlcBySymbol(symbol).close / this.state.emaBySymbol(symbol).valueMaybe.get - 1.0)).toMap
      val riskySymbolsGood: List[Symbol] =
        riskySymbolsReturns
          .keys
          .filter(symbol => riskySymbolsReturns(symbol) > 0.0)
          .toList
          .sortBy(symbol => -1 * riskySymbolsReturns(symbol)) // sorted from highest return to lowest
      val safeSymbolsGood: List[Symbol] =
        safeSymbolsReturns
          .keys
          .filter(symbol => safeSymbolsReturns(symbol) > 0.0)
          .toList
          .sortBy(symbol => -1 * safeSymbolsReturns(symbol)) // sorted from highest return to lowest
      val nRiskySymbols = this.params.riskySymbols.size
      val nRiskySymbolsGood: Int = riskySymbolsGood.size
      val nSafeSymbolsGood: Int = safeSymbolsGood.size
      val safeFraction: PosZDouble = PosZDouble.from(
        math.min(1.0, (nRiskySymbols - nRiskySymbolsGood) / (nRiskySymbols - 0.25 * this.params.protectionFactor * nRiskySymbols))
      ).get

      var g = 0
      for (riskySymbolGood <- riskySymbolsGood) {
        if (g < this.params.nTopRiskySymbols.value) {
          val notional: Double = 1.0 * params.scaling.value * (1 - safeFraction) / math.min(nRiskySymbolsGood, params.nTopRiskySymbols)
          val qty: PosZDouble = PosZDouble.from(notional / ohlcBySymbol(riskySymbolGood).close).get
          val sidedQty = SidedQuantity(qty, SideBuy)
          val sidedQtyNew =
            if (qtyBySymbolNew.contains(riskySymbolGood)) {
              qtyBySymbolNew(riskySymbolGood) + sidedQty
            } else {
              sidedQty
            }
          qtyBySymbolNew(riskySymbolGood) = sidedQtyNew
        }
        g += 1
      }

      var b = 0
      for (safeSymbolGood <- safeSymbolsGood) {
        if (b < this.params.nTopSafeSymbols.value) {
          val notional: Double = 1.0 * params.scaling.value * safeFraction / math.min(nSafeSymbolsGood, params.nTopSafeSymbols)
          val qty: PosZDouble = PosZDouble.from(notional / ohlcBySymbol(safeSymbolGood).close).get
          val sidedQty = SidedQuantity(qty, SideBuy)
          val sidedQtyNew =
            if (qtyBySymbolNew.contains(safeSymbolGood)) {
              qtyBySymbolNew(safeSymbolGood) + sidedQty
            } else {
              sidedQty
            }
          qtyBySymbolNew(safeSymbolGood) = sidedQtyNew
        }
        b += 1
      }
    }

    // set the new position
    val qtyBySymbolByDateNew: QtyBySymbolByDate =
      if (params.symbols.forall(symbol => ohlcBySymbol.contains(symbol)))
        this.state.qtyBySymbolByDate.updateQuantities(date, qtyBySymbolNew)
      else
        this.state.qtyBySymbolByDate

    ProtectiveMomentumStrategy(
      this.params,
      ProtectiveMomentumStrategyState(
        emaBySymbolNew,
        counterNew,
        qtyBySymbolByDateNew
      )
    )
  }
}


object ProtectiveMomentumStrategy {

  def apply(
    params: ProtectiveMomentumStrategyParams
  ): ProtectiveMomentumStrategy = {
    val emaBySymbol: ImmutableMap[Symbol, Ema] = ImmutableMap[Symbol, Ema]()
    val qtyBySymbolByDate = QtyBySymbolByDate()
    val counter = Counter(CounterParams())
    val state = ProtectiveMomentumStrategyState(emaBySymbol, counter, qtyBySymbolByDate)
    ProtectiveMomentumStrategy(
      params,
      state
    )
  }
}
