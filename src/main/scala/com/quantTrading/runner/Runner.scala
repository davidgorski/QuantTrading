package com.quantTrading.runner

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.quantTrading.config.Config

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object Runner {

  def run(config: Config): Unit = {



  }



  private def getGraph(config: Config) = {



    val heartbeat =
      Source
        .tick(FiniteDuration(0L, TimeUnit.SECONDS), FiniteDuration(1L, TimeUnit.MINUTES), ())
        .map(_ => Instant.now(config.clock))
        .mapMaterializedValue(_ => NotUsed)

    val intradayOhlcv




  }
}
