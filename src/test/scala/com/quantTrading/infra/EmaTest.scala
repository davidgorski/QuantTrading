package com.quantTrading.infra

import com.quantTrading.signals.{Ema, EmaParams}
import org.scalactic.anyvals.PosDouble
import org.scalatest.funsuite.AnyFunSuite


class EmaTest extends AnyFunSuite {

  test("Ema works as expected") {
    TestUtils.resetSeed

    for (_ <- 1 to 10) {
      val ohlcvBars = TestUtils.getRandomOhlcv(100)
      val lookback = PosDouble.from(TestUtils.uniformRandom(1, 23)).get
      val lambda = 2.0 / (lookback.value + 1.0)

      var ema = Ema(EmaParams(lookback))
      assert(ema.valueMaybe === None)

      var emaExpected: Option[Double] = None
      for (ohlcvBar <- ohlcvBars) {
        ema = ema.onData(ohlcvBar.close.value)

        emaExpected = emaExpected match {
          case None => Some(ohlcvBar.close.value)
          case Some(d: Double) => Some(lambda * ohlcvBar.close.value + (1 - lambda) * d)
        }

        assert(ema.valueMaybe === emaExpected)
      }
    }
  }
}
