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
  val ProjectCreateContinue = Form(single("category" -> text))

  /**
    * Submits changes on a documentation page.
    */
  val PageEdit = Form(tuple("name" -> text, "content" -> text))

  /**
    * Submits a name change for a project.
    */
  val ProjectRename = Form(single("name" -> text))

}
