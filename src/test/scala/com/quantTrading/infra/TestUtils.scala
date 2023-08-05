package com.quantTrading.infra

import com.quantTrading.Utils
import org.scalactic.anyvals.{PosDouble, PosZDouble, PosZLong}
import com.quantTrading.symbols.Symbol

import java.time.LocalDate
import scala.collection.mutable.ListBuffer
import scala.util.Random

object TestUtils {

  var rand = new Random(0)

  /**
   * Reset the seed so that changing the order of tests (eg) doesn't affect the random numbers passed
   * which makes it easier to confirm behaviors
   */
  def resetSeed: Unit = {
    this.rand = new Random(0)
  }

  /**
   * Inclusive of lb
   * Exclusive of ub
   */
  def uniformRandom(lb: Double, ub: Double): Double = {
    require(ub > lb)
    lb + (ub - lb) * rand.nextDouble()
  }

  def getRandomSymbol: Symbol = {
    Symbol.symbols(rand.nextInt(Symbol.symbols.size))
  }

  def getRandomOhlcv(nOhlcv: Int): List[OhlcvBase] = {
    var ohlcPrev: Option[Ohlcv] = None
    val dailyVol: Double = uniformRandom(0.20, 0.75) / math.sqrt(252)
    val ohlcvList: ListBuffer[OhlcvBase] = ListBuffer[OhlcvBase]()

    for (_ <- 1 to nOhlcv) {
      val date: LocalDate =
        ohlcPrev match {
          case None => LocalDate.now().plusDays(rand.nextInt(5 * 365) * rand.nextGaussian().signum)
          case Some(ohlcv: Ohlcv) => ohlcv.date.plusDays(1)
        }

      val symbol: Symbol =
        ohlcPrev match {
          case None => getRandomSymbol
          case Some(ohlcv: Ohlcv) => ohlcv.symbol
        }

      val open: Double =
        ohlcPrev match {
          case None => 100 * uniformRandom(0.1, 5)
          case Some(ohlc: Ohlcv) => ohlc.close.value * uniformRandom(0.99, 1.01)
        }

      val close: Double = open * (1 + dailyVol * rand.nextGaussian())

      // 25bps above/below max(close, open); why below? so that we have some instances of high=close or high=open
      val high: Double = math.max(math.max(close, open), math.max(close, open) * uniformRandom(0.9975, 1.0025))

      // 25bps above/below min(close, open); why above? so that we have some instances of low=close or low=open
      val low: Double = math.min(math.min(close, open), math.min(close, open) * uniformRandom(0.9975, 1.0025))

      val volume: Double = 10e6 * uniformRandom(0.5, 5) / close

      val ohlcv = Ohlcv(
        date,
        symbol,
        PosDouble.from(Utils.round(open, 2)).get,
        PosDouble.from(Utils.round(high, 2)).get,
        PosDouble.from(Utils.round(low, 2)).get,
        PosDouble.from(Utils.round(close, 2)).get,
        Some(PosZDouble.from(Utils.round(volume, 1)).get)
      )

      ohlcvList += ohlcv

      ohlcPrev = Some(ohlcv)
    }

    ohlcvList.toList
  }
}
