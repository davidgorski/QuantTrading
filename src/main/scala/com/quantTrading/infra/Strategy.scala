package com.quantTrading.infra

import scala.collection.immutable.{Map => ImmutableMap}
import com.quantTrading.symbols.Symbol

trait Strategy[+Params <: ParamsBase, +State <: StateBase]
  extends BacktestIterator[ImmutableMap[Symbol, OhlcvBase], Unit, Params, State] {

  override def valueMaybe: Unit = ()

  override def onData(ohlcBySymbol: ImmutableMap[Symbol, OhlcvBase]): Strategy[Params, State]
}
