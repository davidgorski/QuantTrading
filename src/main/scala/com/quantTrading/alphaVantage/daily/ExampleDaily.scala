package com.quantTrading.alphaVantage.daily

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import com.quantTrading.config.ConfigQa
import com.quantTrading.symbols.QtSymbol
import org.scalactic.anyvals.PosZInt
import scalaz.Validation

import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration


object ExampleDaily {

  def main(args: Array[String]): Unit = {

    implicit val actorSystem: ActorSystem = ActorSystem("daily-query-example")
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher
    
    val nRetries = PosZInt(3)
    val config = ConfigQa()

    val tick: Source[Instant, NotUsed] =
      Source
        .tick(FiniteDuration(0L, TimeUnit.SECONDS), FiniteDuration(1L, TimeUnit.MINUTES), ())
        .map(_ => Instant.now())
        .mapMaterializedValue(_ => NotUsed)

    val queryFlow: Flow[Instant, Validation[String, List[DailyOhlcv]], NotUsed] =
      Daily.getFlow(config)

    val graph: RunnableGraph[NotUsed] =
      tick
        .via(queryFlow)
        .toMat(Sink.foreach(x => println(s"$x ${LocalDateTime.now(config.zoneId)}")))(Keep.none)

    graph.run()

    Thread.sleep(FiniteDuration(10L, TimeUnit.MINUTES).toMillis)
  }
}
