package discourse.model

import java.sql.Timestamp

/**
  * Represents a post on Discourse.
  */
case class DiscoursePost(postId: Int,
                         topicId: Int,
                         userId: Int,
                         username: String,
                         topicSlug: String,
                         createdAt: Timestamp,
                         lastUpdated: Timestamp,
                         deletedAt: Option[Timestamp],
                         cookedContent: String,
                         replyCount: Int,
                         postNum: Int) {

  /** True if this post is a topic. */
  val isTopic: Boolean = this.postNum == 1

  /** True if this post is deleted. */
  val isDeleted: Boolean = this.deletedAt.isDefined

}
