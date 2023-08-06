package com.quantTrading.infra

/**
 *
 * @tparam Input      the input type into onData
 * @tparam ValueMaybe the output type from valueMaybe
 * @tparam This       this type; outputted from onData
 * @tparam Params     the params type (a subtype of ParamsBase)
 * @tparam State      the state type (a subtype of StateBase)
 */
trait BacktestIterator[Input, ValueMaybe, This <: BacktestIterator[_, _, This, _, _], Params <: ParamsBase, State <: StateBase] {

  this: This =>

  def params: Params

  def state: State

  def valueMaybe: ValueMaybe

  def onData(input: Input): This
}
