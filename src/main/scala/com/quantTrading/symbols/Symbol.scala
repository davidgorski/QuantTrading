package com.quantTrading.symbols

import java.time.LocalDate


sealed abstract class Symbol(
  val symbol: String,
  val alphaVantage: String,
  val ibkr: String,
  val isTradable: Boolean,
  val startDate: LocalDate,
)

object Symbol {

  case object SPY extends Symbol("SPY", "SPY", "SPY", true, LocalDate.of(2000, 1, 1))
  case object QQQ extends Symbol("QQQ", "QQQ", "QQQ", true, LocalDate.of(2000, 1, 1))
  case object TLT extends Symbol("TLT", "TLT", "TLT", true, LocalDate.of(2003, 1, 1))
  case object IEF extends Symbol("IEF", "IEF", "IEF", true, LocalDate.of(2003, 1, 1))
  case object GLD extends Symbol("GLD", "GLD", "GLD", true, LocalDate.of(2005, 1, 1))
  case object SLV extends Symbol("SLV", "SLV", "SLV", true, LocalDate.of(2007, 1, 1))
  case object BIL extends Symbol("BIL", "BIL", "BIL", true, LocalDate.of(2007, 7, 1))
  case object HYG extends Symbol("HYG", "HYG", "HYG", true, LocalDate.of(2007, 7, 1))
  case object JNK extends Symbol("JNK", "JNK", "JNK", true, LocalDate.of(2008, 1, 1))
  case object FTSL extends Symbol("FTSL", "FTSL", "FTSL", true, LocalDate.of(2013, 5, 3))
  case object HYD extends Symbol("HYD", "HYD", "HYD", true, LocalDate.of(2009, 2, 6))
  case object USHY extends Symbol("USHY", "USHY", "USHY", true, LocalDate.of(2017, 10, 27))
  case object LQD extends Symbol("LQD", "LQD", "LQD", true, LocalDate.of(2002, 8, 9))
  case object IWM extends Symbol("IWM", "IWM", "IWM", true, LocalDate.of(2000, 6, 2))
  case object EZU extends Symbol("EZU", "EZU", "EZU", true, LocalDate.of(2000, 8, 4))
  case object EWJ extends Symbol("EWJ", "EWJ", "EWJ", true, LocalDate.of(2000, 8, 4))
  case object EEM extends Symbol("EEM", "EEM", "EEM", true, LocalDate.of(2003, 5, 2))
  case object GSG extends Symbol("GSG", "GSG", "GSG", true, LocalDate.of(2006, 8, 4))

  val symbols: List[Symbol] = List[Symbol](
    SPY,
    QQQ,
    TLT,
    IEF,
    GLD,
    SLV,
    BIL,
    HYG,
    JNK,
    FTSL,
    HYD,
    USHY,
    LQD,
    IWM,
    EZU,
    EWJ,
    EEM,
    GSG,
  )

  def apply(symbolString: String): Symbol = {
    for (symbol <- symbols) {
      if (symbol.symbol.equalsIgnoreCase(symbol.symbol))
        return symbol
    }
    throw new RuntimeException(s"Could not construct Symbol from $symbolString")
  }
}
