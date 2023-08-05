package com.quantTrading.infra

import com.quantTrading.scalaUtils.FiniteQueue
import org.scalatest.funsuite.AnyFunSuite

class FiniteQueueTest extends AnyFunSuite {

  test("Finite queue test enqueues and dequeues") {
    val q1 = FiniteQueue[Int]()

    q1.enqueueFinite(1, 3)
    q1.enqueueFinite(2, 3)
    q1.enqueueFinite(3, 3)
    q1.enqueueFinite(4, 3)

    assert(q1.toList == List[Int](2, 3, 4))
  }

  test("Finite queue can access elements appropriately") {
    val q1 = FiniteQueue[Int]()

    q1.enqueueFinite(1, 3)
    q1.enqueueFinite(2, 3)
    q1.enqueueFinite(3, 3)
    q1.enqueueFinite(4, 3)

    assert(q1.get(0) == 2)
    assert(q1.get(q1.size - 1) == 4)
  }
}