package util

import play.api.data.Form
import play.api.data.Forms._

/**
  * Collection of forms used in this application.
  */
object Forms {

  /**
    * Selects a Category for a project.
    */
  lazy val ProjectCategory = Form(single("category" -> text))

  /**
    * Submits a name change for a project.
    */
  lazy val ProjectRename = Form(single("name" -> text))

  /**
    * Submits changes on a documentation page.
    */
  lazy val PageEdit = Form(tuple("name" -> text, "content" -> text))

}
