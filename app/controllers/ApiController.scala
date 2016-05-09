package controllers

import javax.inject.Inject

import db.ModelService
import forums.DiscourseApi
import ore.api.OreRestfulApi
import play.api.libs.json._
import play.api.mvc._

/**
  * Ore API (v1)
  */
class ApiController @Inject()(val api: OreRestfulApi,
                              implicit val models: ModelService,
                              implicit val forums: DiscourseApi) extends Controller {

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
      case "v1" => Ok(api.getProjectList(categories, sort, q, limit, offset))
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
      case "v1" => ApiResult(api.getProject(pluginId))
      case yikes => NotFound
    }
  }

  def listVersions(version: String, pluginId: String, channels: Option[String],
                   limit: Option[Int], offset: Option[Int]) = Action {
    version match {
      case "v1" => ApiResult(api.getVersionList(pluginId, channels, limit, offset))
      case gorp => NotFound
    }
  }

  def showVersion(version: String, pluginId: String, name: String) = Action {
    version match {
      case "v1" => ApiResult(api.getVersion(pluginId, name))
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
      case "v1" => Ok(api.getUserList(limit, offset))
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
      case "v1" => ApiResult(api.getUser(username))
      case sad => NotFound
    }
  }

}
