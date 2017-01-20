package util

import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.ws.WSResponse

import scala.util.{Failure, Success, Try}

object WSUtils {

  def parseJson(response: WSResponse, log: Logger): Option[JsValue] = {
    if (response.status < 200 || response.status >= 300)
      return None
    Try(response.json) match {
      case Failure(e) =>
        log.warn("Failed to parse response as JSON", e)
        None
      case Success(json) =>
        log.info("Response: " + json)
        Some(json)
    }
  }

}
