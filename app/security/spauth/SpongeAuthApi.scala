package security.spauth

import java.util.concurrent.TimeoutException
import javax.inject.Inject

import ore.OreConfig
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import util.WSUtils.parseJson
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Interfaces with the SpongeAuth Web API
  */
trait SpongeAuthApi {

  /** The base URL of the instance */
  val url: String
  /** Secret API key */
  val apiKey: String
  val ws: WSClient
  val timeout: Duration = 10.seconds

  val Logger = play.api.Logger("SpongeAuth")

  implicit private val spongeUserReads: Reads[SpongeUser] = (
    (JsPath \ "id").read[Int] and
    (JsPath \ "username").read[String] and
    (JsPath \ "email").read[String] and
    (JsPath \ "avatar_url").readNullable[String]
  )(SpongeUser.apply _)

  /**
    * Creates a new user with the specified credentials.
    *
    * @param username Username
    * @param email    Email
    * @param password Password
    * @param verified True if should bypass email verification
    * @return         Newly created user
    */
  def createUser(username: String,
                 email: String,
                 password: String,
                 verified: Boolean = false): Either[String, SpongeUser]
  = doCreateUser(username, email, password, verified)

  /**
    * Creates a "dummy" user that cannot log in and has no password.
    *
    * @param username Username
    * @param email    Email
    * @param verified True if should bypass email verification
    * @return         Newly created user
    */
  def createDummyUser(username: String, email: String, verified: Boolean = false): Either[String, SpongeUser]
  = doCreateUser(username, email, null, verified, dummy = true)

  private def doCreateUser(username: String,
                           email: String,
                           password: String,
                           verified: Boolean = false,
                           dummy: Boolean = false): Either[String, SpongeUser] = {
    var params = Map(
      "api-key" -> Seq(this.apiKey),
      "username" -> Seq(username),
      "email" -> Seq(email),
      "verified" -> Seq(verified.toString),
      "dummy" -> Seq(dummy.toString))
    if (password != null)
      params += "password" -> Seq(password)
    readUser(this.ws.url(route("/api/users")).post(params))
  }

  /**
    * Returns the user with the specified username.
    *
    * @param username Username to lookup
    * @return         User with username
    */
  def getUser(username: String): Option[SpongeUser] = {
    val url = route("/api/users/" + username) + s"?apiKey=$apiKey"
    readUser(this.ws.url(url).get()).right.toOption
  }

  /**
    * Deletes the user with the specified username.
    *
    * @param username Username to lookup
    * @return         Deleted user
    */
  def deleteUser(username: String): Either[String, SpongeUser] = {
    val url = route("/api/users") + s"?username=$username&apiKey=$apiKey"
    readUser(this.ws.url(url).delete())
  }

  private def readUser(response: Future[WSResponse], nullable: Boolean = false): Either[String, SpongeUser] = {
    await(response.map(parseJson(_, Logger)).map(_.map { json =>
      val obj = json.as[JsObject]
      if (obj.keys.contains("error"))
        Left((obj \ "error").as[String])
      else
        Right(obj.as[SpongeUser])
    } getOrElse {
      Left("error.spongeauth.parse")
    }) recover {
      case toe: TimeoutException =>
        Left("error.spongeauth.connect")
      case e =>
        Logger.error("An unexpected error occured while handling a response", e)
        Left("error.spongeauth.unexpected")
    })
  }

  private def await[A](future: Future[A]): A = Await.result(future, this.timeout)

  private def route(route: String) = this.url + route

}

final class SpongeAuth @Inject()(config: OreConfig, override val ws: WSClient) extends SpongeAuthApi {

  val conf = this.config.security

  override val url = this.conf.getString("api.url").get
  override val apiKey = this.conf.getString("api.key").get
  override val timeout = this.conf.getLong("api.timeout").get.millis

}
