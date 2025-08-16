package com.quantTrading.runner

import akka.actor.ActorSystem
import com.quantTrading.config.ConfigQa

import scala.concurrent.ExecutionContext

object RunnerQa {

  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem = ActorSystem("quant-trading-actor-system-qa")
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher

    val config = ConfigQa()
    Runner.run(config)
  }
}
