package ore.rest

import javax.inject.Inject

import db.ModelService
import db.impl.access.{ProjectBase, UserBase}
import db.impl.pg.OrePostgresDriver.api._
import db.impl.action.{ProjectActions, UserActions, VersionActions}
import db.impl.service.{ProjectBase, UserBase}
import ore.project.Categories.Category
import ore.project.{Categories, ProjectSortingStrategies}
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}
import util.OreConfig
import util.StringUtils.equalsIgnoreCase

/**
  * The Ore API
  */
trait OreRestfulApi {

  val writes: OreWrites
  import writes._

  val service: ModelService
  val config: OreConfig
  val users: UserBase = this.service.access(classOf[UserBase])

  implicit val projects: ProjectBase = this.service.access(classOf[ProjectBase])

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
    val queries = this.service.getActions(classOf[ProjectActions])
    val categoryArray: Array[Category] = categories.map(Categories.fromString).orNull
    val ordering = sort.map(ProjectSortingStrategies.withId(_).get).getOrElse(ProjectSortingStrategies.Default)
    val filter = q.map(queries.searchFilter).orNull
    val maxLoad = this.config.projects.getInt("init-load").get
    val lim = Math.max(limit.getOrElse(maxLoad), maxLoad)
    val future = queries.collect(filter, categoryArray, lim, offset.getOrElse(-1), ordering)
    val projects = this.service.await(future).get
    toJson(projects)
  }

  /**
    * Returns a Json value of the Project with the specified ID.
    *
    * @param pluginId Project plugin ID
    * @return Json value of project if found, None otherwise
    */
  def getProject(pluginId: String): Option[JsValue] = this.projects.withPluginId(pluginId).map(toJson(_))

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
    this.projects.withPluginId(pluginId).map { project =>
      // Map channel names to IDs
      val channelIds: Option[Seq[Int]] = channels.map(_.toLowerCase.split(',').map { name =>
        project.channels.find(equalsIgnoreCase(_.name, name)).get.id.get
      })

      // Only allow versions in the specified channels
      val filter = channelIds.map(service.getActions(classOf[VersionActions]).channelFilter).orNull
      val maxLoad = this.config.projects.getInt("init-version-load").get
      val lim = Math.max(limit.getOrElse(maxLoad), maxLoad)

      toJson(project.versions.sorted(_.createdAt.desc, filter, lim, offset.getOrElse(-1)))
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
    this.projects.withPluginId(pluginId)
      .flatMap(_.versions.find(equalsIgnoreCase(_.versionString, name)))
      .map(toJson(_))
  }

  /**
    * Returns a Json value of Users.
    *
    * @param limit  Amount to take
    * @param offset Amount to drop
    * @return       List of users
    */
  def getUserList(limit: Option[Int], offset: Option[Int]): JsValue = toJson {
    this.service.await(service.getActions(classOf[UserActions]).collect(
      limit = limit.getOrElse(-1),
      offset = offset.getOrElse(-1))
    ).get
  }

  /**
    * Returns a Json value of the User with the specified username.
    *
    * @param username Username of User
    * @return         JSON user if found, None otherwise
    */
  def getUser(username: String): Option[JsValue] = this.users.withName(username).map(toJson(_))

  /**
    * Returns the Sponge "statusz" endpoint for Ore.
    *
    * @return statusz json
    */
  def getStatusZ: JsValue = Json.obj(
    "BUILD_NUMBER"  ->  sys.env("BUILD_NUMBER"),
    "GIT_BRANCH"    ->  sys.env("GIT_BRANCH"),
    "GIT_COMMIT"    ->  sys.env("GIT_COMMIT"),
    "JOB_NAME"      ->  sys.env("JOB_NAME"),
    "BUILD_TAG"     ->  sys.env("BUILD_TAG"),
    "SPONGE_ENV"    ->  sys.env("SPONGE_ENV"),
    "SERVICE"       ->  "Ore"
  )

}

class OreRestfulServer @Inject()(override val writes: OreWrites,
                                 override val service: ModelService,
                                 override val config: OreConfig)
                           extends OreRestfulApi
