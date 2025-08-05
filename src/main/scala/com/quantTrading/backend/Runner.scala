package com.quantTrading.backend

object Runner {

  @throws[IOException]
  def main(args: Array[String]): Unit = {
    new MainIB().connect()
    System.in.read //press enter to exit

    System.exit(0)
  }
}
