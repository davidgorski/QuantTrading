package com.quantTrading.signals

import com.quantTrading.infra.{BacktestIterator, ParamsBase, StateBase}
import org.scalactic.anyvals.PosZLong


case class CounterParams() extends ParamsBase


case class CounterState(value: PosZLong = PosZLong(0L)) extends StateBase


case class Counter(
  params: CounterParams,
  state: CounterState = CounterState(),
) extends BacktestIterator[Unit, PosZLong, Counter, CounterParams, CounterState] {

  override def onData(input: Unit): Counter = {
    Counter(params, CounterState(PosZLong.from(this.state.value + 1L).get))
  }

  override def valueMaybe: PosZLong = state.value
}
