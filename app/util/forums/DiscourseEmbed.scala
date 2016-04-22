package util.forums

import models.user.User
import play.api.libs.ws.WSClient
import util.forums.SpongeForums.validate

import scala.concurrent.Future

class DiscourseEmbed(url: String, apiKey: String, categoryId: Int, ws: WSClient) {

  def createTopicAs(user: User, title: String, content: String) = {
    val params = Map(
      "title" -> title,
      "raw" -> content,
      "api_key" -> this.apiKey,
      "api_username" -> user.username
    )

    ws.url(url + "/posts").post(params).map { response =>
      validate(response) { json =>
        val topicId = (json \ "topic_id").as[Int]
        val update = Map("topic_id" -> topicId, "category_id" -> this.categoryId)
        ws.url(url + "/t/" + topicId).put(update)
      }
    }
  }

}

object DiscourseEmbed {
  object Disabled extends DiscourseEmbed(null, null, -1, null) {
    override def createTopicAs(user: User, title: String, content: String) = Future(None)
  }
}
