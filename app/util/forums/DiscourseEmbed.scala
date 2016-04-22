package util.forums

import models.project.Project
import play.api.libs.ws.WSClient
import util.forums.SpongeForums.validate

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DiscourseEmbed(url: String, apiKey: String, categoryId: Int, ws: WSClient) {

  def createTopic(project: Project) = {
    val username = project.ownerName
    val params = this.keyedRequest(username) + (
      "title" -> Seq(username + " / " + project.name),
      "raw" -> Seq(project.description.getOrElse("No descriptionnnnnnnnnnn")))
    ws.url(url + "/posts").post(params).map { response =>
      validate(response) { json =>
        val topicId = (json \ "topic_id").as[Int]
        val update = this.keyedRequest(username) + (
          "topic_id" -> Seq(topicId.toString),
          "category_id" -> Seq(this.categoryId.toString))
        project.topicId = topicId
        ws.url(url + "/t/" + topicId).put(update)
      }
    }
  }

  def renameTopic(project: Project) = {
    val topicId = project.topicId.get
    val params = this.keyedRequest(project.ownerName) + (
      "topic_id" -> Seq(topicId.toString),
      "title" -> Seq(project.ownerName + " / " + project.name))
    println(params)
    ws.url(url + "/t/" + topicId).put(params).map { response =>
      println(response)
      println(response.json)
    }
  }

  def deleteTopic(project: Project) = {
    val k = "api_key" -> this.apiKey
    val u = "api_username" -> project.ownerName
    ws.url(url + "/t/" + project.topicId.get).withQueryString(k, u).delete()
  }

  private def keyedRequest(username: String) = {
    Map("api_key" -> Seq(this.apiKey), "api_username" -> Seq(username))
  }

}

object DiscourseEmbed {
  object Disabled extends DiscourseEmbed(null, null, -1, null) {
    override def createTopic(project: Project) = Future(None)
    override def deleteTopic(project: Project) = Future(null)
  }
}
