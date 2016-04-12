package models.project

import java.sql.Timestamp

import db.Model
import db.query.Queries
import models.project.Page._
import org.pegdown.Extensions._
import org.pegdown.PegDownProcessor
import play.api.Play.current
import play.api.Play.{configuration => config}
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
                val           projectId: Int,
                val           name: String,
                val           slug: String,
                private var   _contents: String,
                val           isDeletable: Boolean = true)
                extends       Model {

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
    Queries.now(Queries.Pages.setString(this, _.contents, _contents)).get
    this._contents = _contents
  }

  /**
    * Returns the HTML representation of this Page.
    *
    * @return HTML representation
    */
  def html: String = MD.markdownToHtml(contents)

}

object Page {

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
