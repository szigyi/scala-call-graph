package hu.szigyi.scala.graph.app

import cats.effect.{ExitCode, IO, IOApp}
import hu.szigyi.scala.graph.app.ScalaCallGraph.callGraph

object Application extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    (for {
      jarPath        <- IO.pure(args.head)
      packagePattern  = Option.when(args.size > 1)(args(1))
      result         <- callGraph(jarPath, packagePattern)
      _               = println(result)
    } yield result).map(_ => ExitCode.Success)
}
