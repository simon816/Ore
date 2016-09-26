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
                         cookedContent: String,
                         replyCount: Int,
                         postNum: Int) {

  /** True if this post is a topic. */
  val isTopic: Boolean = postNum == 1

}
