package com.quantTrading.infra

import org.mongodb.scala.bson.ObjectId
import org.scalactic.anyvals.PosZDouble
import scala.collection.immutable.{SortedMap => ImmutableSortedMap, Map => ImmutableMap}
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
  endDate: LocalDate
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
      strategyResult.endDate
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
  endDate: LocalDate
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

    StrategyResult(
      pnlSortedMap,
      strategyResultSerializable.sharpe,
      PosZDouble.from(strategyResultSerializable.pctInMarket).get,
      strategyResultSerializable.linearity,
      strategyResultSerializable.pnlPerYear,
      strategyResultSerializable.annualizedVol,
      strategyResultSerializable.startDate,
      strategyResultSerializable.endDate
    )
  }
}