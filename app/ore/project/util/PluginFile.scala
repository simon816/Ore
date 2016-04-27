package ore.project.util

import java.io.{FileInputStream, FileOutputStream, IOException}
import java.nio.file.{Files, Path, Paths}
import java.util.jar.{JarEntry, JarInputStream}
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}

import db.orm.model.UserOwner
import models.user.User
import org.apache.commons.codec.digest.DigestUtils
import org.spongepowered.plugin.meta.{McModInfo, PluginMetadata}

import scala.util.control.Breaks._

/**
  * Represents an uploaded plugin file.
  *
  * @param _path Path to uploaded file
  */
class PluginFile(private var _path: Path, override val user: User) extends UserOwner {

  private val MetaFileName = "mcmod.info"

  private var _meta: Option[PluginMetadata] = None

  /**
    * Returns the actual file path associated with this plugin.
    *
    * @return Path of plugin file
    */
  def path: Path = this._path

  /**
    * Deletes the File at this PluginFile's Path
    */
  def delete() = Files.delete(this._path)

  /**
    * Returns the loaded PluginMetadata, if any.
    *
    * @return PluginMetadata if present, None otherwise
    */
  def meta: Option[PluginMetadata] = this._meta

  /**
    * Returns true if this is a ZIP file.
    *
    * @return True if zip file
    */
  def isZipped: Boolean = {
    this._path.toString.endsWith(".zip")
  }

  /**
    * Wraps this file in a ZIP archive.
    *
    * @return New path
    */
  def zip: Path = {
    val path = this._path.toString
    val zipPath = path.substring(0, path.lastIndexOf('.')) + ".zip"
    val out = new ZipOutputStream(new FileOutputStream(zipPath))
    val entry = new ZipEntry(this._path.getFileName.toString)
    out.putNextEntry(entry)

    val in = new FileInputStream(this._path.toString)
    var len: Int = 0
    val buffer: Array[Byte] = new Array[Byte](4096)
    while ({ len = in.read(buffer); len } > 0) {
      out.write(buffer, 0, len)
    }

    in.close()
    out.close()

    this._path = Paths.get(zipPath)
    this._path
  }

  def md5: String = DigestUtils.md5Hex(Files.newInputStream(this.path))


  /**
    * Reads the temporary file's plugin meta file and returns the result.
    *
    * @return Result of parse
    */
  def loadMeta: PluginMetadata = {
    try {
      // Try to read ZIP first
      var zip: ZipFile = null
      if (this._path.toString.endsWith(".zip")) {
        try {
          zip = new ZipFile(this._path.toFile)
        } catch {
          case ignored: IOException => ;
        }
      }

      // Find plugin JAR
      var jarIn: JarInputStream = null
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
        jarIn = new JarInputStream(Files.newInputStream(this._path))
      }

      // Find plugin meta file
      var entry: JarEntry = null
      var metaFound: Boolean = false
      breakable {
        while ( {
          entry = jarIn.getNextJarEntry; entry
        } != null) {
          if (entry.getName.equals(MetaFileName)) {
            metaFound = true
            break
          }
        }
      }

      if (!metaFound) {
        throw new InvalidPluginFileException("No plugin meta file found.")
      }

      val metaList = McModInfo.DEFAULT.read(jarIn)
      jarIn.close()
      if (metaList.size() > 1) {
        throw new InvalidPluginFileException("Multiple meta files found.")
      }

      // Parse plugin meta info
      val meta = metaList.get(0)
      this._meta = Some(meta)
      meta
    } catch {
      case e: Exception => throw new InvalidPluginFileException(cause = e)
    }
  }

  override def userId: Int = this.user.id.get

}
