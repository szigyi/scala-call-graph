package hu.szigyi.scala.graph.app

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.traverse.toTraverseOps
import hu.szigyi.scala.graph.Model._
import hu.szigyi.scala.graph.visitor.ClassVisitor
import org.apache.bcel.classfile.ClassParser

import java.io.File
import java.util.jar.JarFile

object ScalaCallGraph extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    runApp(args).map(_ => ExitCode.Success)

  def runApp(args: List[String]): IO[Set[ClassLevel]] =
    for {
      jars        <- args.traverse(toJarFile)
      classParsers = jars.flatMap { case (jarPath, jarFile) => toClassParsers(jarPath, jarFile) }
      methodCalls  = classParsers.flatMap(cp => toMethodCalls(cp))
      classLevels  = toClassLevelCalls(methodCalls)
      _            = classLevels.map(println)
    } yield classLevels

  def toJarFile(jarPath: String): IO[(String, JarFile)] = {
    val file = new File(jarPath)
    if (file.exists()) IO.pure((jarPath, new JarFile(file)))
    else IO.raiseError(Error(s"Jar file $jarPath does not exist"))
  }

  def toClassParsers(jarPath: String, jar: JarFile): Seq[ClassParser] = {
    import scala.jdk.CollectionConverters.*
    jar.entries().asScala.toSeq.flatMap { entry =>
      if (entry.isDirectory || !entry.getName.endsWith(".class")) None
      else Some(new ClassParser(jarPath, entry.getName))
    }
  }

  def toMethodCalls(classParser: ClassParser): Seq[Invokation] =
    new ClassVisitor(classParser.parse()).methodCalls

  def toClassLevelCalls(methodCalls: Seq[Invokation]): Set[ClassLevel] = {
    def findCallers[T <: Invokation](caller: ClassMethod, cs: Seq[Invokation]): Seq[Invokation] =
      cs.collect { case c if c.caller == caller && c.isInstanceOf[T] => c }

    val calledBy = methodCalls.groupBy(_.called.className)
    methodCalls.map { call =>
      val callers = calledBy(call.called.className)
      val invokes = callers.toSet.map {
        case VirtualInvokation(caller, _)   => Virtual(caller.className, findCallers[VirtualInvokation](caller, callers).size)
        case InterfaceInvokation(caller, _) => InterferenceRef(caller.className, findCallers[InterfaceInvokation](caller, callers).size)
        case SpecialInvokation(caller, _)   => Special(caller.className, findCallers[SpecialInvokation](caller, callers).size)
        case StaticInvokation(caller, _)    => Static(caller.className, findCallers[StaticInvokation](caller, callers).size)
        case DynamicInvokation(caller, _)   => Dynamic(caller.className, findCallers[DynamicInvokation](caller, callers).size)
      }
      ClassLevel(call.called.className, invokes)
    }.toSet
  }
}
