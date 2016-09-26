package discourse

import java.sql.Timestamp
import java.text.SimpleDateFormat

import discourse.model.{DiscourseGroup, DiscoursePost, DiscourseUser}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * A trait to mixin for implicit [[Reads]] for the Discourse models.
  */
trait DiscourseReads {

  val DateFormat = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSS'Z'")

  implicit val timestampReads: Reads[Timestamp] = new Reads[Timestamp] {
    def reads(json: JsValue): JsResult[Timestamp] = JsSuccess(new Timestamp(DateFormat.parse(json.as[String]).getTime))
  }

  implicit val groupReads: Reads[DiscourseGroup] = (
    (JsPath \ "id").read[Int] and
      (JsPath \ "name").read[String] and
      (JsPath \ "automatic").read[Boolean] and
      (JsPath \ "user_count").read[Int] and
      (JsPath \ "visible").read[Boolean]
    )(DiscourseGroup.apply _)

  implicit val userReads: Reads[DiscourseUser] = (
    (JsPath \ "id").read[Int] and
      (JsPath \ "username").read[String] and
      (JsPath \ "created_at").readNullable[Timestamp] and
      (JsPath \ "email").readNullable[String] and
      (JsPath \ "name").readNullable[String] and
      (JsPath \ "avatar_template").readNullable[String] and
      (JsPath \ "groups").read[List[DiscourseGroup]]
    )(DiscourseUser.apply _)

  implicit val postReads: Reads[DiscoursePost] = (
    (JsPath \ "id").read[Int] and
      (JsPath \ "topic_id").read[Int] and
      (JsPath \ "user_id").read[Int] and
      (JsPath \ "username").read[String] and
      (JsPath \ "topic_slug").read[String] and
      (JsPath \ "created_at").read[Timestamp] and
      (JsPath \ "updated_at").read[Timestamp] and
      (JsPath \ "cooked").read[String] and
      (JsPath \ "reply_count").read[Int] and
      (JsPath \ "post_number").read[Int]
    )(DiscoursePost.apply _)

}
