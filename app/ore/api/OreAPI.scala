package ore.api

import db.OrePostgresDriver.api._
import db.query.Queries
import db.query.Queries._
import models.project.{Version, Project}
import models.user.User
import ore.project.{ProjectSortingStrategies, Categories}
import ore.project.Categories.Category
import play.api.libs.json.{JsValue, Json}

object OreAPI {

  import OreWrites._

  object v1 {

    def listProjects(categories: Option[String], sort: Option[Int], q: Option[String],
                     limit: Option[Int], offset: Option[Int]): JsValue = {
      val categoryArray: Array[Category] = categories.map(Categories.fromString).orNull
      val s = sort.map(ProjectSortingStrategies.withId(_).get).getOrElse(ProjectSortingStrategies.Default)
      val filter = q.map(Queries.Projects.searchFilter).orNull
      val lim = Math.max(limit.getOrElse(Project.InitialLoad), Project.InitialLoad)
      val f = Queries.Projects.collect(filter, categoryArray, lim, offset.getOrElse(-1), s)
      val projects = now(f).get
      Json.toJson(projects)
    }

    def showProject(pluginId: String): Option[JsValue] = Project.withPluginId(pluginId).map(Json.toJson(_))

    def listVersions(pluginId: String, channels: Option[String], limit: Option[Int], offset: Option[Int]): Option[JsValue] = {
      Project.withPluginId(pluginId).map { project =>
        // Map channel names to IDs
        val channelIds: Option[Seq[Int]] = channels.map(_.toLowerCase.split(',').map { name =>
          project.channels.find(_.name.toLowerCase === name).get.id.get
        })
        // Only allow versions in the specified channels
        val filter = channelIds.map(Queries.Versions.channelFilter).orNull
        val lim = Math.max(limit.getOrElse(Version.InitialLoad), Version.InitialLoad)
        Json.toJson(project.versions.sorted(_.createdAt.desc, filter, lim, offset.getOrElse(-1)))
      }
    }

    def showVersion(pluginId: String, channel: String, name: String): Option[JsValue] = {
      Project.withPluginId(pluginId)
        .flatMap(_.channels.find(_.name.toLowerCase === name.toLowerCase)
        .flatMap(_.versions.find(_.versionString === name)))
        .map(Json.toJson(_))
    }

    def listUsers(limit: Option[Int], offset: Option[Int]): JsValue
    = Json.toJson(now(Queries.Users.collect(limit.getOrElse(-1), offset.getOrElse(-1))).get)

    def showUser(username: String): Option[JsValue] = User.withName(username).map(Json.toJson(_))

  }

}
