package com.quantTrading

object Utils {

  def round(num: Double, nDecimals: Int): Double = {
    val s: Double = math.pow(10, nDecimals)
    math.floor(s * num) / s
  }

}
