package controllers

import ore.api.OreAPI.v1
import play.api.libs.json._
import play.api.mvc._

/**
  * Ore API (v1)
  */
class ApiController extends Controller {

  def ApiResult(json: Option[JsValue]): Result = json.map(Ok(_)).getOrElse(NotFound)

  /**
    * Returns a JSON view of all projects.
    *
    * @param version    API version
    * @return           JSON view of projects
    */
  def listProjects(version: String, categories: Option[String], sort: Option[Int], q: Option[String],
                   limit: Option[Int], offset: Option[Int]) = Action {
    version match {
      case "v1" => Ok(v1.listProjects(categories, sort, q, limit, offset))
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
      case "v1" => ApiResult(v1.showProject(pluginId))
      case yikes => NotFound
    }
  }

  def listVersions(version: String, pluginId: String, channels: Option[String],
                   limit: Option[Int], offset: Option[Int]) = Action {
    version match {
      case "v1" => ApiResult(v1.listVersions(pluginId, channels, limit, offset))
      case gorp => NotFound
    }
  }

  def showVersion(version: String, pluginId: String, channel: String, name: String) = Action {
    version match {
      case "v1" => ApiResult(v1.showVersion(pluginId, channel, name))
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
      case "v1" => Ok(v1.listUsers(limit, offset))
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
      case "v1" => ApiResult(v1.showUser(username))
      case sad => NotFound
    }
  }

}
