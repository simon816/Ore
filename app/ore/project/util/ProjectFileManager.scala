package ore.project.util

import java.nio.file.Files._
import java.nio.file.Path

import models.project.Project
import util.OreEnv
import util.StringUtils.optional2option

import scala.util.Try
import collection.JavaConverters._

/**
  * Handles file management of Projects.
  */
class ProjectFileManager(val env: OreEnv) {

  /**
    * Returns the specified project's plugin directory.
    *
    * @param owner  Owner name
    * @param name   Project name
    * @return       Plugin directory
    */
  def getProjectDir(owner: String, name: String): Path = getUserDir(owner).resolve(name)

  /**
    * Returns the directory that contains a [[Project]]'s custom icon.
    *
    * @param owner Project owner
    * @param name Project name
    * @return Icon directory path
    */
  def getIconDir(owner: String, name: String): Path = getProjectDir(owner, name).resolve("icon")

  /**
    * Returns the path to a custom [[Project]] icon, if any, None otherwise.
    *
    * @param project Project to get icon for
    * @return Project icon
    */
  def getIconPath(project: Project): Option[Path] = findFirstFile(getIconDir(project.ownerName, project.name))

  def getPendingIconDir(owner: String, name: String): Path = getIconDir(owner, name).resolve("pending")

  def getPendingIconPath(project: Project): Option[Path]
  = findFirstFile(getPendingIconDir(project.ownerName, project.name))

  private def findFirstFile(dir: Path): Option[Path] = {
    if (exists(dir))
      list(dir).iterator.asScala.filterNot(isDirectory(_)).toStream.headOption
    else
      None
  }

  /**
    * Returns the specified user's plugin directory.
    *
    * @param user User name
    * @return     Plugin directory
    */
  def getUserDir(user: String): Path = this.env.plugins.resolve(user)

  /**
    * Renames this specified project in the file system.
    *
    * @param owner    Owner name
    * @param oldName  Old project name
    * @param newName  New project name
    * @return         New path
    */
  def renameProject(owner: String, oldName: String, newName: String): Try[Unit] = Try {
    val newProjectDir = getProjectDir(owner, newName)
    move(getProjectDir(owner, oldName), newProjectDir)
    // Rename plugin files
    for (channelDir <- newProjectDir.toFile.listFiles()) {
      if (channelDir.isDirectory) {
        for (pluginFile <- channelDir.listFiles()) {
          move(pluginFile.toPath, getProjectDir(owner, newName).resolve(pluginFile.getName))
        }
      }
    }
  }

}
