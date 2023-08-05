package com.quantTrading.scalaUtils

import scala.collection.mutable


case class FiniteQueue[A](
  private val q: mutable.Queue[A] = mutable.Queue[A]()
) {

  def enqueueFinite(elem: A, maxSize: Int): FiniteQueue[A] = {
    q.enqueue(elem)
    while (q.size > maxSize) {
      q.dequeue()
    }
    FiniteQueue[A](q)
  }

  def size: Int = q.size

  def get(i: Int): A = q(i)

  def toList: List[A] = q.toList
}
