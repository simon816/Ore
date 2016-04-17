package util.forums

import db.query.Queries
import db.query.Queries.now
import models.user.User
import ore.permission.role.RoleTypes
import ore.permission.role.RoleTypes.RoleType
import play.api.libs.json.JsObject
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Handles retrieval of groups on a discourse forum.
  *
  * @param url Discourse URL
  */
class DiscourseAPI(private val url: String, ws: WSClient) {

  /**
    * Attempts to retrieve the user with the specified username from the forums
    * and creates them if they exist.
    *
    * @param username Username to find
    * @return         New user or None
    */
  def fetchUser(username: String): Future[Option[User]] = {
    ws.url(userUrl(username)).get.map { response =>
      validate(response) { json =>
        val userObj = (json \ "user").as[JsObject]
        var user = new User((userObj \ "id").as[Int], (userObj \ "name").asOpt[String].orNull,
                            (userObj \ "username").as[String], (userObj \ "email").asOpt[String].orNull)
        user = now(Queries.Users.getOrCreate(user)).get
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
    now(ws.url(userUrl(username)).get.map { response =>
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

  private def validate[A](response: WSResponse)(f: JsObject => A): Option[A] = try {
    val json = response.json.as[JsObject]
    if (!json.keys.contains("errors")) {
      Some(f(json))
    } else {
      None
    }
  } catch {
    case e: Exception => None
  }

  private def userUrl(username: String): String = {
    url + "/users/" + username + ".json"
  }

}

object DiscourseAPI {

  /**
    * Represents a DiscourseAPI object in a disabled state.
    */
  object Disabled extends DiscourseAPI("", null) {
    override def fetchUser(username: String) = Future(None)
    override def fetchRoles(username: String) = Future(Set())
    override def avatarUrl(username: String, size: Int) = ""
  }

}
