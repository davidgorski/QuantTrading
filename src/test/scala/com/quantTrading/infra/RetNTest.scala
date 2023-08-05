package com.quantTrading.infra

import com.quantTrading.signals.{RetN, RetNParams}
import org.scalactic.anyvals.{PosDouble, PosLong}
import org.scalatest.funsuite.AnyFunSuite

class RetNTest extends AnyFunSuite {

  test("RetN works as expected") {
    TestUtils.resetSeed

    for (_ <- 1 to 10) {
      val ohlcvBars: List[OhlcvBase] = TestUtils.getRandomOhlcv(100)

      val lookback: PosLong = PosLong.from(1 + TestUtils.rand.nextInt(10)).get
      var retN = RetN(RetNParams(lookback))
      assert(retN.valueMaybe === None)

      for (i <- ohlcvBars.indices) {
        val ohlcvBar: OhlcvBase = ohlcvBars(i)

        retN = retN.onData(ohlcvBar)

        if (i < lookback.value)
          assert(retN.valueMaybe === None)
        else {
          val retNExpected: Double = ohlcvBars(i).close / ohlcvBars(i - lookback.value.toInt).close - 1
          assert(math.abs(retN.valueMaybe.get - retNExpected) < 0.0001)
        }
      }
    }
  }
}