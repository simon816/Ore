package ore.project.util

import java.io.IOException
import java.nio.file.{Files, Path}
import java.util.jar.{JarEntry, JarInputStream}
import java.util.zip.{ZipEntry, ZipFile}

import models.user.User
import ore.UserOwned
import org.apache.commons.codec.digest.DigestUtils
import org.spongepowered.plugin.meta.{McModInfo, PluginMetadata}

import scala.util.control.Breaks._

/**
  * Represents an uploaded plugin file.
  *
  * @param _path Path to uploaded file
  */
class PluginFile(private var _path: Path, val user: User) extends UserOwned {

  private val MetaFileName = "mcmod.info"

  private var _meta: Option[PluginMetadata] = None

  /**
    * Returns the actual file path associated with this plugin.
    *
    * @return Path of plugin file
    */
  def path: Path = this._path

  /**
    * Moves this PluginFile to the specified [[Path]].
    *
    * @param path Path to move file to
    */
  def move(path: Path) = {
    Files.move(this.path, path)
    this._path = path
  }

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
    * Returns an MD5 hash of this PluginFile.
    *
    * @return MD5 hash
    */
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
        while ({ entry = jarIn.getNextJarEntry; entry } != null) {
          if (entry.getName.equals(MetaFileName)) {
            metaFound = true
            break
          }
        }
      }

      if (!metaFound) {
        throw new InvalidPluginFileException("No plugin meta entries found.")
      }

      val metaList = McModInfo.DEFAULT.read(jarIn)
      jarIn.close()
      if (metaList.size() > 1) {
        throw new InvalidPluginFileException("Multiple meta entries found.")
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
