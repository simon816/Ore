package plugin

import java.io.{FileInputStream, FileOutputStream, IOException}
import java.nio.file.{Files, Path, Paths}
import java.util.jar.{JarEntry, JarInputStream}
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}

import models.auth.User
import org.spongepowered.plugin.meta.{McModInfo, PluginMetadata}

import scala.util.control.Breaks._

/**
  * Represents an uploaded plugin file.
  *
  * @param path Path to uploaded file
  */
class PluginFile(private var path: Path, private val owner: User) {

  private val META_FILE_NAME = "mcmod.info"

  private var meta: Option[PluginMetadata] = None

  /**
    * Returns the actual file path associated with this plugin.
    *
    * @return Path of plugin file
    */
  def getPath: Path = this.path

  /**
    * Deletes the File at this PluginFile's Path
    */
  def delete() = Files.delete(this.path)

  /**
    * Returns the owner of this file.
    *
    * @return Owner of file
    */
  def getOwner: User = this.owner

  /**
    * Returns the loaded PluginMetadata, if any.
    *
    * @return PluginMetadata if present, None otherwise
    */
  def getMeta: Option[PluginMetadata] = this.meta

  def isZipped: Boolean = {
    this.path.toString.endsWith(".zip")
  }

  def zip: Path = {
    val path = this.path.toString
    val zipPath = path.substring(0, path.lastIndexOf('.')) + ".zip"
    val out = new ZipOutputStream(new FileOutputStream(zipPath))
    val entry = new ZipEntry(this.path.getFileName.toString)
    out.putNextEntry(entry)

    val in = new FileInputStream(this.path.toString)
    var len: Int = 0
    val buffer: Array[Byte] = new Array[Byte](4096)
    while ({ len = in.read(buffer); len } > 0) {
      out.write(buffer, 0, len)
    }

    in.close()
    out.close()

    this.path = Paths.get(zipPath)
    this.path
  }

  /**
    * Reads the temporary file's plugin meta file and returns the result.
    *
    * @return Result of parse
    */
  def loadMeta: PluginMetadata = {
    // Try to read ZIP first
    var zip: ZipFile = null
    if (this.path.toString.endsWith(".zip")) {
      try {
        zip = new ZipFile(this.path.toFile)
      } catch {
        case ignored: IOException => ;
      }
    }

    // Find plugin JAR
    var jarIn: JarInputStream = null
    try {
      if (zip != null) {
        var pluginEntry: ZipEntry = null
        val entries = zip.entries()
        breakable {
          while (entries.hasMoreElements) {
            val entry = entries.nextElement()
            val name = entry.getName
            if (!entry.isDirectory && name.split("/").length == 1 && name.endsWith(".jar")) {
              pluginEntry = entry
              break
            }
          }
        }
        if (pluginEntry == null) {
          throw new InvalidPluginFileException("Could not find a JAR file in the top level of ZIP file.")
        }
        jarIn = new JarInputStream(zip.getInputStream(pluginEntry))
      } else {
        jarIn = new JarInputStream(Files.newInputStream(this.path))
      }
    } catch {
      case ioe: IOException => throw new InvalidPluginFileException(cause = ioe)
    }

    // Find plugin meta file
    var entry: JarEntry = null
    var metaFound: Boolean = false
    breakable {
      while ({ entry = jarIn.getNextJarEntry; entry } != null) {
        if (entry.getName.equals(META_FILE_NAME)) {
          metaFound = true
          break
        }
      }
    }

    if (!metaFound) {
      throw new InvalidPluginFileException("No plugin meta file found.")
    }

    val metaList = McModInfo.DEFAULT.read(jarIn)
    if (metaList.size() > 1) {
      throw new InvalidPluginFileException("No plugin meta file found.")
    }

    // Parse plugin meta info
    val meta = metaList.get(0)
    this.meta = Some(meta)
    meta
  }

}
