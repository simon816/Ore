package plugin

import java.io.IOException
import java.nio.file.{Files, Path}
import java.util.jar.JarFile

import models.auth.User
import org.spongepowered.plugin.meta.{McModInfo, PluginMetadata}

import scala.collection.JavaConversions._
import scala.util.Try

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

  /**
    * Reads the temporary file's plugin meta file and returns the result.
    *
    * @return Result of parse
    */
  def loadMeta: PluginMetadata = {
    // Read the JAR
    var jar: JarFile = null
    try {
      jar = new JarFile(this.path.toFile)
    } catch {
      case ioe: IOException => throw new InvalidPluginFileException(cause = ioe)
    }

    val metaEntry = jar.getEntry(META_FILE_NAME)
    if (metaEntry == null) {
      throw new InvalidPluginFileException("No plugin meta file found.")
    }

    val metaList = McModInfo.DEFAULT.read(jar.getInputStream(metaEntry))
    if (metaList.size() > 1) {
      throw new InvalidPluginFileException("No plugin meta file found.")
    }

    // Parse plugin meta info
    val meta = metaList.get(0)
    this.meta = Some(meta)
    val authors = meta.getAuthors.toList
    if (authors.isEmpty) {
      throw new InvalidPluginFileException("No authors found.")
    }
    meta
  }

}
