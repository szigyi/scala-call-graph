package hu.szigyi.scala.graph.e2e

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import hu.szigyi.scala.graph.Model.*
import hu.szigyi.scala.graph.app.ScalaCallGraph
import hu.szigyi.scala.graph.service.Service
import org.scalatest.freespec.{AnyFreeSpec, AsyncFreeSpec}
import org.scalatest.matchers.should.Matchers
import testing.JarsBuilder

import java.io.{ByteArrayOutputStream, File, PrintStream}

class CallGraphSpec extends AnyFreeSpec with Matchers {

  "Class Level" - {
    "different classes referencing each other" - {
      "virtual references" - {
        "should return the references between two classes" in {
          val builder = new JarsBuilder()
          builder.add("ClassA","""
          class ClassA {
            private val b: ClassB = null
            def methodA(): Unit = {
              b.methodB()
            }
          }""")
          builder.add("ClassB","""
          class ClassB {
            private val a: ClassA = null
            def methodB(): Unit = {
              a.methodA()
            }
          }""")
          val jarFile = builder.build()

          val result = new ScalaCallGraph(new Service).callGraph(jarFile.getPath, None).unsafeRunSync()

          result shouldBe Set(
            ClassLevel("java.lang.Object", Set(Special("ClassA", 1), Special("ClassB", 1))),
            ClassLevel("ClassA", Set(Virtual("ClassB", 1))),
            ClassLevel("ClassB", Set(Virtual("ClassA", 1)))
          )
        }

        "should return the references between two classes, counting the constructor as well" in {
          val builder = new JarsBuilder()
          builder.add("ClassA","""
          class ClassA {
            private val b = new ClassB
            def methodA(): Unit = {
              b.methodB()
            }
          }""")
          builder.add("ClassB","""
          class ClassB {
            private val a = new ClassA
            def methodB(): Unit = {
              a.methodA()
            }
          }""")
          val jarFile = builder.build()

          val result = new ScalaCallGraph(new Service).callGraph(jarFile.getPath, None).unsafeRunSync()

          result shouldBe Set(
            ClassLevel("java.lang.Object", Set(Special("ClassA", 1), Special("ClassB", 1))),
            ClassLevel("ClassA", Set(Special("ClassB", 1), Virtual("ClassB", 1))),
            ClassLevel("ClassB", Set(Special("ClassA", 1), Virtual("ClassA", 1)))
          )
        }
      }
    }
  }
}
