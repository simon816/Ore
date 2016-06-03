package ore.project.util

import java.nio.file.Files._
import java.nio.file.Path

import util.OreEnv

import scala.util.Try

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
