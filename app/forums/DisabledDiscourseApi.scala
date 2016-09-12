package forums

import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Represents the DiscourseApi in a disabled state.
  */
class DisabledDiscourseApi extends DiscourseApi {
  override val url = null
  override val key: String = null
  override val admin: String = null
  override val embed = DiscourseEmbeddingService.Disabled
  override protected val ws: WSClient = null
  override def fetchUser(username: String) = Future(None)
  override def fetchAvatarUrl(username: String, size: Int) = Future(None)
}
