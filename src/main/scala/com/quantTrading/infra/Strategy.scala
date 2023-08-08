package com.quantTrading.infra

import scala.collection.immutable.{Map => ImmutableMap}
import com.quantTrading.symbols.Symbol

trait Strategy[
  This <: Strategy[This, Params, State],
  +Params <: ParamsBase,
  +State <: StateBase
] extends BacktestIterator[ImmutableMap[Symbol, OhlcvBase], Unit, This, Params, State] {

  this: This =>

  override def valueMaybe: Unit = ()

  override def onData(ohlcBySymbol: ImmutableMap[Symbol, OhlcvBase]): This
}
