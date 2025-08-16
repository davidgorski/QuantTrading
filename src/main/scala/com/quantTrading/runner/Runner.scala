package com.quantTrading.runner

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import com.quantTrading.akka.FanOutFanInFlow3
import com.quantTrading.alphaVantage.daily.{Daily, DailyOhlcv}
import com.quantTrading.config.Config
import com.quantTrading.scala.ChoiceType3
import scalaz.Validation

import java.time.{Instant, Duration}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}


object Runner {

  def run(
    config: Config
  )(
    implicit
    actorSystem: ActorSystem,
    executionContext: ExecutionContext,
  ): Unit = {

    val graph = getGraph(config)
    graph.run()

    Thread.sleep(FiniteDuration(10L, TimeUnit.MINUTES).toMillis)
  }


  private def getGraph(
    config: Config
  )(
    implicit
    actorSystem: ActorSystem,
    executionContext: ExecutionContext,
  ): RunnableGraph[NotUsed] = {

    val heartbeat: Source[Instant, NotUsed] =
      Source
        .tick(FiniteDuration(0L, TimeUnit.SECONDS), FiniteDuration(1L, TimeUnit.MINUTES), ())
        .map(_ => Instant.now(config.clock))
        .mapMaterializedValue(_ => NotUsed)

    val heartbeatPassthrough: Flow[Instant, Instant, NotUsed] = Flow[Instant].map(x => x)
    val dailyData: Flow[Instant, Validation[String, List[DailyOhlcv]], NotUsed] = Daily.getFlow(config)
    val intradayData: Flow[Instant, Instant, NotUsed] = Flow[Instant].map(x => x)

    val mktDataFlow: Flow[Instant, List[ChoiceType3[Instant, Validation[String, List[DailyOhlcv]], Instant]], NotUsed] =
      FanOutFanInFlow3.getFlow[Instant, Instant, Validation[String, List[DailyOhlcv]], Instant](
        heartbeatPassthrough,
        dailyData,
        intradayData
      )

    val graph: RunnableGraph[NotUsed] =
      heartbeat.via(mktDataFlow).toMat(Sink.foreach(println))(Keep.none)

    graph
  }
}
