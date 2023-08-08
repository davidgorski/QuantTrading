package com.quantTrading.infra

import com.quantTrading.infra.Side.{Side, SideBuy, SideSell}
import com.quantTrading.symbols.Symbol
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation
import org.scalactic.anyvals.{PosDouble, PosZDouble}
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation

import java.time.LocalDate
import scala.collection.immutable.{Map => ImmutableMap, SortedMap => ImmutableSortedMap}
import scala.collection.mutable.{Map => MutableMap, SortedMap => MutableSortedMap}


trait StrategyState extends StateBase {

  def qtyBySymbolByDate: QtyBySymbolByDate

  def getStrategyResult(
    ohlcByDateBySymbol: ImmutableSortedMap[LocalDate, ImmutableMap[Symbol, OhlcvBase]]
  ): StrategyResult = {

    val pnlByDateMut: MutableSortedMap[LocalDate, Double] = MutableSortedMap[LocalDate, Double]()(Ordering.by(_.toEpochDay))

    val dates = qtyBySymbolByDate.qtyBySymbolByDate.keys.toList

    var i: Int = 0
    var inTheMarketCount: Long = 0
    var notInTheMarketCount: Long = 0

    for (dateTday <- dates) {
      var pnl: Double = 0.0
      if (i > 0) {
        val dateYday: LocalDate = dates(i-1)
        var inTheMarket: Boolean = false
        for (symbol <- ohlcByDateBySymbol(dateTday).keys) {
          val qtyYdayMaybe: Option[SidedQuantity] = qtyBySymbolByDate.qtyBySymbolByDate(dateYday).get(symbol)
          val ohlcvYdayMaybe: Option[OhlcvBase] = ohlcByDateBySymbol(dateYday).get(symbol)
          val ohlcvTdayMaybe: Option[OhlcvBase] = ohlcByDateBySymbol(dateTday).get(symbol)
          val pnlNew: Double = (qtyYdayMaybe, ohlcvYdayMaybe, ohlcvTdayMaybe) match {

              case (Some(SidedQuantity(qtyYday: PosZDouble, side: Side)), Some(ohlcvYday: OhlcvBase), Some(ohlcvTday: OhlcvBase)) =>
                inTheMarket = inTheMarket || (qtyYday > 0.0)
                (ohlcvTday.close.value - ohlcvYday.close.value) * qtyYday.value * side.toInt

              case (Some(SidedQuantity(qty: PosZDouble, _)), _, _) =>
                if (qty.value != 0.0)
                  throw new RuntimeException("Cannot have a nonzero position")
                else
                  0.0

              case _ =>
                0.0
            }
          pnl += pnlNew
        }
        inTheMarketCount += (if (inTheMarket) 1L else 0L)
        notInTheMarketCount += (if (inTheMarketCount > 0 && !inTheMarket) 1L else 0L)
      }
      pnlByDateMut(dateTday) = pnl
      i += 1
    }

    val stdDevOperator: StandardDeviation = new StandardDeviation(false)
    val stdDev: Double = stdDevOperator.evaluate(pnlByDateMut.values.toArray)
    val mean: Double = pnlByDateMut.values.sum / pnlByDateMut.size.toDouble
    val sharpe: Double = mean / stdDev * math.sqrt(252.0)
    val pctInMkt: PosZDouble = PosZDouble.from(inTheMarketCount.toDouble / (inTheMarketCount + notInTheMarketCount).toDouble).get
    val pnlPerYear: Double = pnlByDateMut.values.sum * 365.2425 / (pnlByDateMut.keys.map(_.toEpochDay).max - pnlByDateMut.keys.map(_.toEpochDay).min)
    val corrOperator: PearsonsCorrelation = new PearsonsCorrelation()
    val dateIndex: List[Double] = pnlByDateMut.values.toList.scanLeft(0.0)((l, _) => l + 1.0)
    val pnlRunningCum: List[Double] = pnlByDateMut.values.toList.scanLeft(0.0)((l, r) => l + r)
    val linearity: Double = corrOperator.correlation(dateIndex.toArray, pnlRunningCum.toArray)
    val annualizedVol: Double = math.sqrt(252.0) * stdDev
    val pnlByDate: ImmutableSortedMap[LocalDate, Double] = ImmutableSortedMap(pnlByDateMut.toArray:_*)(Ordering.by(_.toEpochDay))
    val startDate: LocalDate = pnlByDate.keys.minBy(_.toEpochDay)
    val endDate: LocalDate = pnlByDate.keys.maxBy(_.toEpochDay)

    val strategyPerformance = StrategyResult(
      pnlByDate,
      sharpe,
      pctInMkt,
      linearity,
      pnlPerYear,
      annualizedVol,
      startDate,
      endDate
    )

    strategyPerformance
  }
}

case class QtyBySymbolByDate(
  qtyBySymbolByDate: ImmutableSortedMap[LocalDate, ImmutableMap[Symbol, SidedQuantity]] = ImmutableSortedMap[LocalDate, ImmutableMap[Symbol, SidedQuantity]]()(Ordering.by(_.toEpochDay))
) {

  def +(that: QtyBySymbolByDate): QtyBySymbolByDate = {
    val localDates = this.qtyBySymbolByDate.keySet ++ that.qtyBySymbolByDate.keySet
    val immutableMap: ImmutableMap[LocalDate, ImmutableMap[Symbol, SidedQuantity]] =
      localDates
      .map { localDate =>
        val thisSymbolMap = this.qtyBySymbolByDate.getOrElse(localDate, ImmutableMap[Symbol, SidedQuantity]())
        val thatSymbolMap = that.qtyBySymbolByDate.getOrElse(localDate, ImmutableMap[Symbol, SidedQuantity]())
        val keySetInner: Set[Symbol] = thisSymbolMap.keySet ++ thatSymbolMap.keySet
        val symbolToSidedQty: ImmutableMap[Symbol, SidedQuantity] =
          keySetInner.map { symbol =>
            val sidedQtyNew = thisSymbolMap.getOrElse(symbol, SidedQuantity.empty) + thatSymbolMap.getOrElse(symbol, SidedQuantity.empty)
            (symbol, sidedQtyNew)
          }.toMap
        (localDate, symbolToSidedQty)
      }.toMap
    val sortedMap: ImmutableSortedMap[LocalDate, ImmutableMap[Symbol, SidedQuantity]] = (
      ImmutableSortedMap.empty[LocalDate, ImmutableMap[Symbol, SidedQuantity]](Ordering.by(_.toEpochDay))
      ++ immutableMap
    )
    QtyBySymbolByDate(sortedMap)
  }

  def *(scalar: PosDouble): QtyBySymbolByDate = {

    val immutableMap: ImmutableMap[LocalDate, ImmutableMap[Symbol, SidedQuantity]] =
      this.qtyBySymbolByDate.map { kv =>
        val localDate: LocalDate = kv._1
        val qtyBySymbol: ImmutableMap[Symbol, SidedQuantity] = kv._2
        (localDate, qtyBySymbol.map { kvInner => (kvInner._1, kvInner._2 * scalar) } )
      }

    val qtyBySymbolByDateNew: ImmutableSortedMap[LocalDate, ImmutableMap[Symbol, SidedQuantity]] = (
      ImmutableSortedMap.empty[LocalDate, ImmutableMap[Symbol, SidedQuantity]](Ordering.by(_.toEpochDay))
        ++ immutableMap
      )

    QtyBySymbolByDate(qtyBySymbolByDateNew)
  }

  def updateNotionals(
    date: LocalDate,
    ohlcvBySymbol: ImmutableMap[Symbol, OhlcvBase],
    notionalBySymbol: MutableMap[Symbol, Double]
  ): QtyBySymbolByDate = {
    updateNotionals(date, ohlcvBySymbol, notionalBySymbol.toMap)
  }

  def updateNotionals(
    date: LocalDate,
    ohlcvBySymbol: ImmutableMap[Symbol, OhlcvBase],
    notionalBySymbol: ImmutableMap[Symbol, Double]
  ): QtyBySymbolByDate = {

    assert(!this.qtyBySymbolByDate.contains(date))
    val qtyBySymbol = MutableMap[Symbol, SidedQuantity]()
    for (symbol <- notionalBySymbol.keys) {
      val close = ohlcvBySymbol(symbol).close.value
      val quantity = PosZDouble.from(math.abs(notionalBySymbol(symbol)) / close).get
      val side = if (notionalBySymbol(symbol) > 0.0) SideBuy else SideSell
      val sidedQuantity = SidedQuantity(quantity, side)
      qtyBySymbol += (symbol -> sidedQuantity)
    }
    val qtyBySymbolByDateNew = qtyBySymbolByDate + (date -> qtyBySymbol.toMap)
    QtyBySymbolByDate(qtyBySymbolByDateNew)
  }

  def updateQuantities(
    date: LocalDate,
    qtyBySymbol: MutableMap[Symbol, SidedQuantity]
  ): QtyBySymbolByDate = {

    assert(!this.qtyBySymbolByDate.contains(date))
    val qtyBySymbolByDateNew = qtyBySymbolByDate + (date -> qtyBySymbol.toMap)
    QtyBySymbolByDate(qtyBySymbolByDateNew)
  }
}
