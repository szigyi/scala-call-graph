package hu.szigyi.scala.graph.app

import cats.effect.{ExitCode, IO, IOApp}
import hu.szigyi.scala.graph.app.ScalaCallGraph
import hu.szigyi.scala.graph.service.Service

object Application extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    (for {
      jarPath        <- IO.pure(args.head)
      packagePattern  = Option.when(args.size > 1)(args(1))
      _              <- app(jarPath, packagePattern)
    } yield ()).map(_ => ExitCode.Success)

  private def app(jarPath: String, packagePattern: Option[String]) = {
    for {
      cg <- IO.pure(new ScalaCallGraph(new Service))
      _  <- cg.callGraph(jarPath, packagePattern)
    } yield ()
  }
}
