package hu.szigyi.scala.graph.service

import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import hu.szigyi.scala.graph.Model.*
import hu.szigyi.scala.graph.visitor.ClassVisitor
import org.apache.bcel.classfile.ClassParser

import scala.jdk.CollectionConverters.*
import java.io.File
import java.time.Instant
import java.util.jar.JarFile

object Service extends StrictLogging {

  def toJarFile(jarPath: String): IO[JarFile] = {
    val file = new File(jarPath)
    if (file.exists()) IO.pure(new JarFile(file))
    else IO.raiseError(Error(s"Jar file $jarPath does not exist"))
  }

  def toClassParsers(jarPath: String, jar: JarFile): Seq[ClassParser] =
    jar.entries().asScala.toSeq.flatMap { entry =>
      if (entry.isDirectory || !entry.getName.endsWith(".class")) None
      else Some(new ClassParser(jarPath, entry.getName))
    }

  def toMethodCalls(classParsers: Seq[ClassParser]): Seq[Invokation] =
    classParsers.flatMap { classParser =>
      new ClassVisitor(classParser.parse()).methodCalls
    }

  def toClassLevelCalls(methodCalls: Seq[Invokation]): Set[ClassLevel] = {
    val calledBy = methodCalls.groupBy(_.called.className)
    methodCalls.zipWithIndex.map { case (call, index) =>
      val callers: Seq[Invokation] = calledBy(call.called.className)
      val callersByClass: Map[String, Seq[Invokation]] = callers.groupBy(_.getClass.getName) // to speed up the look ups
      val virtualCallers = callersByClass.getOrElse("VirtualInvokation", Seq.empty)
      val interfaceCallers = callersByClass.getOrElse("InterfaceInvokation", Seq.empty)
      val specialCallers = callersByClass.getOrElse("SpecialInvokation", Seq.empty)
      val staticCallers = callersByClass.getOrElse("StaticInvokation", Seq.empty)
      val dynamicCallers = callersByClass.getOrElse("DynamicInvokation", Seq.empty)

      val invokes = callers.toSet.map {
        case VirtualInvokation(caller, _)   => Virtual(caller.className, virtualCallers.collect { case c if c.caller == caller => c }.size)
        case InterfaceInvokation(caller, _) => InterferenceRef(caller.className, interfaceCallers.collect { case c if c.caller == caller => c }.size)
        case SpecialInvokation(caller, _)   => Special(caller.className, specialCallers.collect { case c if c.caller == caller => c }.size)
        case StaticInvokation(caller, _)    => Static(caller.className, staticCallers.collect { case c if c.caller == caller => c }.size)
        case DynamicInvokation(caller, _)   => Dynamic(caller.className, dynamicCallers.collect { case c if c.caller == caller => c }.size)
      }
      if (index % 10000 == 0) logger.debug(s"$index:${call.called.className}")
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
