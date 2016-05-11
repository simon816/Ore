package forums

import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Represents the DiscourseApi in a disabled state.
  */
class DisabledDiscourseApi extends DiscourseApi {
  override val url = null
  override val embed = DiscourseEmbeddingService.Disabled
  override protected val ws: WSClient = null
  override def fetchUser(username: String) = Future(None)
  override def fetchAvatarUrl(username: String, size: Int) = Future(None)
}
