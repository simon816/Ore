package controllers

import java.io.{IOException, File}
import java.nio.file.{Paths, Files}
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

  /**
    * Returns the actual file associated with this plugin.
    *
    * @return File of plugin
    */
  def getFile = file

  /**
    * Reads the temporary file's plugin meta file and moves it to the
    * appropriate location.
    *
    * @return Result of parse
    */
  def parse: Result = {
    var jar : JarFile = null
    try {
      jar = new JarFile(this.file)
    } catch {
      case ioe: IOException => return BadRequest("Error reading plugin file.")
    }
    val metaEntry = jar.getEntry(META_FILE_NAME)
    if (metaEntry == null) {
      return BadRequest("No plugin meta file found.")
    }
    val metaList = McModInfo.DEFAULT.read(jar.getInputStream(metaEntry))
    if (metaList.size() > 1) {
      return BadRequest("Multiple plugins found.")
    }
    val meta = metaList.get(0)
    val authors = meta.getAuthors.toList
    if (authors.isEmpty) {
      return BadRequest("No authors found.")
    }

    val devs = for (author <- authors) yield Dev(author)
    val owner = Dev.get("Spongie").get // TODO: Replace with auth'd user
    // TODO: Verify version unique
    val project = Project(meta.getId, meta.getName, meta.getDescription, owner, devs) // TODO: Store in database
    val output = getUploadPath(owner.name, project.name, meta.getVersion)
    if (!Files.exists(output.getParent)) {
      Files.createDirectories(output.getParent)
    }
    Files.move(Paths.get(file.getPath), output)
    Redirect(routes.Projects.show(owner.name, project.name))
  }

  private def getUploadPath(owner: String, name: String, version: String) = {
    Paths.get(Play.application.path.getPath)
      .resolve(PLUGIN_DIR)
      .resolve(owner)
      .resolve(name + "-" + version + PLUGIN_FILE_EXTENSION)
  }

}
