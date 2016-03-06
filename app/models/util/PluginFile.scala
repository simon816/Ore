package models.util

import java.nio.file.{Files, Path, Paths}
import java.util.jar.JarFile

import models.author.Author
import models.util.PluginFile._
import org.spongepowered.plugin.meta.{McModInfo, PluginMetadata}
import play.api.Play
import play.api.Play.current

import scala.collection.JavaConversions._

/**
  * Represents an uploaded plugin file.
  *
  * @param path Path to uploaded file
  */
case class PluginFile(private var path: Path, private val owner: Author) {

  def this(owner: Author) = this(Paths.get(TEMP_DIR).resolve(owner.name).resolve(TEMP_FILE), owner)

  private var meta: Option[PluginMetadata] = None

  /**
    * Returns the actual file path associated with this plugin.
    *
    * @return Path of plugin file
    */
  def getPath = this.path

  /**
    * Returns the owner of this file.
    *
    * @return Owner of file
    */
  def getOwner = this.owner

  def getMeta = this.meta

  /**
    * Reads the temporary file's plugin meta file and returns a new project
    * from it.
    *
    * TODO: Add PluginFiles to existing Projects
    *
    * @return Result of parse
    */
  def loadMeta: PluginMetadata = {
    // Read the JAR
    val jar = new JarFile(this.path.toFile)
    val metaEntry = jar.getEntry(META_FILE_NAME)
    if (metaEntry == null) {
      throw new Exception("No plugin meta file found.")
    }

    val metaList = McModInfo.DEFAULT.read(jar.getInputStream(metaEntry))
    if (metaList.size() > 1) {
      throw new Exception("Multiple plugins found.")
    }

    // Parse plugin meta info
    val meta = metaList.get(0)
    this.meta = Some(meta)
    val authors = meta.getAuthors.toList
    if (authors.isEmpty) {
      throw new Exception("No authors found.")
    }
    meta
  }

  /**
    * Returns true if this PluginFile has been uploaded to the appropriate
    * location.
    *
    * @return True if has been uploaded, false otherwise
    */
  def isUploaded: Boolean = {
    if (this.meta.isEmpty) {
      throw new Exception("No meta info found for plugin.")
    }
    val meta = this.meta.get
    this.path.equals(getUploadPath(this.owner.name, meta.getName, meta.getVersion))
  }

  /**
    * Uploads this PluginFile to the owner's upload directory.
    */
  def upload() = {
    if (isUploaded) {
      throw new Exception("Plugin already uploaded.")
    }
    val meta = this.meta.get
    val output = getUploadPath(this.owner.name, meta.getName, meta.getVersion)
    if (!Files.exists(output.getParent)) {
      Files.createDirectories(output.getParent)
    }
    Files.move(this.path, output)
    this.path = output
  }

  private def getUploadPath(owner: String, name: String, version: String) = {
    Paths.get(Play.application.path.getPath)
      .resolve(PLUGIN_DIR)
      .resolve(this.owner.name)
      .resolve(name + "-" + version + PLUGIN_FILE_EXTENSION)
  }

}

object PluginFile {

  val META_FILE_NAME = "mcmod.info"
  val PLUGIN_FILE_EXTENSION = ".jar"
  val PLUGIN_DIR = "uploads/plugins"
  val TEMP_DIR = "tmp"
  val TEMP_FILE = "plugin" + PLUGIN_FILE_EXTENSION

}
