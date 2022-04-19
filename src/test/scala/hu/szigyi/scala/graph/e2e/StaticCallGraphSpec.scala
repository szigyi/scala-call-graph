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

class StaticCallGraphSpec extends AnyFreeSpec with Matchers {

  "static references" - {
    "should return the static reference from class to object" in {
      val builder = new JarsBuilder()
      builder.add("ClassA",
        """
          class ClassA {
            def methodA(): Unit = {
              ClassB.methodB()
            }
          }""")
      builder.add("ClassB",
        """
          object ClassB {
            def methodB(): Unit = ???
          }""")
      val jarFile = builder.build()

      val result = new ScalaCallGraph(new Service).callGraph(jarFile.getPath, None).unsafeRunSync()

      result shouldBe Set(
        ClassLevel("java.lang.Object", Set(Special("ClassA", 1), Special("ClassB$", 1))),
        ClassLevel("ClassB$", Set(Virtual("ClassA", 1), Special("ClassB$", 1), Virtual("ClassB", 1))),
        ClassLevel("scala.runtime.ModuleSerializationProxy", Set(Special("ClassB$", 1))),
        ClassLevel("scala.Predef$", Set(Virtual("ClassB$", 1)))
      )
    }

    "should return the static references between two objects" in {
      val builder = new JarsBuilder()
      builder.add("ClassA",
        """
          object ClassA {
            def methodA(): Unit = {
              ClassB.methodB()
            }
          }""")
      builder.add("ClassB",
        """
          object ClassB {
            def methodB(): Unit = {
              ClassA.methodA()
            }
          }""")
      val jarFile = builder.build()

      val result = new ScalaCallGraph(new Service).callGraph(jarFile.getPath, None).unsafeRunSync()

      result shouldBe Set(
        ClassLevel("java.lang.Object", Set(Special("ClassA$", 1), Special("ClassB$", 1))),
        ClassLevel("ClassA$", Set(Special("ClassA$", 1), Virtual("ClassA", 1), Virtual("ClassB$", 1))),
        ClassLevel("scala.runtime.ModuleSerializationProxy", Set(Special("ClassA$", 1), Special("ClassB$", 1))),
        ClassLevel("ClassB$", Set(Virtual("ClassA$", 1), Special("ClassB$", 1), Virtual("ClassB", 1)))
      )
    }
  }
}
