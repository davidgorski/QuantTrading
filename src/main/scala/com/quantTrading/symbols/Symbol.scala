package com.quantTrading.symbols

import scalaz.{Failure, Success, Validation}

sealed abstract class Symbol(
  val symbol: String
)

object Symbol {

  case object SPY extends Symbol("SPY")
  case object QQQ extends Symbol("QQQ")
  case object IWM extends Symbol("IWM")
  case object EZU extends Symbol("EZU")
  case object EWJ extends Symbol("EWJ")
  case object IEF extends Symbol("IEF")
  case object EEM extends Symbol("EEM")
  case object TLT extends Symbol("TLT")
  case object GLD extends Symbol("GLD")
  case object BIL extends Symbol("BIL")
  case object HYG extends Symbol("HYG")
  case object LQD extends Symbol("LQD")

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



