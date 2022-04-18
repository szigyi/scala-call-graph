package hu.szigyi.scala.graph

import hu.szigyi.scala.graph.Model.{ClassLevel, Virtual}
import hu.szigyi.scala.graph.app.ScalaCallGraph
import hu.szigyi.scala.graph.app.ScalaCallGraph.filterByPackage
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class FilterByPackageSpec extends AnyFreeSpec with Matchers {

  "should return the classes in the given package" in {
    val packagePattern = Some("main.sub.a")
    val cls = Set(
      ClassLevel("main.sub.a.ClassA", Set(Virtual("java.lang.Object", 1), Virtual("main.sub.a.ClassB", 1))),
      ClassLevel("java.lang.Object", Set(Virtual("java.lang.Object", 1), Virtual("main.sub.a.ClassB", 1)))
    )

    filterByPackage(packagePattern, cls) shouldBe Set(ClassLevel("main.sub.a.ClassA", Set(Virtual("main.sub.a.ClassB", 1))))
  }

  "should not return the classes when none of the references coming from the given package" in {
    val packagePattern = Some("main.sub.a")
    val cls = Set(
      ClassLevel("main.sub.a.ClassA", Set(Virtual("java.lang.Object", 1), Virtual("main.sub.b.ClassB", 1), Virtual("main.other.ClassC", 1)))
    )

    filterByPackage(packagePattern, cls) shouldBe Set.empty
  }
}
