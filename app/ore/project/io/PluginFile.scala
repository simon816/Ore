package ore.project.io

import java.io.{IOException, InputStream}
import java.nio.file.{Files, Path}
import java.util.jar.{JarEntry, JarInputStream}
import java.util.zip.{ZipEntry, ZipFile}

import models.user.User
import ore.user.UserOwned
import org.apache.commons.codec.digest.DigestUtils
import org.spongepowered.plugin.meta.{McModInfo, PluginMetadata}
import play.api.i18n.MessagesApi

import scala.collection.JavaConverters._
import scala.util.control.Breaks._

/**
  * Represents an uploaded plugin file.
  *
  * @param _path Path to uploaded file
  */
class PluginFile(private var _path: Path, val signaturePath: Path, val user: User) extends UserOwned {

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
    * TODO: More validation on PluginMetadata results (null checks, etc)
    *
    * @return Result of parse
    */
  @throws[InvalidPluginFileException]
  def loadMeta()(implicit messages: MessagesApi): PluginMetadata = {
    var jarIn: JarInputStream = null

    try {
      // Find plugin JAR
      jarIn = new JarInputStream(newJarStream)

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
        throw InvalidPluginFileException("error.plugin.metaNotFound")

      // Read the meta file
      val metaList = McModInfo.DEFAULT.read(jarIn).asScala.toList
      if (metaList.isEmpty)
        throw InvalidPluginFileException("error.plugin.metaNotFound")

      // Parse plugin meta info
      val meta = metaList.head

      // check meta
      def checkMeta(value: Any, field: String) = {
        if (value == null)
          throw InvalidPluginFileException(messages("error.plugin.incomplete", field))
      }
      checkMeta(meta.getName, "name")
      checkMeta(meta.getVersion, "version")

      this._meta = Some(meta)
      meta
    } catch {
      case pe: InvalidPluginFileException =>
        throw pe
      case e: Exception =>
        throw InvalidPluginFileException(cause = e)
    } finally {
      if (jarIn != null)
        jarIn.close()
      else
        throw InvalidPluginFileException("error.plugin.unexpected")
    }
  }

  /**
    * Returns a new [[InputStream]] for this [[PluginFile]]'s main JAR file.
    *
    * @return InputStream of JAR
    */
  @throws[IOException]
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
      throw InvalidPluginFileException("error.plugin.jarNotFound")
    pluginEntry
  }

  override def userId: Int = this.user.id.get

}
