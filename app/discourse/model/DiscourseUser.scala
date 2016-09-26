package discourse.model

import java.sql.Timestamp

/**
  * Represents a user on Discourse.
  */
case class DiscourseUser(id: Int,
                         username: String,
                         createdAt: Option[Timestamp] = None,
                         email: Option[String] = None,
                         fullName: Option[String] = None,
                         avatarTemplate: Option[String] = None,
                         groups: List[DiscourseGroup] = List())
