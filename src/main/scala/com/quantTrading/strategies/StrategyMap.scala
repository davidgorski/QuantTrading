//package com.quantTrading.strategies
//
//import com.quantTrading.infra.{Strategy, StrategyParamsBase, StrategyState}
//import com.quantTrading.strategies.compositeStrategy.{CompositeStrategy, CompositeStrategyParams}
//import com.quantTrading.strategies.dualMomentumStrategy.{DualMomentumStrategy, DualMomentumStrategyParams}
//import com.quantTrading.strategies.ibsStrategy.{IbsStrategy, IbsStrategyParams}
//import com.quantTrading.strategies.protectiveMomentumStrategy.{ProtectiveMomentumStrategy, ProtectiveMomentumStrategyParams}
//import com.quantTrading.strategies.rsiStrategy.{RsiStrategy, RsiStrategyParams}
//import com.quantTrading.strategies.trendStrategy.{TrendStrategy, TrendStrategyParams}
//import com.quantTrading.strategies.xassetStrategy.{XassetStrategy, XassetStrategyParams}
//import com.quantTrading.symbols.Symbol
//import org.scalactic.anyvals.{PosDouble, PosLong}
//
//import scala.collection.mutable.{Map => MutableMap}
//
//
//object StrategyMap {
//
//  private type StrategyGeneratorType = () => Strategy[StrategyParamsBase, StrategyState]
//
//  // these are generators so that we can easily construct the composite strategies by recreating the base strategies
//  // if we don't do it this way then we will have to copy the objects
//  private val strategyGenerators: List[StrategyGeneratorType] = List[StrategyGeneratorType](
//
//    () => RsiStrategy(RsiStrategyParams(
//      "RSI SPY",
//      PosDouble(5.0),
//      Symbol.SPY,
//      0.20,
//      PosDouble(1062575.05)
//    )),
//
//    () => TrendStrategy(TrendStrategyParams(
//      "Trend Strategy",
//      List[Symbol](Symbol.SPY, Symbol.IEF, Symbol.GLD).map((_, PosDouble.from(252).get)).toMap,
//      List[Symbol](Symbol.SPY, Symbol.IEF, Symbol.GLD).map((_, PosDouble.from(22).get)).toMap,
//      PosDouble(4068.348)
//    )),
//
//    () => DualMomentumStrategy(DualMomentumStrategyParams(
//      "Dual Momentum",
//      List[List[Symbol]](
//        List[Symbol](Symbol.SPY, Symbol.QQQ, Symbol.BIL),
//        List[Symbol](Symbol.TLT, Symbol.IEF, Symbol.BIL),
//        List[Symbol](Symbol.HYG, Symbol.LQD, Symbol.BIL),
//      ),
//      PosDouble(252.0),
//      PosDouble(1428571.43)
//    )),
//
//    () => ProtectiveMomentumStrategy(ProtectiveMomentumStrategyParams(
//      "Protective Momentum",
//      List[Symbol](Symbol.BIL, Symbol.IEF),
//      List[Symbol](Symbol.SPY, Symbol.QQQ, Symbol.IWM, Symbol.EZU, Symbol.EWJ, Symbol.EEM, Symbol.GLD, Symbol.HYG, Symbol.LQD, Symbol.TLT),
//      PosDouble(252.0),
//      PosDouble(2.0),
//      PosLong(6L),
//      PosLong(1L),
//      PosDouble(1250000.0)
//    )),
//
//    () => IbsStrategy(IbsStrategyParams(
//      "IBS SPY",
//      PosLong(5L),
//      Symbol.SPY,
//      0.25,
//      PosDouble(909090.91)
//    )),
//
//    () => IbsStrategy(IbsStrategyParams(
//      "IBS QQQ",
//      PosLong(10L),
//      Symbol.QQQ,
//      0.10,
//      PosDouble(833333.33)
//    )),
//
//    () => IbsStrategy(IbsStrategyParams(
//      "IBS TLT",
//      PosLong(5L),
//      Symbol.TLT,
//      0.10,
//      PosDouble(2000000.00)
//    )),
//
//    () => IbsStrategy(IbsStrategyParams(
//      "IBS IEF",
//      PosLong(5L),
//      Symbol.IEF,
//      0.10,
//      PosDouble(5000000.00)
//    )),
//
//    () => XassetStrategy(XassetStrategyParams(
//      "Xasset SPY TLT",
//      PosDouble(5.0),
//      Symbol.SPY,
//      Symbol.TLT,
//      PosDouble(1111111.11)
//    )),
//
//    () => XassetStrategy(XassetStrategyParams(
//      "Xasset SPY IEF",
//      PosDouble(5.0),
//      Symbol.SPY,
//      Symbol.IEF,
//      PosDouble(1111111.11)
//    )),
//
//    () => XassetStrategy(XassetStrategyParams(
//      "Xasset QQQ TLT",
//      PosDouble(5.0),
//      Symbol.QQQ,
//      Symbol.TLT,
//      PosDouble(1000000.00)
//    )),
//
//    () => XassetStrategy(XassetStrategyParams(
//      "Xasset QQQ IEF",
//      PosDouble(5.0),
//      Symbol.QQQ,
//      Symbol.IEF,
//      PosDouble(1000000.00)
//    )),
//
//    () => XassetStrategy(XassetStrategyParams(
//      "Xasset SPY HYG",
//      PosDouble(5.0),
//      Symbol.SPY,
//      Symbol.HYG,
//      PosDouble(3333333.33)
//    )),
//
//    () => XassetStrategy(XassetStrategyParams(
//      "Xasset QQQ HYG",
//      PosDouble(5.0),
//      Symbol.QQQ,
//      Symbol.HYG,
//      PosDouble(2500000.00)
//    ))
//  )
//
//  private val compositeStrategyGen: List[StrategyGeneratorType] = List[StrategyGeneratorType](
//    () => CompositeStrategy(CompositeStrategyParams(
//      "CompositeStrategy",
//      strategyGenerators.map(_.apply()),
//      PosDouble(1000000.0)
//    ))
//  )
//
//  // this includes the composite strategies
//  def getStrategyMap: MutableMap[String, Strategy[StrategyParamsBase, StrategyState]] = {
//
//    // base strategies
//    val baseStrategies: List[Strategy[StrategyParamsBase, StrategyState]] = strategyGenerators.map(_.apply())
//
//    // composite strategies
//    val compositeStrategies: List[Strategy[StrategyParamsBase, StrategyState]] = compositeStrategyGen.map(_.apply())
//
//    // all strategies
//    val allStrategies = baseStrategies ++ compositeStrategies
//
//    val strategyMap = MutableMap(
//      allStrategies.map(strategy => (strategy.params.name, strategy)):_*
//    )
//
//    strategyMap
//  }
//}
