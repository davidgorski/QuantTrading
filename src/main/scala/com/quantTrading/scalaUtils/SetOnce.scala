package com.quantTrading.scalaUtils

class SetOnce[A](var toOption: Option[A] = None) {

  def set(a: A): Unit = {
    if (toOption.isEmpty)
      toOption = Some(a)
    else
      throw new RuntimeException("The variable has already ben set")
  }

  def get: A = toOption.get
}
