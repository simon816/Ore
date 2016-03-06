package controllers

import java.io.{IOException, File}
import java.nio.file.{FileAlreadyExistsException, Paths, Files}
import java.util.jar.JarFile

import models.Project
import models.author.Author
import org.spongepowered.plugin.meta.{PluginMetadata, McModInfo}
import play.api.Play
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.Play.current
import scala.collection.JavaConversions._

/**
  * Represents an uploaded plugin file.
  *
  * @param file Uploaded file
  */
case class PluginFile(private val file: File, private val owner: Author) {

  val META_FILE_NAME = "mcmod.info"
  val PLUGIN_FILE_EXTENSION = ".jar"
  val PLUGIN_DIR = "uploads/plugins"

  private var result: Option[Result] = None
  private var meta: Option[PluginMetadata] = None

  /**
    * Returns the actual file associated with this plugin.
    *
    * @return File of plugin
    */
  def getFile = this.file

  /**
    * Returns the result, if any, produced by a parse.
    *
    * @return Result if present, none otherwise
    */
  def getResult = this.result

  /**
    * Returns the owner of this file.
    *
    * @return Owner of file
    */
  def getOwner = this.owner

  /**
    * Reads the temporary file's plugin meta file and returns a new project
    * from it.
    *
    * TODO: Add PluginFiles to existing Projects
    *
    * @return Result of parse
    */
  def parse: Option[Project] = {
    this.result = None

    // Read the JAR
    var jar : JarFile = null
    try {
      jar = new JarFile(this.file)
    } catch {
      case ioe: IOException =>
        this.result = Some(BadRequest("Error reading plugin file."))
        return None
    }
    val metaEntry = jar.getEntry(META_FILE_NAME)
    if (metaEntry == null) {
      this.result = Some(BadRequest("No plugin meta file found."))
      return None
    }
    val metaList = McModInfo.DEFAULT.read(jar.getInputStream(metaEntry))
    if (metaList.size() > 1) {
      this.result = Some(BadRequest("Multiple plugins found."))
      return None
    }

    // Parse plugin meta info
    val meta = metaList.get(0)
    this.meta = Some(meta)
    val authors = meta.getAuthors.toList
    if (authors.isEmpty) {
      this.result = Some(BadRequest("No authors found."))
      return None
    }

    val project = Project.fromMeta(this.owner, meta)
    project.setPendingUpload(this)
    Some(project)
  }

  /**
    * Uploads this PluginFile to the owner's upload directory.
    */
  def upload = {
    this.result = None
    val meta = this.meta.get
    val output = getUploadPath(this.owner.name, meta.getName, meta.getVersion)
    if (!Files.exists(output.getParent)) {
      Files.createDirectories(output.getParent)
    }
    try {
      Files.move(Paths.get(file.getPath), output)
    } catch {
      case e: FileAlreadyExistsException =>
        this.result = Some(BadRequest("Version already exists."))
    }
  }

  private def getUploadPath(owner: String, name: String, version: String) = {
    Paths.get(Play.application.path.getPath)
      .resolve(PLUGIN_DIR)
      .resolve(this.owner.name)
      .resolve(name + "-" + version + PLUGIN_FILE_EXTENSION)
  }

}
