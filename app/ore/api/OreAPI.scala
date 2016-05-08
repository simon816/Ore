package ore.api

import db.ModelService
import db.impl.OrePostgresDriver.api._
import db.impl.query.user.UserActions
import db.impl.query.{ProjectActions, VersionActions}
import models.project.{Project, Version}
import models.user.User
import ore.project.Categories.Category
import ore.project.{Categories, ProjectSortingStrategies}
import play.api.libs.json.{JsValue, Json}
import util.StringUtils.equalsIgnoreCase

/**
  * The Ore API
  */
object OreAPI {

  import OreWrites._

  /** Iteration #1 */
  object v1 {

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
                       limit: Option[Int], offset: Option[Int])(implicit service: ModelService): JsValue = {
      val queries = service.provide[ProjectActions]
      val categoryArray: Array[Category] = categories.map(Categories.fromString).orNull
      val s = sort.map(ProjectSortingStrategies.withId(_).get).getOrElse(ProjectSortingStrategies.Default)
      val filter = q.map(queries.searchFilter).orNull
      val lim = Math.max(limit.getOrElse(Project.InitialLoad), Project.InitialLoad)
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
    def getProject(pluginId: String)(implicit service: ModelService): Option[JsValue]
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
                       limit: Option[Int], offset: Option[Int])(implicit service: ModelService): Option[JsValue] = {
      Project.withPluginId(pluginId).map { project =>
        // Map channel names to IDs
        val channelIds: Option[Seq[Int]] = channels.map(_.toLowerCase.split(',').map { name =>
          project.channels.find(equalsIgnoreCase(_.name, name)).get.id.get
        })
        // Only allow versions in the specified channels
        val filter = channelIds.map(service.provide[VersionActions].channelFilter).orNull
        val lim = Math.max(limit.getOrElse(Version.InitialLoad), Version.InitialLoad)
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
    def getVersion(pluginId: String, name: String)(implicit service: ModelService): Option[JsValue] = {
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
    def getUserList(limit: Option[Int], offset: Option[Int])(implicit service: ModelService): JsValue
    = Json.toJson(service.await(service.provide[UserActions].collect(limit.getOrElse(-1), offset.getOrElse(-1))).get)

    /**
      * Returns a Json value of the User with the specified username.
      *
      * @param username Username of User
      * @return         JSON user if found, None otherwise
      */
    def getUser(username: String)(implicit service: ModelService): Option[JsValue]
    = User.withName(username).map(Json.toJson(_))

  }

}
