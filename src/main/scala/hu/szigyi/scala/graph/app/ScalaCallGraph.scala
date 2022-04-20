package hu.szigyi.scala.graph.app

import cats.effect.{ExitCode, IO, IOApp}
import com.typesafe.scalalogging.StrictLogging
import hu.szigyi.scala.graph.Model.*
import hu.szigyi.scala.graph.service.Service
import hu.szigyi.scala.graph.visitor.ClassVisitor
import org.apache.bcel.classfile.ClassParser

import java.io.File
import java.util.jar.JarFile

class ScalaCallGraph(service: Service) extends StrictLogging {
  import service.*

  def callGraph(file: File, packagePattern: Option[String]): IO[Set[ClassLevel]] =
    for {
      jarFile      <- toJarFile(file)
      classParsers  = toClassParsers(file, jarFile)
      _             = logger.info(s"ClassParsers: ${classParsers.size}")
      methodCalls   = toMethodCalls(classParsers)
      _             = logger.info(s"MethodCalls: ${methodCalls.size}")
      classLevels   = toClassLevelCalls(methodCalls)
      _             = logger.info(s"ClassLevels: ${classLevels.size}")
      filtered      = filterByPackage(packagePattern, classLevels)
      _             = logger.info(s"Filtered: ${filtered.size}")
    } yield filtered

}
