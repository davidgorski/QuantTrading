package com.quantTrading.symbols

import scalaz.{Failure, Success, Validation}

sealed abstract class Symbol(
  val symbol: String,
  val alphavantage: String
)

object Symbol {

  case object SPY extends Symbol("SPY", "SPY")
  case object QQQ extends Symbol("QQQ", "QQQ")
  case object IWM extends Symbol("IWM", "IWM")
  case object EZU extends Symbol("EZU", "EZU")
  case object EWJ extends Symbol("EWJ", "EWJ")
  case object IEF extends Symbol("IEF", "IEF")
  case object EEM extends Symbol("EEM", "EEM")
  case object TLT extends Symbol("TLT", "TLT")
  case object GLD extends Symbol("GLD", "GLD")
  case object BIL extends Symbol("BIL", "BIL")
  case object HYG extends Symbol("HYG", "HYG")
  case object LQD extends Symbol("LQD", "LQD")

  val symbols: List[Symbol] = List[Symbol](
    SPY,
    QQQ,
    IWM,
    EZU,
    EWJ,
    IEF,
    EEM,
    TLT,
    GLD,
    BIL,
    HYG,
    BIL,
    LQD
  )

  def apply(symbolString: String): Symbol = {
    for (symbol <- symbols) {
      if (symbol.symbol.equalsIgnoreCase(symbol.symbol))
        return symbol
    }
    throw new RuntimeException(s"Could not construct Symbol from $symbolString")
  }
}



