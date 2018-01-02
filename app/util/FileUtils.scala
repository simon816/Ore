package util

import java.io.IOException
import java.lang.Long.numberOfLeadingZeros
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

object FileUtils {

  /**
    * Formats the number of bytes into a human readable file size
    * (e.g. 100.0 KB).
    *
    * Based on http://stackoverflow.com/a/24805871
    *
    * @param size The size in bytes
    * @return The formatted string
    */
  def formatFileSize(size: Long): String = {
    if (size < 1024) return s"$size B"
    val z = (63 - numberOfLeadingZeros(size)) / 10
    f"${size.toDouble / (1L << (z * 10))}%.1f ${" KMGTPE".charAt(z)}%sB"
  }

  /**
    * Deletes the directory at the specified [[Path]] with all of its contents.
    *
    * @param dir The directory to delete
    */
  def deleteDirectory(dir: Path): Unit = {
    Files.walkFileTree(dir, DeleteFileVisitor)
  }

  /**
    * Deletes the contents of a directory without deleteing the directory itself.
    *
    * @param dir The directory to clean
    */
  def cleanDirectory(dir: Path): Unit = {
    Files.walkFileTree(dir, new CleanFileVisitor(dir))
  }

  /**
    * Represents a [[java.nio.file.FileVisitor]] which will recursively delete a directory
    * with all its contents.
    */
  private class DeleteFileVisitor extends SimpleFileVisitor[Path] {

    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      if (Files.exists(file)) {
        Files.delete(file)
      }
      FileVisitResult.CONTINUE
    }

    override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
      if (Files.exists(dir)) {
        Files.delete(dir)
      }
      FileVisitResult.CONTINUE
    }

  }

  private object DeleteFileVisitor extends DeleteFileVisitor

  /**
    * Similar to [[DeleteFileVisitor]], except that it will only clean the folder
    * contents. It will not delete the given [[dir]].
    *
    * @param dir The directory to clean
    */
  private class CleanFileVisitor(private val dir: Path) extends DeleteFileVisitor {

    override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
      if (dir != this.dir) {
        super.postVisitDirectory(dir, exc)
      } else {
        FileVisitResult.CONTINUE
      }
    }

  }

}
