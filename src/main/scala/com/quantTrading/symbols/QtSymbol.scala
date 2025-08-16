package com.quantTrading.symbols

import java.time.LocalDate


sealed abstract class QtSymbol(
  val symbol: String,
  val alphaVantage: String,
  val ibkr: String,
  val isTradable: Boolean,
  val startDate: LocalDate,
)

object QtSymbol {

  case object SPY extends QtSymbol("SPY", "SPY", "SPY", true, LocalDate.of(2000, 1, 1))
  case object QQQ extends QtSymbol("QQQ", "QQQ", "QQQ", true, LocalDate.of(2000, 1, 1))
  case object TLT extends QtSymbol("TLT", "TLT", "TLT", true, LocalDate.of(2003, 1, 1))
  case object IEF extends QtSymbol("IEF", "IEF", "IEF", true, LocalDate.of(2003, 1, 1))
  case object GLD extends QtSymbol("GLD", "GLD", "GLD", true, LocalDate.of(2005, 1, 1))
  case object SLV extends QtSymbol("SLV", "SLV", "SLV", true, LocalDate.of(2007, 1, 1))
  case object BIL extends QtSymbol("BIL", "BIL", "BIL", true, LocalDate.of(2007, 7, 1))
  case object HYG extends QtSymbol("HYG", "HYG", "HYG", true, LocalDate.of(2007, 7, 1))
  case object JNK extends QtSymbol("JNK", "JNK", "JNK", true, LocalDate.of(2008, 1, 1))
  case object FTSL extends QtSymbol("FTSL", "FTSL", "FTSL", true, LocalDate.of(2013, 5, 3))
  case object HYD extends QtSymbol("HYD", "HYD", "HYD", true, LocalDate.of(2009, 2, 6))
  case object USHY extends QtSymbol("USHY", "USHY", "USHY", true, LocalDate.of(2017, 10, 27))
  case object LQD extends QtSymbol("LQD", "LQD", "LQD", true, LocalDate.of(2002, 8, 9))
  case object IWM extends QtSymbol("IWM", "IWM", "IWM", true, LocalDate.of(2000, 6, 2))
  case object EZU extends QtSymbol("EZU", "EZU", "EZU", true, LocalDate.of(2000, 8, 4))
  case object EWJ extends QtSymbol("EWJ", "EWJ", "EWJ", true, LocalDate.of(2000, 8, 4))
  case object EEM extends QtSymbol("EEM", "EEM", "EEM", true, LocalDate.of(2003, 5, 2))
  case object GSG extends QtSymbol("GSG", "GSG", "GSG", true, LocalDate.of(2006, 8, 4))

  val symbols: Set[QtSymbol] = Set[QtSymbol](
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

  def apply(symbolString: String): QtSymbol = {
    for (symbol <- symbols) {
      if (symbol.symbol.equalsIgnoreCase(symbol.symbol))
        return symbol
    }
    throw new RuntimeException(s"Could not construct Symbol from $symbolString")
  }
}
