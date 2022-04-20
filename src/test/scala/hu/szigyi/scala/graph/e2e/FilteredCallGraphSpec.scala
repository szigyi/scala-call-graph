package hu.szigyi.scala.graph.e2e

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import hu.szigyi.scala.graph.Model.*
import hu.szigyi.scala.graph.ScalaIO
import hu.szigyi.scala.graph.app.ScalaCallGraph
import hu.szigyi.scala.graph.service.Service
import org.scalatest.freespec.{AnyFreeSpec, AsyncFreeSpec}
import org.scalatest.matchers.should.Matchers
import testing.JarsBuilder

import java.io.{ByteArrayOutputStream, File, PrintStream}

class FilteredCallGraphSpec extends AnyFreeSpec with Matchers {

  "should return the filtered references" in {
    val builder = new JarsBuilder(new ScalaIO)
    builder.add("ClassA",
      """
        package main.sub.a
        import main.sub.b.ClassB
        class ClassA {
          private val b: ClassB = null
          def methodA(): Unit = {
            b.methodB()
          }
        }""")
    builder.add("ClassB",
      """
        package main.sub.b
        import main.sub.a.ClassA
        class ClassB {
          private val a: ClassA = null
          def methodB(): Unit = {
            a.methodA()
          }
        }""")
    builder.add("ClassC",
      """
        package main.other
        import main.sub.a.ClassA
        class ClassC {
          private val a: ClassA = null
          def methodC(): Unit = {
            a.methodA()
          }
        }""")
    val jarFile = builder.build()

    val result = new ScalaCallGraph(new Service).callGraph(jarFile, Some("main.sub")).unsafeRunSync()

    result shouldBe Set(
      ClassLevel("main.sub.a.ClassA", Set(Virtual("main.sub.b.ClassB", 1))),
      ClassLevel("main.sub.b.ClassB", Set(Virtual("main.sub.a.ClassA", 1)))
    )
  }
}
