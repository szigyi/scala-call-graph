package hu.szigyi.scala.graph.app

import hu.szigyi.scala.graph.ScalaIO
import hu.szigyi.scala.graph.service.{CsvOutput, Service}

class Module {
  val scalaCallGraph = new ScalaCallGraph(new Service)
  val csvOutput      = new CsvOutput
  val io             = new ScalaIO
}
