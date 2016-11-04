package controllers

import javax.inject.Inject

import ore.rest.OreRestfulApi
import org.spongepowered.play.StatusZ
import play.api.libs.json._
import play.api.mvc._

/**
  * Ore API (v1)
  */
final class ApiController @Inject()(api: OreRestfulApi, status: StatusZ) extends Controller {

  private def ApiResult(json: Option[JsValue]): Result = json.map(Ok(_)).getOrElse(NotFound)

  /**
    * Returns a JSON view of all projects.
    *
    * @param version    API version
    * @return           JSON view of projects
    */
  def listProjects(version: String, categories: Option[String], sort: Option[Int], q: Option[String],
                   limit: Option[Int], offset: Option[Int]) = Action {
    version match {
      case "v1" => Ok(this.api.getProjectList(categories, sort, q, limit, offset))
      case zoinks => NotFound
    }
  }

  /**
    * Returns a JSON view of a Project.
    *
    * @param version    API version
    * @param pluginId   Plugin ID of project
    * @return           Project with Plugin ID
    */
  def showProject(version: String, pluginId: String) = Action {
    version match {
      case "v1" => ApiResult(this.api.getProject(pluginId))
      case yikes => NotFound
    }
  }

  /**
    * Returns a JSON view of Versions meeting the specified criteria.
    *
    * @param version  API version string
    * @param pluginId Project plugin ID
    * @param channels Channels to get versions from
    * @param limit    Amount to take
    * @param offset   Amount to drop
    * @return         List of versions
    */
  def listVersions(version: String, pluginId: String, channels: Option[String],
                   limit: Option[Int], offset: Option[Int]) = Action {
    version match {
      case "v1" => ApiResult(this.api.getVersionList(pluginId, channels, limit, offset))
      case gorp => NotFound
    }
  }

  /**
    * Shows the specified Project Version.
    *
    * @param version  API version
    * @param pluginId Project plugin ID
    * @param name     Version name
    * @return         JSON view of Version
    */
  def showVersion(version: String, pluginId: String, name: String) = Action {
    version match {
      case "v1" => ApiResult(this.api.getVersion(pluginId, name))
      case fffffs => NotFound
    }
  }

  /**
    * Returns a JSON view of Ore Users.
    *
    * @param version  API version
    * @param limit    Amount of users to get
    * @param offset   Offset to drop
    * @return         List of users
    */
  def listUsers(version: String, limit: Option[Int], offset: Option[Int]) = Action {
    version match {
      case "v1" => Ok(this.api.getUserList(limit, offset))
      case oops => NotFound
    }
  }

  /**
    * Returns a JSON view of the specified User.
    *
    * @param version    API version
    * @param username   Username of user
    * @return           User with username
    */
  def showUser(version: String, username: String) = Action {
    version match {
      case "v1" => ApiResult(this.api.getUser(username))
      case sad => NotFound
    }
  }

  /**
    * Returns a JSON statusz endpoint for Ore.
    *
    * @return statusz json
    */
  def showStatusZ = Action(Ok(this.status.json))

}
