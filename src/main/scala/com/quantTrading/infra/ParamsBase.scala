package com.quantTrading.infra

import org.scalactic.anyvals.PosDouble

trait ParamsBase

trait StrategyParamsBase extends ParamsBase {
  def name: String
  def scaling: PosDouble
}
