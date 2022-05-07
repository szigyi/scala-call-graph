package hu.szigyi.scala.graph.app

import cats.effect.{ExitCode, IO, IOApp}
import hu.szigyi.scala.graph.ScalaIO
import hu.szigyi.scala.graph.app.ScalaCallGraph
import hu.szigyi.scala.graph.service.Service

import java.io.File
import java.time.Instant

object Application extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    (for {
      jarFile          <- IO.pure(new File(args.head))
      outputDir         = new File(args(1))
      separateRefTypes  = if (args.size > 2 && args(2) == "separateRefTypes") true else false
      packagePattern    = Option.when(args.size > 3)(args(3))
      _                <- app(jarFile, packagePattern, separateRefTypes, outputDir)
    } yield ()).map(_ => ExitCode.Success)

  private def app(jarFile: File, packagePattern: Option[String], separateRefTypes: Boolean, outputDir: File) = {
    for {
      module <- IO.pure(new Module)
      graph  <- module.scalaCallGraph.callGraph(jarFile, packagePattern)
      csv     = module.csvOutput.toCsv(graph, separateRefTypes)
      json    = module.jsonOutput.toJson(graph, separateRefTypes)
      _       = module.io.writeFile(outputDir, s"scala_callgraph_${jarFile.getName}_${Instant.now()}.csv", csv)
      _       = module.io.writeFile(outputDir, s"scala_callgraph_${jarFile.getName}_${Instant.now()}.json", json)
    } yield ()
  }
}
