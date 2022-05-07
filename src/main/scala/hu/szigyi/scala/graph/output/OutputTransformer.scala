package hu.szigyi.scala.graph.output

object OutputTransformer {
//  /**
//   * Base function we use to convert case classes to CSV.
//   *
//   * Based on this article: https://schlining.medium.com/using-type-classes-to-quickly-convert-scala-3-case-classes-to-csv-69ce85b4739d
//   * @param a The object to convert
//   */
//  def transform[A : Transformer](a: A): String = summon[Transformer[A]].f(a)
//
//  // Base trait
//  trait Transformer[T]:
//    def f(t: T): String
//
//  // Create a type class of T => String for every type in your case class
//  given Transformer[String] with
//    def f(x: String): String = x
//
//  given Transformer[Int] with
//    def f(x: Int): String = x.toString
//
//  given [T] (using t: Transformer[T]): Transformer[Option[T]] =
//    (x: Option[T]) => x match
//      case None => ""
//      case Some(x) => t.f(x)
}
