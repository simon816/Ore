package models.project

import java.sql.Timestamp

import db.orm.dao.ModelDAO
import db.orm.model.NamedModel
import db.query.Queries
import ore.permission.scope.ProjectScope
import org.pegdown.Extensions._
import org.pegdown.PegDownProcessor
import play.api.Play.{configuration => config, current}
import util.Input._

/**
  * Represents a documentation page within a project.
  *
  * @param id           Page ID
  * @param createdAt    Timestamp of creation
  * @param projectId    Project ID
  * @param name         Page name
  * @param slug         Page URL slug
  * @param _contents     Markdown contents
  * @param isDeletable  True if can be deleted by the user
  */
case class Page(override val  id: Option[Int] = None,
                override val  createdAt: Option[Timestamp] = None,
                override val  projectId: Int,
                override val  name: String,
                val           slug: String,
                private var   _contents: String,
                val           isDeletable: Boolean = true)
                extends       NamedModel
                with          ProjectScope {

  import models.project.Page._

  def this(projectId: Int, name: String, content: String, isDeletable: Boolean) = {
    this(projectId=projectId, name=compact(name),
         slug=slugify(name), _contents=content, isDeletable=isDeletable)
  }

  /**
    * Returns the Markdown contents of this Page.
    *
    * @return Markdown contents
    */
  def contents: String = this._contents

  /**
    * Sets the Markdown contents of this Page.
    *
    * @param _contents Markdown contents
    */
  def contents_=(_contents: String) = {
    if (isDefined) Queries.now(Queries.Pages.setString(this, _.contents, _contents)).get
    this._contents = _contents
  }

  /**
    * Returns the HTML representation of this Page.
    *
    * @return HTML representation
    */
  def html: String = MD.markdownToHtml(contents)

}

object Page extends ModelDAO[Page] {

  /**
    * The name of each Project's homepage.
    */
  val HomeName: String = config.getString("ore.pages.home.name").get

  /**
    * The template body for the Home page.
    */
  val HomeMessage: String = config.getString("ore.pages.home.message").get

  /**
    * The Markdown processor.
    */
  val MD: PegDownProcessor = new PegDownProcessor(ALL & ~ANCHORLINKS)

  override def withId(id: Int): Option[Page] = Queries.now(Queries.Pages.get(id)).get

  /**
    * Returns a template for new Pages.
    *
    * @param title  Page title
    * @param body   Default message
    * @return       Template
    */
  def template(title: String, body: String = ""): String = {
    "# " + title + "\n" + body
  }

}
