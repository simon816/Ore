package util

import play.api.data.Form
import play.api.data.Forms._

/**
  * Collection of forms used in this application.
  */
object Forms {

  /**
    * Continue on from the "project create" page to the first "version create"
    * page.
    */
  lazy val ProjectCreateContinue = Form(single("category" -> text))

  /**
    * Submits changes on a documentation page.
    */
  lazy val PageEdit = Form(tuple("name" -> text, "content" -> text))

  /**
    * Submits a name change for a project.
    */
  lazy val ProjectRename = Form(single("name" -> text))

}
