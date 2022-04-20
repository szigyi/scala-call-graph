package hu.szigyi.scala.graph

import com.typesafe.scalalogging.StrictLogging

import java.io.{BufferedWriter, File, FileWriter}

class ScalaIO extends StrictLogging {

  def writeFile(dir: File, fileName: String, body: String): Unit = {
    logger.info(s"Writing $fileName file to ${dir.getPath}")
    val file = new File(dir, fileName)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(body)
    bw.close()
  }
}
