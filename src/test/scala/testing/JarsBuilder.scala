package testing

import com.typesafe.scalalogging.StrictLogging
import hu.szigyi.scala.graph.ScalaIO
import testing.TestingIO.*

import java.io.*
import java.util.jar.{Attributes, JarEntry, JarOutputStream, Manifest}
import javax.tools.JavaFileObject
import scala.collection.mutable
import scala.language.postfixOps
import scala.sys.process.*

class JarsBuilder(io: ScalaIO) extends StrictLogging {

  private val TEMP_DIR = "/tmp/graph/classes/"
  private val classFiles = mutable.ListBuffer.empty[(String, String)]
  private val buildSbt = """
    lazy val root = (project in file("."))
      .settings(
        name := "scala-callgraph-test",
        version := "0.1.0",

        scalaVersion := "3.0.0"
      )"""

  def add(className: String, classBody: String): Unit =
    classFiles.addOne(className, classBody)

  def build(): File = {
    val root = new File(TEMP_DIR)
    val srcDir = new File(root, "/src/main/scala")
    createOrClearTempDir(root)
    srcDir.mkdirs()

    logger.info(s"Writing ${classFiles.size} scala classes to $TEMP_DIR")
    import scala.jdk.CollectionConverters.*
    classFiles.toSeq.foreach { case (className, classBody) =>
      io.writeFile(srcDir, className + ".scala", classBody)
    }

    io.writeFile(root, "build.sbt", buildSbt)
    io.writeFile(root, "build.sbt", buildSbt)

    logger.info("Building Jar from compiled classes")
    logger.info(Process("sbt package", new File(TEMP_DIR)).!!)

    new File(root, "/target/scala-3.0.0/scala-callgraph-test_3-0.1.0.jar")
  }
}
