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

class NativeCallGraphSpec extends AnyFreeSpec with Matchers {

  "should return the self reference for native method" in {
    val builder = new JarsBuilder(new ScalaIO)
    builder.add("ClassA","""
      class ClassA {
        def methodA(): Unit = {
          methodB()
        }

        @native def methodB(): Unit
      }""")
    val jarFile = builder.build()

    val result = new ScalaCallGraph(new Service).callGraph(jarFile, None).unsafeRunSync()

    result shouldBe Set(
      ClassLevel("java.lang.Object", Set(Special("ClassA", 1))),
      ClassLevel("ClassA", Set(Virtual("ClassA", 1)))
    )
  }

  "should return the reference for native method" in {
    val builder = new JarsBuilder(new ScalaIO)
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

    val result = new ScalaCallGraph(new Service).callGraph(jarFile, None).unsafeRunSync()

    result shouldBe Set(
      ClassLevel("java.lang.Object", Set(Special("ClassA", 1), Special("ClassB", 1))),
      ClassLevel("ClassA", Set(Special("ClassB", 1), Virtual("ClassB", 1)))
    )
  }
}
