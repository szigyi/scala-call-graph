package hu.szigyi.scala.graph

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import hu.szigyi.scala.graph.Model._
import hu.szigyi.scala.graph.ScalaCallGraph
import org.scalatest.freespec.{AnyFreeSpec, AsyncFreeSpec}
import org.scalatest.matchers.should.Matchers
import testing.JarsBuilder

import java.io.{ByteArrayOutputStream, File, PrintStream}

class NativeCallGraphSpec extends AnyFreeSpec with Matchers {

  "should return the self reference for native method" in {
    val builder = new JarsBuilder()
    builder.add("ClassA","""
      class ClassA {
        def methodA(): Unit = {
          methodB()
        }

        @native def methodB(): Unit
      }""")
    val jarFile = builder.build()

    val result = ScalaCallGraph.runApp(List[String](jarFile.getPath)).unsafeRunSync()

    result shouldBe Set(
      ClassLevel("java.lang.Object", Set(Special("ClassA", 1))),
      ClassLevel("ClassA", Set(Virtual("ClassA", 1)))
    )
  }

  "should return the reference for native method" in {
    val builder = new JarsBuilder()
    builder.add("ClassA","""
      class ClassA {
        @native def methodA(): Unit
      }""")
    builder.add("ClassB","""
      class ClassB {
        def methodB(): Unit = {
          new ClassA().methodA()
        }
      }""")
    val jarFile = builder.build()

    val result = ScalaCallGraph.runApp(List[String](jarFile.getPath)).unsafeRunSync()

    result shouldBe Set(
      ClassLevel("java.lang.Object", Set(Special("ClassA", 1), Special("ClassB", 1))),
      ClassLevel("ClassA", Set(Special("ClassB", 2), Virtual("ClassB", 2)))
    )
  }
}
