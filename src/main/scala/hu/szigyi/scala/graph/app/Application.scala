package hu.szigyi.scala.graph.app

import cats.effect.{ExitCode, IO, IOApp}
import hu.szigyi.scala.graph.ScalaIO
import hu.szigyi.scala.graph.app.ScalaCallGraph
import hu.szigyi.scala.graph.service.Service

import java.io.File

object Application extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    (for {
      jarPath        <- IO.pure(args.head)
      packagePattern  = Option.when(args.size > 1)(args(1))
      _              <- app(jarPath, packagePattern)
    } yield ()).map(_ => ExitCode.Success)

  private def app(jarPath: String, packagePattern: Option[String]) = {
    for {
      module <- IO.pure(new Module)
      graph  <- module.scalaCallGraph.callGraph(jarPath, packagePattern)
      csv     = module.csvOutput.toCsv(graph)
      _       = ScalaIO.writeFile(new File("."), "scala_callgraph.csv", csv)
    } yield ()
  }
}
