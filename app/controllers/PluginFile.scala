package controllers

import java.io.{IOException, File}
import java.nio.file.{FileAlreadyExistsException, Paths, Files}
import java.util.jar.JarFile

import models.Project
import models.author.Dev
import org.spongepowered.plugin.meta.McModInfo
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
case class PluginFile(private val file: File) {

  val META_FILE_NAME = "mcmod.info"
  val PLUGIN_FILE_EXTENSION = ".jar"
  val PLUGIN_DIR = "uploads/plugins"

  private var result: Option[Result] = None

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
    * Reads the temporary file's plugin meta file and moves it to the
    * appropriate location.
    *
    * @return Result of parse
    */
  def parse: Option[Project] = {
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

    val meta = metaList.get(0)
    val authors = meta.getAuthors.toList
    if (authors.isEmpty) {
      this.result = Some(BadRequest("No authors found."))
      return None
    }

    val devs = for (author <- authors) yield Dev(author)
    val owner = Dev.get("Spongie").get // TODO: Replace with auth'd user
    // TODO: Verify version unique
    val project = Project(meta.getId, meta.getName, meta.getDescription, owner, devs) // TODO: Store in database
    val output = getUploadPath(owner.name, project.name, meta.getVersion)
    if (!Files.exists(output.getParent)) {
      Files.createDirectories(output.getParent)
    }

    try {
      Files.move(Paths.get(file.getPath), output)
    } catch {
      case e: FileAlreadyExistsException =>
        Files.delete(Paths.get(file.getPath))
        this.result = Some(BadRequest("Version already exists."))
        return None
    }

    Some(project)
  }

  private def getUploadPath(owner: String, name: String, version: String) = {
    Paths.get(Play.application.path.getPath)
      .resolve(PLUGIN_DIR)
      .resolve(owner)
      .resolve(name + "-" + version + PLUGIN_FILE_EXTENSION)
  }

}
