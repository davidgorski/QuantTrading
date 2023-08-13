package com.quantTrading.infra

/**
 *
 * @tparam Input      the input type into onData
 * @tparam ValueMaybe the output type from valueMaybe
 * @tparam Params     the params type (a subtype of ParamsBase)
 * @tparam State      the state type (a subtype of StateBase)
 */
trait BacktestIterator[Input, ValueMaybe, +Params <: ParamsBase, +State <: StateBase] {

  def params: Params

  def state: State

  def valueMaybe: ValueMaybe

  def onData(input: Input): BacktestIterator[Input, ValueMaybe, Params, State]
}
