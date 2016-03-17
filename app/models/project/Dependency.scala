package models.project

import db.Storage

import scala.util.{Failure, Success}

/**
  * Represents a dependency to another plugin. Either on or not on Ore.
  *
  * @param pluginId   Unique plugin ID
  * @param version    Version of dependency
  */
case class Dependency(pluginId: String, version: String) {

  /**
    * Tries to resolve this dependency as a Project and returns the result.
    *
    * @return Project if dependency is on Ore, empty otherwise.
    */
  def getProject: Option[Project] = Storage.now(Storage.optProject(this.pluginId)) match {
    case Failure(thrown) => throw thrown
    case Success(projectOpt) => projectOpt
  }

}
