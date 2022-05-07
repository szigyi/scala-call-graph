package hu.szigyi.scala.graph.app

import hu.szigyi.scala.graph.ScalaIO
import hu.szigyi.scala.graph.output.{CsvOutput, JsonOutput}
import hu.szigyi.scala.graph.service.Service

class Module {
  private val service = new Service
  val scalaCallGraph  = new ScalaCallGraph(service)
  val csvOutput       = new CsvOutput(service)
  val jsonOutput      = new JsonOutput(service)
  val io              = new ScalaIO
}
