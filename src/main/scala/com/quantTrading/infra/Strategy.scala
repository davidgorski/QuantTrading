package com.quantTrading.infra

import scala.collection.immutable.{Map => ImmutableMap}
import com.quantTrading.symbols.QtSymbol

trait Strategy[+Params <: ParamsBase, +State <: StateBase]
  extends BacktestIterator[ImmutableMap[QtSymbol, OhlcvBase], Unit, Params, State] {

  override def valueMaybe: Unit = ()

  override def onData(ohlcBySymbol: ImmutableMap[QtSymbol, OhlcvBase]): Strategy[Params, State]
}
