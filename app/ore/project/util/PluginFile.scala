package ore.project.util

import java.io.InputStream
import java.nio.file.{Files, Path}
import java.util.jar.{JarEntry, JarInputStream}
import java.util.zip.{ZipEntry, ZipFile}

import models.user.User
import ore.UserOwned
import org.apache.commons.codec.digest.DigestUtils
import org.spongepowered.plugin.meta.{McModInfo, PluginMetadata}

import scala.util.control.Breaks._
import scala.collection.JavaConverters._

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
  def loadMeta(): PluginMetadata = {
    try {
      // Find plugin JAR
      val jarIn: JarInputStream = new JarInputStream(newJarStream)

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

      if (!metaFound)
        throw new InvalidPluginFileException("No plugin meta file found.")

      // Read the meta file
      val metaList = McModInfo.DEFAULT.read(jarIn).asScala.toList
      jarIn.close()
      if (metaList.isEmpty)
        throw new InvalidPluginFileException("No plugin meta entries found.")

      // Parse plugin meta info
      val meta = metaList.head
      this._meta = Some(meta)
      meta
    } catch {
      case e: Exception =>
        throw new InvalidPluginFileException(cause = e)
    }
  }

  /**
    * Returns a new [[InputStream]] for this [[PluginFile]]'s main JAR file.
    *
    * @return InputStream of JAR
    */
  def newJarStream: InputStream = {
    if (this.path.toString.endsWith(".jar"))
      Files.newInputStream(this.path)
    else {
      val zip = new ZipFile(this.path.toFile)
      zip.getInputStream(findTopLevelJar(zip))
    }
  }

  private def findTopLevelJar(zip: ZipFile): ZipEntry = {
    if (this.path.toString.endsWith(".jar"))
      throw new Exception("Plugin is already JAR")

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

    if (pluginEntry == null)
      throw new InvalidPluginFileException("Could not find a JAR file in the top level of ZIP file.")
    pluginEntry
  }

  override def userId: Int = this.user.id.get

}
