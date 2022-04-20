package hu.szigyi.scala.graph.service

import com.typesafe.scalalogging.StrictLogging
import hu.szigyi.scala.graph.Model
import hu.szigyi.scala.graph.Model.{ClassLevel, ClassMethod, Reference, VirtualInvokation}
import hu.szigyi.scala.graph.service.CsvOutput.*

import java.net.URL
import scala.deriving.*
import scala.compiletime.summonAll
import java.nio.file.Path

class CsvOutput extends StrictLogging {

  def toCsv(classLevels: Set[ClassLevel]): String = {
    val flattenedInvokations = classLevels.flatMap { classLevel =>
      classLevel.references.map {
        case Model.Virtual(className, count)         => ClassInvokation(classLevel.referencedClass, className, count, "virtual")
        case Model.InterferenceRef(className, count) => ClassInvokation(classLevel.referencedClass, className, count, "interface")
        case Model.Special(className, count)         => ClassInvokation(classLevel.referencedClass, className, count, "special")
        case Model.Static(className, count)          => ClassInvokation(classLevel.referencedClass, className, count, "static")
        case Model.Dynamic(className, count)         => ClassInvokation(classLevel.referencedClass, className, count, "dynamic")
      }
    }.toList
    logger.info(s"Converting references to ${flattenedInvokations.size} lines of CSV")
    transform(flattenedInvokations)
  }
}

object CsvOutput {
  case class ClassInvokation(referenced: String, caller: String, count: Int, referenceType: String)

  /**
   * Base function we use to convert case classes to CSV.
   *
   * Based on this article: https://schlining.medium.com/using-type-classes-to-quickly-convert-scala-3-case-classes-to-csv-69ce85b4739d
   * @param a The object to convert
   */
  def transform[A : Transformer](a: A): String = summon[Transformer[A]].f(a)

  // Base trait
  trait Transformer[T]:
    def f(t: T): String

  // Create a type class of T => String for every type in your case class
  given Transformer[String] with
    def f(x: String): String = x

  given Transformer[Int] with
    def f(x: Int): String = x.toString

  given Transformer[Double] with
    def f(x: Double) = f"$x%.2f"

  given [T] (using t: Transformer[T]): Transformer[Option[T]] =
    (x: Option[T]) => x match
      case None => ""
      case Some(x) => t.f(x)

  /**
   * Transforms a list of case classes into CSV data, including header row
   */
  given [A <: Product] (using t: Transformer[A]): Transformer[List[A]] =
    (x: List[A]) => (asHeader(x.head) :: x.map(transform)).mkString("\n")

  /**
   * Turns a case class into a CSV row string
   *
   * From https://kavedaa.github.io/auto-ui-generation/auto-ui-generation.html
   */
  inline given [A <: Product] (using m: Mirror.ProductOf[A]): Transformer[A] =
    new Transformer[A]:
      type ElemTransformers = Tuple.Map[m.MirroredElemTypes, Transformer]
      val elemTransformers: List[Transformer[Any]] = summonAll[ElemTransformers].toList.asInstanceOf[List[Transformer[Any]]]
      def f(a: A): String =
        val elems = a.productIterator.toList
        val transformed = elems.zip(elemTransformers) map { (elem, transformer) => transformer.f(elem) }
        transformed.mkString(",")


  /**
   * Turns a case class into a CSV header row string
   */
  def asHeader[A <: Product](a: A): String = a.productElementNames.toList.mkString(",")

}
