package ore.api

import javax.inject.Inject

import db.ModelService
import db.impl.OrePostgresDriver.api._
import db.impl.action.user.UserActions
import db.impl.action.{ProjectActions, VersionActions}
import forums.DiscourseApi
import models.project.Project
import models.user.User
import ore.project.Categories.Category
import ore.project.{Categories, ProjectSortingStrategies}
import play.api.libs.json.{JsValue, Json}
import util.OreConfig
import util.StringUtils.equalsIgnoreCase

/**
  * The Ore API
  */
trait OreRestfulApi {

  import OreWrites._

  implicit val service: ModelService
  implicit val forums: DiscourseApi
  implicit val config: OreConfig

  /**
    * Returns a Json value of the Projects meeting the specified criteria.
    *
    * @param categories Project categories
    * @param sort       Ordering
    * @param q          Query string
    * @param limit      Amount to take
    * @param offset     Amount to drop
    * @return           JSON list of projects
    */
  def getProjectList(categories: Option[String], sort: Option[Int], q: Option[String],
                     limit: Option[Int], offset: Option[Int]): JsValue = {
    val queries = service.provide(classOf[ProjectActions])
    val categoryArray: Array[Category] = categories.map(Categories.fromString).orNull
    val s = sort.map(ProjectSortingStrategies.withId(_).get).getOrElse(ProjectSortingStrategies.Default)
    val filter = q.map(queries.searchFilter).orNull
    val maxLoad = config.projects.getInt("init-load").get
    val lim = Math.max(limit.getOrElse(maxLoad), maxLoad)
    val f = queries.collect(filter, categoryArray, lim, offset.getOrElse(-1), s)
    val projects = service.await(f).get
    Json.toJson(projects)
  }

  /**
    * Returns a Json value of the Project with the specified ID.
    *
    * @param pluginId Project plugin ID
    * @return Json value of project if found, None otherwise
    */
  def getProject(pluginId: String): Option[JsValue]
  = Project.withPluginId(pluginId).map(Json.toJson(_))

  /**
    * Returns a Json value of the Versions meeting the specified criteria.
    *
    * @param pluginId Project plugin ID
    * @param channels Version channels
    * @param limit    Amount to take
    * @param offset   Amount to drop
    * @return         JSON list of versions
    */
  def getVersionList(pluginId: String, channels: Option[String],
                     limit: Option[Int], offset: Option[Int]): Option[JsValue] = {
    Project.withPluginId(pluginId).map { project =>
      // Map channel names to IDs
      val channelIds: Option[Seq[Int]] = channels.map(_.toLowerCase.split(',').map { name =>
        project.channels.find(equalsIgnoreCase(_.name, name)).get.id.get
      })
      // Only allow versions in the specified channels
      val filter = channelIds.map(service.provide(classOf[VersionActions]).channelFilter).orNull
      val maxLoad = config.projects.getInt("init-version-load").get
      val lim = Math.max(limit.getOrElse(maxLoad), maxLoad)
      Json.toJson(project.versions.sorted(_.createdAt.desc, filter, lim, offset.getOrElse(-1)))
    }
  }

  /**
    * Returns a Json value of the specified version.
    *
    * @param pluginId Project plugin ID
    * @param name     Version name
    * @return         JSON version if found, None otherwise
    */
  def getVersion(pluginId: String, name: String): Option[JsValue] = {
    Project.withPluginId(pluginId)
      .flatMap(_.versions.find(equalsIgnoreCase(_.versionString, name)))
      .map(Json.toJson(_))
  }

  /**
    * Returns a Json value of Users.
    *
    * @param limit  Amount to take
    * @param offset Amount to drop
    * @return       List of users
    */
  def getUserList(limit: Option[Int], offset: Option[Int]): JsValue
  = Json.toJson(service.await(service.provide(classOf[UserActions]).collect(limit.getOrElse(-1), offset.getOrElse(-1))).get)

  /**
    * Returns a Json value of the User with the specified username.
    *
    * @param username Username of User
    * @return         JSON user if found, None otherwise
    */
  def getUser(username: String): Option[JsValue]
  = User.withName(username).map(Json.toJson(_))

}

class OreRestful @Inject()(override val service: ModelService,
                           override val forums: DiscourseApi,
                           override val config: OreConfig)
                           extends OreRestfulApi
