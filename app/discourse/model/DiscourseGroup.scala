package discourse.model

/**
  * Represents a Discourse user group.
  */
case class DiscourseGroup(id: Int,
                          name: String,
                          automatic: Boolean,
                          userCount: Int,
                          visible: Boolean)
