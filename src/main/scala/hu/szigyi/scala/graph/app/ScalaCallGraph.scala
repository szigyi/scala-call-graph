package hu.szigyi.scala.graph.app

import cats.effect.{ExitCode, IO, IOApp}
import hu.szigyi.scala.graph.Model._
import hu.szigyi.scala.graph.visitor.ClassVisitor
import org.apache.bcel.classfile.ClassParser

import java.io.File
import java.util.jar.JarFile

object ScalaCallGraph extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    (for {
      jarPath        <- IO.pure(args.head)
      packagePattern  = Option.when(args.size > 1)(args(1))
      result         <- callGraph(jarPath, packagePattern)
    } yield result).map(_ => ExitCode.Success)

  def callGraph(jarPath: String, packagePattern: Option[String]): IO[Set[ClassLevel]] =
    for {
      jarFile      <- toJarFile(jarPath)
      classParsers  = toClassParsers(jarPath, jarFile)
      methodCalls   = classParsers.flatMap(cp => toMethodCalls(cp))
      classLevels   = toClassLevelCalls(methodCalls)
      filtered      = filterByPackage(packagePattern, classLevels)
    } yield filtered

  def toJarFile(jarPath: String): IO[JarFile] = {
    val file = new File(jarPath)
    if (file.exists()) IO.pure(new JarFile(file))
    else IO.raiseError(Error(s"Jar file $jarPath does not exist"))
  }

  def toClassParsers(jarPath: String, jar: JarFile): Seq[ClassParser] = {
    import scala.jdk.CollectionConverters._
    jar.entries().asScala.toSeq.flatMap { entry =>
      if (entry.isDirectory || !entry.getName.endsWith(".class")) None
      else Some(new ClassParser(jarPath, entry.getName))
    }
  }

  def toMethodCalls(classParser: ClassParser): Seq[Invokation] =
    new ClassVisitor(classParser.parse()).methodCalls

  def toClassLevelCalls(methodCalls: Seq[Invokation]): Set[ClassLevel] = {

    val calledBy = methodCalls.groupBy(_.called.className)
    methodCalls.map { call =>
      val callers = calledBy(call.called.className)
      val invokes = callers.toSet.map {
        case VirtualInvokation(caller, _)   => Virtual(caller.className, callers.collect { case c: VirtualInvokation if c.caller == caller => c }.size)
        case InterfaceInvokation(caller, _) => InterferenceRef(caller.className, callers.collect { case c: InterfaceInvokation if c.caller == caller => c }.size)
        case SpecialInvokation(caller, _)   => Special(caller.className, callers.collect { case c: SpecialInvokation if c.caller == caller => c }.size)
        case StaticInvokation(caller, _)    => Static(caller.className, callers.collect { case c: StaticInvokation if c.caller == caller => c }.size)
        case DynamicInvokation(caller, _)   => Dynamic(caller.className, callers.collect { case c: DynamicInvokation if c.caller == caller => c }.size)
      }
      ClassLevel(call.called.className, invokes)
    }.toSet
  }

  def filterByPackage(rawPackagePattern: Option[String], classLevels: Set[ClassLevel]): Set[ClassLevel] = {
    val packagePattern = rawPackagePattern match {
      case Some(value) => value.replace(".", "\\.") + ".*"
      case None        => ".*"
    }
    classLevels.flatMap { cl =>
      if (cl.referencedClass.matches(packagePattern)) {
        val references = cl.references.filter(_.className.matches(packagePattern))
        if (references.nonEmpty) Some(cl.copy(references = references))
        else None
      } else {
        None
      }
    }
  }
}
