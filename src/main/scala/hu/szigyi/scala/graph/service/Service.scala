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

class Service extends StrictLogging {

  def toJarFile(jarFile: File): IO[JarFile] = {
    if (jarFile.exists()) IO.pure(new JarFile(jarFile))
    else IO.raiseError(Error(s"Jar file ${jarFile.getPath} does not exist"))
  }

  def toClassParsers(jarFile: File, jar: JarFile): Seq[ClassParser] =
    jar.entries().asScala.toSeq.flatMap { entry =>
      if (entry.isDirectory || !entry.getName.endsWith(".class")) None
      else Some(new ClassParser(jarFile.getPath, entry.getName))
    }

  def toMethodCalls(classParsers: Seq[ClassParser]): Seq[Invokation] =
    classParsers.flatMap { classParser =>
      new ClassVisitor(classParser.parse()).methodCalls
    }

  def toClassLevelCalls(methodCalls: Seq[Invokation]): Set[ClassLevel] = {
    val calledBy = methodCalls.groupBy(_.called.className)
    methodCalls.zipWithIndex.map { case (call, index) =>
      val callers: Seq[Invokation] = calledBy(call.called.className)
      val callersByClass   = callers.groupBy(_.getClass.getSimpleName) // to speed up the look ups
      val virtualCallers   = callersByClass.getOrElse("VirtualInvokation", Seq.empty)
      val interfaceCallers = callersByClass.getOrElse("InterfaceInvokation", Seq.empty)
      val specialCallers   = callersByClass.getOrElse("SpecialInvokation", Seq.empty)
      val staticCallers    = callersByClass.getOrElse("StaticInvokation", Seq.empty)
      val dynamicCallers   = callersByClass.getOrElse("DynamicInvokation", Seq.empty)

      val invokes = callers.toSet.map {
        case VirtualInvokation(caller, _)   => Virtual(caller.className, virtualCallers.count(_.caller == caller))
        case InterfaceInvokation(caller, _) => InterfaceRef(caller.className, interfaceCallers.count(_.caller == caller))
        case SpecialInvokation(caller, _)   => Special(caller.className, specialCallers.count(_.caller == caller))
        case StaticInvokation(caller, _)    => Static(caller.className, staticCallers.count(_.caller == caller))
        case DynamicInvokation(caller, _)   => Dynamic(caller.className, dynamicCallers.count(_.caller == caller))
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

  def toOutputInvokation(classLevels: Set[ClassLevel], separateRefTypes: Boolean): Seq[OutputInvokation] =
    if (separateRefTypes) {
      classLevels.flatMap { classLevel =>
        classLevel.references.map {
          case Virtual(className, count)         => OutputInvokation(classLevel.referencedClass, className, count, "virtual")
          case InterfaceRef(className, count)    => OutputInvokation(classLevel.referencedClass, className, count, "interface")
          case Special(className, count)         => OutputInvokation(classLevel.referencedClass, className, count, "special")
          case Static(className, count)          => OutputInvokation(classLevel.referencedClass, className, count, "static")
          case Dynamic(className, count)         => OutputInvokation(classLevel.referencedClass, className, count, "dynamic")
        }
      }.toList
    } else {
      classLevels.flatMap { classLevel =>
        classLevel.references.groupBy(_.className).map {
          case (referenceClassName, references) => OutputInvokation(classLevel.referencedClass, referenceClassName, references.map(_.count).sum, "")
        }
      }.toList
    }
}
