package com.quantTrading.infra

import com.quantTrading.signals.{Rsi, RsiParams}
import org.scalactic.anyvals.PosDouble
import org.scalatest.funsuite.AnyFunSuite

class RsiTest extends AnyFunSuite {

  test("Rsi works as expected") {
    TestUtils.resetSeed

    for (_ <- 1 to 10) {
      val ohlcvBars: List[OhlcvBase] = TestUtils.getRandomOhlcv(100)

      val lookback = PosDouble.from(TestUtils.uniformRandom(1, 23)).get
      val lambda = 2.0 / (lookback.value + 1.0)

      var rsi = Rsi(RsiParams(lookback))
      var emaUpMaybe: Option[Double] = None
      var emaDnMaybe: Option[Double] = None
      assert(rsi.valueMaybe === None)

      var rsiExpected: Option[Double] = None
      var ohlcvBarPrev: Option[OhlcvBase] = None
      for (ohlcvBar <- ohlcvBars) {
        if (ohlcvBarPrev.isDefined) {
          val r1 = ohlcvBar.close.value / ohlcvBarPrev.get.close.value - 1
          if (emaUpMaybe.isEmpty)
            emaUpMaybe = Some(math.max(r1, 0.0))
          if (emaDnMaybe.isEmpty)
            emaDnMaybe = Some(math.abs(math.min(r1, 0.0)))
          emaUpMaybe = Some(lambda * math.max(r1, 0.0) + (1 - lambda) * emaUpMaybe.get)
          emaDnMaybe = Some(lambda * math.abs(math.min(r1, 0.0)) + (1 - lambda) * emaDnMaybe.get)
          if (emaDnMaybe.get == 0.0 || emaUpMaybe.get == 0.0) {
            rsiExpected = None
          } else {
            rsiExpected = Some(1.0 - 1.0 / (1.0 + emaUpMaybe.get / emaDnMaybe.get))
          }

          rsi = rsi.onData(r1)
          assert(rsi.valueMaybe.isEmpty == rsiExpected.isEmpty)
          assert(rsi.valueMaybe.isEmpty || math.abs(rsi.valueMaybe.get - rsiExpected.get) < 0.0001)
        }

        ohlcvBarPrev = Some(ohlcvBar)
      }
    }
  }
}
