package com.quantTrading.akka

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, ZipN}
import com.quantTrading.scala._


object FanOutFanInFlow3 {

  def getFlow[I, O1, O2, O3](
    f1: Flow[I, O1, NotUsed],
    f2: Flow[I, O2, NotUsed],
    f3: Flow[I, O3, NotUsed],
  ): Flow[I, List[ChoiceType3[O1, O2, O3]], NotUsed] = {

    val nInputsOutputs = 3

    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[I](nInputsOutputs))

        val zipN = builder.add(ZipN[ChoiceType3[O1, O2, O3]](nInputsOutputs))

        broadcast.out(0) ~> f1.async ~> Flow[O1].map(Choice1(_)) ~> zipN.in(0)
        broadcast.out(1) ~> f2.async ~> Flow[O2].map(Choice2(_)) ~> zipN.in(1)
        broadcast.out(2) ~> f3.async ~> Flow[O3].map(Choice3(_)) ~> zipN.in(2)

        val toList = builder.add(Flow[Seq[ChoiceType3[O1, O2, O3]]].map(_.toList))

        zipN ~> toList

        FlowShape(broadcast.in, toList.out)
    })
  }
}
