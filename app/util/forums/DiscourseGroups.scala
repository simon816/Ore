package util.forums

import ore.user.UserRoles
import ore.user.UserRoles.UserRole
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
  def roles(username: String): Future[Set[UserRole]] = {
    ws.url(userUrl(username)).get.map { response =>
      val groups = (response.json \ "user" \ "groups").as[List[JsObject]]
      (for (group <- groups) yield {
        val id = (group \ "id").as[Int]
        UserRoles.values.find(_.externalId == id)
      }).flatten.map(_.asInstanceOf[UserRole]).toSet
    }
  }

  private def userUrl(username: String): String = {
    url + "/users/" + username + ".json"
  }

}
