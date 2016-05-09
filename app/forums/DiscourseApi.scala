package forums

import play.api.libs.json.JsObject
import play.api.libs.ws.WSResponse

trait DiscourseApi {

  val Auth: DiscourseSso
  val Users: DiscourseUsers
  val Embed: DiscourseEmbed

  protected[forums] def validate[A](response: WSResponse)(f: JsObject => A): Option[A] = try {
    val json = response.json.as[JsObject]
    if (!json.keys.contains("errors")) {
      Some(f(json))
    } else {
      None
    }
  } catch {
    case e: Exception => None
  }

}
