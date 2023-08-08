package com.quantTrading.strategies.compositeStrategy

import com.quantTrading.infra.{OhlcvBase, QtyBySymbolByDate, SidedQuantity, Strategy, StrategyParamsBase, StrategyState}
import com.quantTrading.scalaUtils.SetOnce
import com.quantTrading.symbols.Symbol
import org.scalactic.anyvals.PosDouble
import java.time.LocalDate
import scala.collection.immutable.{Map => ImmutableMap, SortedMap => ImmutableSortedMap}
import scala.collection.mutable.{Map => MutableMap}


case class CompositeStrategyParams(
  override val name: String,
  strategyList: List[Strategy[_, StrategyParamsBase, StrategyState]],
  override val scaling: PosDouble,
) extends StrategyParamsBase {

}


case class CompositeStrategyState(
  strategyList: List[Strategy[_, StrategyParamsBase, StrategyState]],
  scaling: PosDouble,
  qtyBySymbolByDateSetOnce: SetOnce[QtyBySymbolByDate]
) extends StrategyState {

  private def qtyBySymbolByDateSetter(): Unit = {
    val resultMutable = MutableMap[LocalDate, MutableMap[Symbol, SidedQuantity]]()

    strategyList.foreach { (strategy: Strategy[_, StrategyParamsBase, StrategyState]) =>

      strategy.state.qtyBySymbolByDate.qtyBySymbolByDate.foreach { dateToSymbolMap =>
        val date: LocalDate = dateToSymbolMap._1
        val symbolMap: ImmutableMap[Symbol, SidedQuantity] = dateToSymbolMap._2
        if (!resultMutable.contains(date))
          resultMutable(date) = MutableMap[Symbol, SidedQuantity]()

        symbolMap.foreach { symbolSidedQuantity =>
          val symbol: Symbol = symbolSidedQuantity._1
          val sidedQuantity: SidedQuantity = symbolSidedQuantity._2
          resultMutable(date)(symbol) = resultMutable(date).getOrElse(symbol, SidedQuantity.empty) + sidedQuantity
        }
      }
    }

    val qtyBySymbolByDateUnscaledMap: ImmutableSortedMap[LocalDate, ImmutableMap[Symbol, SidedQuantity]] = (
      ImmutableSortedMap.empty[LocalDate, ImmutableMap[Symbol, SidedQuantity]](Ordering.by(_.toEpochDay))
        ++ resultMutable.map { kv => (kv._1, kv._2.toMap) }.toMap
      )

    val qtyBySymbolByDateUnscaled = QtyBySymbolByDate(qtyBySymbolByDateUnscaledMap)

    val qtyBySymbolByDateScaled: QtyBySymbolByDate =
      qtyBySymbolByDateUnscaled * this.scaling

    this.qtyBySymbolByDateSetOnce.set(qtyBySymbolByDateScaled)
  }

  override def qtyBySymbolByDate: QtyBySymbolByDate = {
    if (this.qtyBySymbolByDateSetOnce.isUnset)
      this.qtyBySymbolByDateSetter
    this.qtyBySymbolByDateSetOnce.get
  }
}


case class CompositeStrategy(
  override val params: CompositeStrategyParams,
  override val state: CompositeStrategyState
) extends Strategy[CompositeStrategy, CompositeStrategyParams, CompositeStrategyState] {

  override def onData(ohlcBySymbol: ImmutableMap[Symbol, OhlcvBase]): CompositeStrategy = {
    val strategyListNew: List[Strategy[_, StrategyParamsBase, StrategyState]] =
      state
        .strategyList
        .map(_.onData(ohlcBySymbol))
        .collect { case s: Strategy[_, StrategyParamsBase, StrategyState] => s }

      CompositeStrategy(this.params, this.state.copy(strategyListNew))
  }
}

object CompositeStrategy {
  def apply(
    params: CompositeStrategyParams
  ): CompositeStrategy = {

    val state = CompositeStrategyState(
      params.strategyList,
      params.scaling,
      new SetOnce[QtyBySymbolByDate](None)
    )

    CompositeStrategy(
      params,
      state
    )
  }
}