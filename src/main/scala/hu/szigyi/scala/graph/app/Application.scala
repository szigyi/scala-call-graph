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
      jarFile        <- IO.pure(new File(args.head))
      csvOutputDir    = new File(args(1))
      packagePattern  = Option.when(args.size > 2)(args(2))
      _              <- app(jarFile, packagePattern, csvOutputDir)
    } yield ()).map(_ => ExitCode.Success)

  private def app(jarFile: File, packagePattern: Option[String], csvOutputDir: File) = {
    for {
      module <- IO.pure(new Module)
      graph  <- module.scalaCallGraph.callGraph(jarFile, packagePattern)
      csv     = module.csvOutput.toCsv(graph)
      _       = module.io.writeFile(csvOutputDir, s"scala_callgraph_${jarFile.getName}_${Instant.now()}.csv", csv)
    } yield ()
  }
}
