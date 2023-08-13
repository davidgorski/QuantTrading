package com.quantTrading.infra

import com.quantTrading.symbols.Symbol
import org.mongodb.scala.bson.ObjectId
import org.scalactic.anyvals.PosZDouble

import scala.collection.immutable.{Map => ImmutableMap, SortedMap => ImmutableSortedMap}
import java.time.LocalDate

case class StrategyResultSerializable(
  _id: ObjectId,
  pnlByDateStringType: ImmutableMap[String, Double],
  sharpe: Double,
  pctInMarket: Double,
  linearity: Double,
  pnlPerYear: Double,
  annualizedVol: Double,
  startDate: LocalDate,
  endDate: LocalDate,
  notionalBySymbolTday: ImmutableMap[String, Double],
  notionalBySymbolYday: ImmutableMap[String, Double]
)
object StrategyResultSerializable {
  def apply(strategyResult: StrategyResult): StrategyResultSerializable = {
    StrategyResultSerializable(
      new ObjectId(),
      ImmutableMap(strategyResult.pnlByDate.map(kv => (kv._1.toString, kv._2)).toArray:_*),
      strategyResult.sharpe,
      strategyResult.pctInMarket.toDouble,
      strategyResult.linearity,
      strategyResult.pnlPerYear,
      strategyResult.annualizedVol,
      strategyResult.startDate,
      strategyResult.endDate,
      ImmutableMap(strategyResult.notionalBySymbolTday.map(kv => (kv._1.toString, kv._2.toSignedDouble)).toArray:_*),
      ImmutableMap(strategyResult.notionalBySymbolYday.map(kv => (kv._1.toString, kv._2.toSignedDouble)).toArray:_*)
    )
  }
}

case class StrategyResult(
  pnlByDate: ImmutableSortedMap[LocalDate, Double],
  sharpe: Double,
  pctInMarket: PosZDouble,
  linearity: Double,
  pnlPerYear: Double,
  annualizedVol: Double,
  startDate: LocalDate,
  endDate: LocalDate,
  notionalBySymbolTday: ImmutableMap[Symbol, SidedNotional],
  notionalBySymbolYday: ImmutableMap[Symbol, SidedNotional]
) {
  override def toString: String = {
    List[String](
      f"sharpe=$sharpe%.2f",
      f"pctInMarket=${pctInMarket.value}%.2f",
      f"linearity=$linearity%.2f",
      f"pnlPerYear=$pnlPerYear%.2f",
      f"annualizedVol=$annualizedVol%.2f",
      f"startDate=$startDate",
      f"endDate=$endDate"
    )
      .mkString(" | ")
  }
}
object StrategyResult {
  def apply(strategyResultSerializable: StrategyResultSerializable): StrategyResult = {

    val pnlSortedMap = ImmutableSortedMap[LocalDate, Double](
      strategyResultSerializable.pnlByDateStringType.map(kv => (LocalDate.parse(kv._1) -> kv._2)).toArray:_*
    )(Ordering.by(_.toEpochDay))

    val notionalBySymbolTday = ImmutableMap[Symbol, SidedNotional](
      strategyResultSerializable.notionalBySymbolTday.map(kv =>
        (Symbol.fromString(kv._1) -> SidedNotional(kv._2))
      ).toArray: _*
    )

    val notionalBySymbolYday = ImmutableMap[Symbol, SidedNotional](
      strategyResultSerializable.notionalBySymbolYday.map(kv =>
        (Symbol.fromString(kv._1) -> SidedNotional(kv._2))
      ).toArray: _*
    )

    StrategyResult(
      pnlSortedMap,
      strategyResultSerializable.sharpe,
      PosZDouble.from(strategyResultSerializable.pctInMarket).get,
      strategyResultSerializable.linearity,
      strategyResultSerializable.pnlPerYear,
      strategyResultSerializable.annualizedVol,
      strategyResultSerializable.startDate,
      strategyResultSerializable.endDate,
      notionalBySymbolTday,
      notionalBySymbolYday
    )
  }
}