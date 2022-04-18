package testing

import com.typesafe.scalalogging.StrictLogging

import java.io.{BufferedWriter, File, FileWriter}

object ScalaIO extends StrictLogging {

  def writeAsScalaFile(tempDir: File, className: String, body: String): Unit = {
    val file = new File(tempDir, className + ".scala")
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(body)
    bw.close()
  }

  def writeFile(tempDir: File, fileName: String, body: String): Unit = {
    val file = new File(tempDir, fileName)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(body)
    bw.close()
  }

  def writeAsBuildProperties(tempDir: File, body: String): Unit = {
    val file = new File(tempDir, "build.properties")
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(body)
    bw.close()
  }

  def createOrClearTempDir(tempDir: File): Unit = {
    def recursiveDelete(dir: File): Unit = {
      logger.debug(s"Deleting files in ${dir.getPath}")
      dir.listFiles().map { file =>
        if (file.isDirectory) {
          recursiveDelete(file)
          file.delete()
        } else {
          file.delete()
        }
      }
    }
    if (!tempDir.exists()) tempDir.mkdirs()
    recursiveDelete(tempDir)
  }
}
