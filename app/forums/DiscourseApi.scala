package forums

import java.io.File
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

import akka.stream.scaladsl.{FileIO, Source}
import models.user.User
import ore.permission.role.RoleTypes
import ore.permission.role.RoleTypes.RoleType
import play.api.Logger
import play.api.libs.json.JsObject
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.MultipartFormData.{DataPart, FilePart}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * A DiscourseApi that depends on the OreModelService.
  */
trait DiscourseApi {

  /** Handles Discourse post embedding */
  val embed: DiscourseEmbeddingService

  /** The date format for incoming responses */
  val DateFormat = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSS'Z'")

  /** The base URL */
  val url: String

  /** The super secret API key */
  val key: String

  /** The username of an administrator */
  val admin: String

  /** DiscourseSync instance **/
  val sync: DiscourseSync

  private val logger: Logger = Logger("Discourse")
  protected val ws: WSClient

  /**
    * Returns true if the Discourse instance is available.
    *
    * @return True if available
    */
  def isAvailable: Boolean = {
    await(this.ws.url(this.url).get().map(_ => true).recover {
      case e: Exception => false
    })
  }

  /** Returns a user URL for the specified username. */
  def userUrl(username: String): String = url + "/users/" + username + ".json"

  /**
    * Attempts to retrieve the user with the specified username from the forums
    * and creates them if they exist.
    *
    * @param username Username to find
    * @return         New user or None
    */
  def fetchUser(username: String): Future[Option[User]] = {
    this.ws.url(userUrl(username)).get.map { response =>
      validate(response).right.map { json =>
        val userObj = (json \ "user").as[JsObject]
        val user = User(
          id = (userObj \ "id").asOpt[Int],
          _name = (userObj \ "name").asOpt[String],
          _username = (userObj \ "username").as[String],
          _email = (userObj \ "email").asOpt[String],
          _joinDate = (userObj \ "created_at").asOpt[String]
            .map(jd => new Timestamp(DateFormat.parse(jd).getTime)),
          _avatarUrl = (userObj \ "avatar_template").asOpt[String],
          _globalRoles = parseRoles(userObj).toList)
        user
      }.right.toOption
    }
  }

  /**
    * Returns true if a Discourse user exists with the given username.
    *
    * @param username Username to find
    * @return True if user exists
    */
  def userExists(username: String): Boolean = await(fetchUser(username)).get.isDefined

  /**
    * Creates a new user on the forums with the given parameters.
    *
    * @param name     User's full name
    * @param username User's username
    * @param email    User's email
    * @param password User's password
    * @return         ID of new user if created, None otherwise
    */
  def createUser(name: String, username: String, email: String, password: String): Future[Int] = {
    val data = Map(
      "name" -> Seq(name),
      "username" -> Seq(username),
      "email" -> Seq(email),
      "password" -> Seq(password),
      "active" -> Seq("true")
    )
    this.ws.url(getUrl("/users")).post(data).map(response => validate(response) match {
      case Left(errors) =>
        throw FatalForumErrorException(errors)
      case Right(json) =>
        (json \ "user_id").as[Int]
    })
  }

  /**
    * Adds a group to the specified user.
    *
    * @param userId   User ID
    * @param groupId  ID of group to add
    * @return         True if successful
    */
  def addUserGroup(userId: Int, groupId: Int) = {
    val url = getUrl("/admin/users/" + userId + "/groups")
    val data = Map("group_id" -> Seq(groupId.toString))
    this.ws.url(url).post(data).map(response => validate(response).left.foreach { errors =>
      throw FatalForumErrorException(errors)
    })
  }

  /**
    * Returns the URL to the specified user's avatar image.
    *
    * @param username Username to get avatar URL for
    * @param size     Size of avatar
    * @return         Avatar URL
    */
  def fetchAvatarUrl(username: String, size: Int): Future[Option[String]] = {
    this.ws.url(userUrl(username)).get.map { response =>
      validate(response).right.map { json =>
        val template = (json \ "user" \ "avatar_template").as[String]
        this.url + template.replace("{size}", size.toString)
      }.right.toOption
    }
  }

  /**
    * Sets the avatar for the specified user.
    *
    * @param username   User username
    * @param avatarUrl  Avatar URL
    * @return           List of errors if any
    */
  def setAvatar(username: String, avatarUrl: String): Option[List[String]] = {
    val url = this.url + "/uploads"
    val data = args(username) ++ Map(
      "username" -> Seq(username),
      "url" -> Seq(avatarUrl),
      "type" -> Seq("avatar"),
      "synchronous" -> Seq("true")
    )
    await(this.ws.url(url).post(data).map(response => validate(response) match {
      case Left(errors) =>
        Some(errors)
      case Right(json) =>
        pickAvatar(json, username)
    }))
  }

  /**
    * Sets the avatar for the specified user.
    *
    * @param username User username
    * @param file     Avatar file
    * @return         List of errors if any
    */
  def setAvatar(username: String, fileName: String, file: File): Option[List[String]] = {
    val url = this.url + "/uploads"
    val data = Source(
      DataPart("api_key", this.key)
        :: DataPart("api_username", username)
        :: DataPart("username", username)
        :: FilePart("file", fileName, Some("image/jpeg"), FileIO.fromFile(file))
        :: DataPart("type", "avatar")
        :: DataPart("synchronous", "true")
        :: List())
    await(this.ws.url(url).post(data).map(response => validate(response) match {
      case Left(errors) =>
        Some(errors)
      case Right(json) =>
        pickAvatar(json, username)
    }))
  }

  private def pickAvatar(uploadResponse: JsObject, username: String): Option[List[String]] = {
    val uploadId = (uploadResponse \ "id").as[Int]
    val url = this.url + "/users/" + username + "/preferences/avatar/pick"
    val data = args(username) ++ Map("upload_id" -> Seq(uploadId.toString))
    await(this.ws.url(url).put(data).map(response => validate(response).left.toOption))
  }

  /**
    * Builds a RoleType Set from the specified User JSON object.
    *
    * @param userObj  User JsObject
    * @return         Set of RoleTypes
    */
  def parseRoles(userObj: JsObject): Set[RoleType] = {
    val groups = (userObj \ "groups").as[List[JsObject]]
    (for (group <- groups) yield {
      val id = (group \ "id").as[Int]
      RoleTypes.values.find(_.roleId == id)
    }).flatten.map(_.asInstanceOf[RoleType]).toSet
  }

  /**
    * Validates an incoming Discourse API response.
    *
    * @param response Response to validate
    * @return         Return type
    */
  def validate(response: WSResponse): Either[List[String], JsObject] = {
    val json = response.json.as[JsObject]
    if (json.keys.contains("errors")) {
      val errors = (json \ "errors").asOpt[List[String]].getOrElse((json \ "errors").as[String] :: List())
      errors.foreach(this.logger.warn(_))
      Left(errors)
    } else
      Right(json)
  }

  /**
    * Constructs a new exception for fatal forum responses.
    *
    * @param errors Error list
    * @return       New exception
    */
  def FatalForumErrorException(errors: List[String])
  = new RuntimeException("discourse responded with errors: \n\t- " + errors.mkString("\n\t- "))

  private def await[A](future: Future[A]): A = Await.result(future, Duration(10000, TimeUnit.MILLISECONDS))
  private def args(username: String = this.admin) = Map("api_key" -> Seq(this.key), "api_username" -> Seq(username))
  private def getUrl(url: String, username: String = this.admin)
  = this.url + url + "?api_key=" + this.key + "&api_username=" + username

}
