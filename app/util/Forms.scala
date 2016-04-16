package util

import play.api.data.Form
import play.api.data.Forms._

/**
  * Collection of forms used in this application.
  */
object Forms {

  /**
    * Submits a new Channel for a Project.
    */
  lazy val ChannelEdit = Form(tuple("channel-input" -> text, "channel-color-input" -> text))

  lazy val MemberRoles = Form(tuple("users" -> list(number), "roles" -> list(text)))

  /**
    * Submits changes on a documentation page.
    */
  lazy val PageEdit = Form(tuple("name" -> text, "content" -> text))

  /**
    * Submits settings changes for a Project.
    */
  lazy val ProjectSave = Form(tuple("category" -> text, "issues" -> text, "source" -> text, "description" -> text))

  /**
    * Submits a name change for a project.
    */
  lazy val ProjectRename = Form(single("name" -> text))

  /**
    * Submits a tagline change for a User.
    */
  lazy val UserTagline = Form(single("tagline" -> text))

  /**
    * Submits a change to a Version's description.
    */
  lazy val VersionDescription = Form(single("description" -> text))

}
