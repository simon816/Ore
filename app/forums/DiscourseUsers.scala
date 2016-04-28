package forums

import java.sql.Timestamp
import java.text.SimpleDateFormat

import db.query.Queries.await
import forums.SpongeForums.validate
import models.user.User
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
class DiscourseUsers(url: String, ws: WSClient) {

  val DateFormat = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSS'Z'")

  /**
    * Attempts to retrieve the user with the specified username from the forums
    * and creates them if they exist.
    *
    * @param username Username to find
    * @return         New user or None
    */
  def fetch(username: String): Future[Option[User]] = {
    ws.url(userUrl(username)).get.map { response =>
      validate(response) { json =>
        val userObj = (json \ "user").as[JsObject]
        val user = new User(
            (userObj \ "id").as[Int], (userObj \ "name").asOpt[String].orNull,
            (userObj \ "username").as[String], (userObj \ "email").asOpt[String].orNull,
            (userObj \ "created_at").asOpt[String].map(jd => new Timestamp(DateFormat.parse(jd).getTime)).orNull)
        val globalRoles = parseRoles(userObj)
        user.globalRoleTypes = globalRoles
        user
      }
    }
  }

  /**
    * Returns a set of UserRoles that the specified User has on the Sponge
    * forums.
    *
    * @param username   User to get roles for
    * @return           Set of roles the user has
    */
  def fetchRoles(username: String): Future[Set[RoleType]] = {
    ws.url(userUrl(username)).get.map { response =>
      validate(response) { json =>
        parseRoles((json \ "user").as[JsObject])
      } getOrElse {
        Set()
      }
    }
  }

  /**
    * Returns the URL to the specified user's avatar image.
    *
    * @param username Username to get avatar URL for
    * @param size     Size of avatar
    * @return         Avatar URL
    */
  def avatarUrl(username: String, size: Int): String = {
    await(ws.url(userUrl(username)).get.map { response =>
      validate(response) { json =>
        val template = (json \ "user" \ "avatar_template").as[String]
        this.url + template.replace("{size}", size.toString)
      } getOrElse {
        ""
      }
    }).get
  }

  private def parseRoles(userObj: JsObject): Set[RoleType] = {
    val groups = (userObj \ "groups").as[List[JsObject]]
    (for (group <- groups) yield {
      val id = (group \ "id").as[Int]
      RoleTypes.values.find(_.roleId == id)
    }).flatten.map(_.asInstanceOf[RoleType]).toSet
  }

  private def userUrl(username: String): String = {
    url + "/users/" + username + ".json"
  }

}

object DiscourseUsers {

  /**
    * Represents a DiscourseAPI object in a disabled state.
    */
  object Disabled extends DiscourseUsers("", null) {
    override def fetch(username: String) = Future(None)
    override def fetchRoles(username: String) = Future(Set())
    override def avatarUrl(username: String, size: Int) = ""
  }

}
