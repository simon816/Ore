package util.forums

import ore.permission.role.RoleTypes
import RoleTypes.RoleType
import ore.permission.role.RoleTypes
import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Handles retrieval of groups on a discourse forum.
  *
  * @param url Discourse URL
  */
class DiscourseGroups(private val url: String, ws: WSClient) {

  /**
    * Returns a set of UserRoles that the specified User has on the Sponge
    * forums.
    *
    * @param username   User to get roles for
    * @return           Set of roles the user has
    */
  def roles(username: String): Future[Set[RoleType]] = {
    ws.url(userUrl(username)).get.map { response =>
      val groups = (response.json \ "user" \ "groups").as[List[JsObject]]
      (for (group <- groups) yield {
        val id = (group \ "id").as[Int]
        RoleTypes.values.find(_.roleId == id)
      }).flatten.map(_.asInstanceOf[RoleType]).toSet
    }
  }

  private def userUrl(username: String): String = {
    url + "/users/" + username + ".json"
  }

}
