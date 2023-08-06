package com.quantTrading.strategies.xassetStrategy

import com.quantTrading.infra.{BacktestIterator, OhlcvBase, QtyBySymbolByDate, SidedQuantity, Strategy, StrategyParamsBase, StrategyState}
import com.quantTrading.signals._
import com.quantTrading.symbols.Symbol
import com.quantTrading.infra.Side.SideBuy
import org.scalactic.anyvals.{PosDouble, PosLong, PosZDouble}

import scala.collection.immutable.{Map => ImmutableMap}
import scala.collection.mutable.{Map => MutableMap}


case class XassetStrategyParams(
  override val name: String,
  emaLookback: PosDouble,
  mainSymbol: Symbol,
  otherSymbol: Symbol,
  override val scaling: PosDouble
) extends StrategyParamsBase {

  require(!mainSymbol.equals(otherSymbol))
}


case class XassetStrategyState(
  mainSymbolEma: Ema,
  otherSymbolEma: Ema,
  notionalEma: Ema,
  qtyBySymbolByDate: QtyBySymbolByDate,
) extends StrategyState


case class XassetStrategy(
  override val params: XassetStrategyParams,
  override val state: XassetStrategyState
) extends Strategy[XassetStrategy, XassetStrategyParams, XassetStrategyState] {

  override def onData(ohlcBySymbol: ImmutableMap[Symbol, OhlcvBase]): XassetStrategy = {
    val date = ohlcBySymbol.values.head.date

    val mainSymbolEmaNew: Ema =
      if (ohlcBySymbol.contains(params.mainSymbol))
        state.mainSymbolEma.onData(ohlcBySymbol(params.mainSymbol).close)
      else
        state.mainSymbolEma

    val otherSymbolEmaNew: Ema =
      if (ohlcBySymbol.contains(params.otherSymbol))
        state.otherSymbolEma.onData(ohlcBySymbol(params.otherSymbol).close)
      else
        state.otherSymbolEma

    val qtyBySymbolNew = MutableMap[Symbol, SidedQuantity]()
    val notionalMaybe: Option[Double] =
      if (ohlcBySymbol.contains(params.mainSymbol)
        && ohlcBySymbol.contains(params.otherSymbol)
        && mainSymbolEmaNew.valueMaybe.isDefined
        && otherSymbolEmaNew.valueMaybe.isDefined
      ) {
        val mainSymbolReturn = ohlcBySymbol(params.mainSymbol).close / mainSymbolEmaNew.valueMaybe.get - 1
        val otherSymbolReturn = ohlcBySymbol(params.otherSymbol).close / otherSymbolEmaNew.valueMaybe.get - 1
        val notional: Double = params.scaling * (if (mainSymbolReturn < 0.0 && otherSymbolReturn > 0.0) 1.0 else 0.0)
        Some(notional)
      } else {
        None
      }

    val notionalEmaNew =
      notionalMaybe match {
        case Some(notional: Double) =>
          val notionalEmaNew = this.state.notionalEma.onData(notional)
          val qty: PosZDouble = PosZDouble.from(notionalEmaNew.valueMaybe.get / ohlcBySymbol(params.mainSymbol).close).get
          val sidedQty = SidedQuantity(qty, SideBuy)
          qtyBySymbolNew(params.mainSymbol) = sidedQty
          notionalEmaNew

        case None =>
          this.state.notionalEma
      }

    // set the new position
    val qtyBySymbolByDateNew: QtyBySymbolByDate = this.state.qtyBySymbolByDate.updateQuantities(date, qtyBySymbolNew)

    XassetStrategy(
      this.params,
      XassetStrategyState(
        mainSymbolEmaNew,
        otherSymbolEmaNew,
        notionalEmaNew,
        qtyBySymbolByDateNew
      )
    )
  }
}

object XassetStrategy {

  def apply(
    params: XassetStrategyParams
  ): XassetStrategy = {
    val mainSymbolEma = Ema(EmaParams(params.emaLookback))
    val otherSymbolEma = Ema(EmaParams(params.emaLookback))
    val notionalEma = Ema(EmaParams(params.emaLookback))
    val qtyBySymbolByDate = QtyBySymbolByDate()
    val state = XassetStrategyState(mainSymbolEma, otherSymbolEma, notionalEma, qtyBySymbolByDate)
    XassetStrategy(
      params,
      state
    )
  }
}
