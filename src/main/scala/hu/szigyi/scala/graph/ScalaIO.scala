package hu.szigyi.scala.graph

import com.typesafe.scalalogging.StrictLogging

import java.io.{BufferedWriter, File, FileWriter}

object ScalaIO extends StrictLogging {

  def writeFile(tempDir: File, fileName: String, body: String): Unit = {
    val file = new File(tempDir, fileName)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(body)
    bw.close()
  }
}
