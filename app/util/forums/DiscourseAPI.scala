package util.forums

import db.query.Queries
import ore.permission.role.RoleTypes
import ore.permission.role.RoleTypes.RoleType
import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Handles retrieval of groups on a discourse forum.
  *
  * @param url Discourse URL
  */
class DiscourseAPI(private val url: String, ws: WSClient) {

  /**
    * Returns a set of UserRoles that the specified User has on the Sponge
    * forums.
    *
    * @param username   User to get roles for
    * @return           Set of roles the user has
    */
  def roles(username: String): Future[Set[RoleType]] = {
    ws.url(userUrl(username)).get.map { response =>
      val obj = response.json.as[JsObject]
      if (isSuccess(obj)) {
        val groups = (response.json \ "user" \ "groups").as[List[JsObject]]
        (for (group <- groups) yield {
          val id = (group \ "id").as[Int]
          RoleTypes.values.find(_.roleId == id)
        }).flatten.map(_.asInstanceOf[RoleType]).toSet
      } else {
        Set()
      }
    }
  }

  def avatarUrl(username: String, size: Int): String = {
    Queries.now(ws.url(userUrl(username)).get.map { response =>
      val obj = response.json.as[JsObject]
      if (isSuccess(obj)) {
        val template = (obj \ "user" \ "avatar_template").as[String]
        this.url + template.replace("{size}", size.toString)
      } else {
        ""
      }
    }).get
  }

  def isSuccess(json: JsObject): Boolean = !json.keys.contains("errors")

  private def userUrl(username: String): String = {
    url + "/users/" + username + ".json"
  }

}

object DiscourseAPI {

  object Disabled extends DiscourseAPI("", null) {
    override def roles(username: String) = Future(Set())
    override def avatarUrl(username: String, size: Int) = ""
  }

}
