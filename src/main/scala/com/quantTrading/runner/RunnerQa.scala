package com.quantTrading.runner

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext

object RunnerQa {

  def main(args: Array[String]): Unit = {
    implicit val actorSystem = ActorSystem("quant-trading-actor-system")
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher


  }
}
