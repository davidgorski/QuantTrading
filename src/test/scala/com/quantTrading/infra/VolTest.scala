package com.quantTrading.infra

import com.quantTrading.signals.{Rsi, RsiParams, Vol, VolParams}
import org.scalactic.anyvals.PosDouble
import org.scalatest.funsuite.AnyFunSuite

class VolTest extends AnyFunSuite {

  test("Vol works as expected") {
    TestUtils.resetSeed

    for (_ <- 1 to 10) {
      val ohlcvBars: List[OhlcvBase] = TestUtils.getRandomOhlcv(100)

      val lookback: PosDouble = PosDouble.from(TestUtils.uniformRandom(1, 252)).get
      val lambda = 2.0 / (lookback.value + 1.0)

      var vol = Vol(VolParams(lookback))
      assert(vol.valueMaybe === None)
      var ohlcvBarPrev: Option[OhlcvBase] = None
      var variance: Option[Double] = None

      for (ohlcvBar <- ohlcvBars) {
        vol = vol.onData(ohlcvBar)

        if (ohlcvBarPrev.isDefined) {
          val r1 = ohlcvBar.close.value / ohlcvBarPrev.get.close.value - 1
          if (variance.isEmpty)
            variance = Some(r1 * r1)
          variance = Some(lambda * r1 * r1 + (1 - lambda) * variance.get)
          val volExpected = math.sqrt(variance.get)

          assert(math.abs(vol.valueMaybe.get - volExpected) < 0.0001)
        }

        ohlcvBarPrev = Some(ohlcvBar)
      }
    }
  }
}
