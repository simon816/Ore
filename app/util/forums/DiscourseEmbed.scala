package util.forums

import models.user.User
import play.api.libs.ws.WSClient
import util.forums.SpongeForums.validate

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DiscourseEmbed(url: String, apiKey: String, categoryId: Int, ws: WSClient) {

  def createTopicAs(user: User, title: String, content: String) = {
    val params = Map(
      "title" -> Seq(title),
      "raw" -> Seq(content),
      "api_key" -> Seq(this.apiKey),
      "api_username" -> Seq(user.username)
    )

    println(params)

    ws.url(url + "/posts").post(params).map { response =>
      println(response)
      validate(response) { json =>
        println(json)
        val topicId = (json \ "topic_id").as[Int]
        println(topicId)
        val update = Map(
          "topic_id" -> Seq(topicId.toString),
          "category_id" -> Seq(this.categoryId.toString),
          "api_key" -> Seq(this.apiKey),
          "api_username" -> Seq(user.username)
        )
        ws.url(url + "/t/" + topicId).put(update).map { response =>
          println(response)
          validate(response){ json =>
            println(json)
          }
        }
      }
    }
  }

}

object DiscourseEmbed {
  object Disabled extends DiscourseEmbed(null, null, -1, null) {
    override def createTopicAs(user: User, title: String, content: String) = Future(None)
  }
}
