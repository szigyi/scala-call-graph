package testing

import com.typesafe.scalalogging.StrictLogging

import java.io.{BufferedWriter, File, FileWriter}

object TestingIO extends StrictLogging {

  def createOrClearTempDir(tempDir: File): Unit = {
    def recursiveDelete(dir: File): Unit = {
      logger.trace(s"Deleting files in ${dir.getPath}")
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
