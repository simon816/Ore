package security.sso

import java.util.concurrent.TimeoutException
import javax.inject.Inject

import ore.OreConfig
import org.spongepowered.play.util.WSUtils.parseJson
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

trait SpongeAuthApi {

  val url: String
  val apiKey: String
  val ws: WSClient
  val timeout: Duration = 10.seconds

  val Logger = play.api.Logger("SpongeAuth")

  implicit private val spongeUserReads: Reads[SpongeUser] = (
    (JsPath \ "id").read[Int] and
    (JsPath \ "username").read[String] and
    (JsPath \ "email").read[String]
  )(SpongeUser.apply _)

  def createUser(username: String,
                 email: String,
                 password: String,
                 verified: Boolean = false): Either[String, SpongeUser]
  = doCreateUser(username, email, password, verified, dummy = false)

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

  def getUser(username: String): Option[SpongeUser] = {
    val url = route("/api/users/" + username) + s"?apiKey=$apiKey"
    readUser(this.ws.url(url).get()).right.toOption
  }

  def deleteUser(username: String): Either[String, SpongeUser] = {
    val url = route("/api/users") + s"?username=$username&apiKey=$apiKey"
    readUser(this.ws.url(url).delete())
  }

  def readUser(response: Future[WSResponse], nullable: Boolean = false): Either[String, SpongeUser] = {
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
