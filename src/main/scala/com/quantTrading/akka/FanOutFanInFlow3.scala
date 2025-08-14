package com.quantTrading.akka

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, ZipN}
import com.quantTrading.scala._


object FanOutFanInFlow3 {

  def getFlow[I, O1, O2, O3](
    f1: I => O1,
    f2: I => O2,
    f3: I => O3,
  ): Flow[I, List[ChoiceType3[O1, O2, O3]], NotUsed] = {

    val nInputsOutputs = 3

    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[I](nInputsOutputs))

        val f1Shape = builder.add(Flow[I].map(f1))
        val f2Shape = builder.add(Flow[I].map(f2))
        val f3Shape = builder.add(Flow[I].map(f3))

        val zipN = builder.add(ZipN[ChoiceType3[O1, O2, O3]](nInputsOutputs))

        broadcast.out(0) ~> f1Shape ~> Flow[O1].map(Choice1(_)).async ~> zipN.in(0)
        broadcast.out(1) ~> f2Shape ~> Flow[O2].map(Choice2(_)).async ~> zipN.in(1)
        broadcast.out(2) ~> f3Shape ~> Flow[O3].map(Choice3(_)).async ~> zipN.in(3)

        val listify = builder.add(Flow[Seq[ChoiceType3[O1, O2, O3]]].map(_.toList))

        zipN ~> listify

        FlowShape(broadcast.in, listify.out)
    })
  }
}
