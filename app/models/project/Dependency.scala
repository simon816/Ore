package models.project

import db.Storage

import scala.util.{Success, Failure}

case class Dependency(pluginId: String, version: String) {

  def getProject: Option[Project] = Storage.now(Storage.optProject(this.pluginId)) match {
    case Failure(thrown) => throw thrown
    case Success(projectOpt) => projectOpt
  }

}
