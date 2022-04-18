package hu.szigyi.scala.graph

import cats.effect.unsafe.implicits.global
import cats.effect.IO
import hu.szigyi.scala.graph.Model._
import hu.szigyi.scala.graph.ScalaCallGraph
import org.scalatest.freespec.{AnyFreeSpec, AsyncFreeSpec}
import org.scalatest.matchers.should.Matchers
import testing.JarsBuilder

import java.io.{ByteArrayOutputStream, File, PrintStream}

class CallGraphSpec extends AnyFreeSpec with Matchers {

  "Class Level" - {
    "self references" - {
      "should return the self references for native method" in {
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

      "should return the self references" in {
        val builder = new JarsBuilder()
        builder.add("ClassA","""
          class ClassA {
            def methodA(): Unit = {
              methodB()
            }

            def methodB(): Unit = ???
          }""")
        val jarFile = builder.build()

        val result = ScalaCallGraph.runApp(List[String](jarFile.getPath)).unsafeRunSync()

        result shouldBe Set(
          ClassLevel("java.lang.Object", Set(Special("ClassA", 1))),
          ClassLevel("ClassA", Set(Virtual("ClassA", 1))),
          ClassLevel("scala.Predef$", Set(Virtual("ClassA", 1)))
        )
      }

      "should correctly count the number of self references" in {
        val builder = new JarsBuilder()
        builder.add("ClassA","""
          class ClassA {
            def methodA(): Unit = {
              methodB()
              methodC()
            }

            def methodB(): Unit = ???
            def methodC(): Unit = ???
          }""")
        val jarFile = builder.build()

        val result = ScalaCallGraph.runApp(List[String](jarFile.getPath)).unsafeRunSync()

        result shouldBe Set(
          ClassLevel("java.lang.Object", Set(Special("ClassA", 1))),
          ClassLevel("ClassA", Set(Virtual("ClassA", 2))),
          ClassLevel("scala.Predef$", Set(Virtual("ClassA", 1)))
        )
      }
    }

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

          val result = ScalaCallGraph.runApp(List[String](jarFile.getPath)).unsafeRunSync()

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

          val result = ScalaCallGraph.runApp(List[String](jarFile.getPath)).unsafeRunSync()

          result shouldBe Set(
            ClassLevel("java.lang.Object", Set(Special("ClassA", 1), Special("ClassB", 1))),
            ClassLevel("ClassA", Set(Special("ClassB", 1), Virtual("ClassB", 1))),
            ClassLevel("ClassB", Set(Special("ClassA", 1), Virtual("ClassA", 1)))
          )
        }
      }

      "static references" - {
        "should return the static reference from class to object" in {
          val builder = new JarsBuilder()
          builder.add("ClassA","""
          class ClassA {
            def methodA(): Unit = {
              ClassB.methodB()
            }
          }""")
          builder.add("ClassB","""
          object ClassB {
            def methodB(): Unit = ???
          }""")
          val jarFile = builder.build()

          val result = ScalaCallGraph.runApp(List[String](jarFile.getPath)).unsafeRunSync()

          result shouldBe Set(
            ClassLevel("java.lang.Object", Set(Special("ClassA", 1), Special("ClassB$", 1))),
            ClassLevel("ClassB$", Set(Virtual("ClassA", 1), Special("ClassB$", 1), Virtual("ClassB", 1))),
            ClassLevel("scala.runtime.ModuleSerializationProxy", Set(Special("ClassB$", 1))),
            ClassLevel("scala.Predef$", Set(Virtual("ClassB$", 1)))
          )
        }

        "should return the static references between two objects" in {
          val builder = new JarsBuilder()
          builder.add("ClassA","""
          object ClassA {
            def methodA(): Unit = {
              ClassB.methodB()
            }
          }""")
          builder.add("ClassB","""
          object ClassB {
            def methodB(): Unit = {
              ClassA.methodA()
            }
          }""")
          val jarFile = builder.build()

          val result = ScalaCallGraph.runApp(List[String](jarFile.getPath)).unsafeRunSync()

          result shouldBe Set(
            ClassLevel("java.lang.Object", Set(Special("ClassA$", 1), Special("ClassB$", 1))),
            ClassLevel("ClassA$", Set(Special("ClassA$", 1), Virtual("ClassA", 1), Virtual("ClassB$", 1))),
            ClassLevel("scala.runtime.ModuleSerializationProxy", Set(Special("ClassA$", 1), Special("ClassB$", 1))),
            ClassLevel("ClassB$", Set(Virtual("ClassA$", 1), Special("ClassB$", 1), Virtual("ClassB", 1)))
          )
        }
      }
    }
  }
}
