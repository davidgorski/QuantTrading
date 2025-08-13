package com.quantTrading.alphaVantage.daily

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink}
import com.quantTrading.symbols.Symbol
import org.scalactic.anyvals.PosZInt
import java.time.{LocalDateTime, ZoneId}
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration


object ExampleDaily {

  def main(args: Array[String]): Unit = {

    implicit val actorSystem: ActorSystem = ActorSystem("daily-query-example")
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher

    val symbols = Symbol.symbols
    val nRetries = PosZInt(3)

    val zoneId = ZoneId.of("America/New_York")
    val graph: RunnableGraph[NotUsed] =
      Daily.getSource(symbols, nRetries)
        .toMat(Sink.foreach(x => println(s"$x ${LocalDateTime.now(zoneId)}")))(Keep.none)

    graph.run()

    Thread.sleep(FiniteDuration(10L, TimeUnit.MINUTES).toMillis)
  }
}
