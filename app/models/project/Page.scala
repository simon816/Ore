package models.project

import java.sql.Timestamp

import db.orm.dao.ModelDAO
import db.orm.model.ModelKeys._
import db.orm.model.{ModelKeys, NamedModel}
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
                with          ProjectScope { self =>

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
    this._contents = _contents
    if (isDefined) update(Contents)
  }

  /**
    * Returns the HTML representation of this Page.
    *
    * @return HTML representation
    */
  def html: String = MarkdownProcessor.markdownToHtml(contents)

  // Table bindings

  override type M <: Page { type M = self.M }

  bind[String](Contents, _._contents, contents => Seq(Queries.Pages.setString(this, _.contents, contents)))

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
  val MarkdownProcessor: PegDownProcessor = new PegDownProcessor(ALL & ~ANCHORLINKS)

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
