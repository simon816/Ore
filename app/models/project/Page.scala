package models.project

import java.sql.Timestamp

import com.google.common.base.Preconditions._
import db.PageTable
import db.dao.ModelSet
import db.model.Model
import db.model.ModelKeys._
import db.model.annotation.{Bind, BindingsGenerator}
import forums.SpongeForums
import ore.permission.scope.ProjectScope
import org.pegdown.Extensions._
import org.pegdown.PegDownProcessor
import play.twirl.api.Html
import util.C._
import util.StringUtils._

import scala.annotation.meta.field

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
                              name: String,
                              slug: String,
                              isDeletable: Boolean = true,
                @(Bind @field) private var _contents: String)
                extends Model(id, createdAt) with ProjectScope { self =>

  import models.project.Page._

  override type M <: Page { type M = self.M }

  BindingsGenerator.generateFor(this)

  checkNotNull(this.name, "name cannot be null", "")
  checkNotNull(this._contents, "contents cannot be null", "")

  checkArgument(this.name.length <= MaxNameLength, "name too long", "")
  checkArgument(_contents.length <= MaxLength, "contents too long", "")
  checkArgument(_contents.length >= MinLength, "contents not long enough", "")

  def this(projectId: Int, name: String, content: String, isDeletable: Boolean) = {
    this(projectId=projectId, name=compact(name),
         slug=slugify(name), _contents=content.trim, isDeletable=isDeletable)
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
    checkArgument(_contents.length <= MaxLength, "contents too long", "")
    checkArgument(_contents.length >= MinLength, "contents not long enough", "")
    this._contents = _contents
    if (isDefined) {
      val project = this.project
      if (this.name.equals(HomeName) && project.topicId.isDefined) SpongeForums.Embed.updateTopic(project)
      update(Contents)
    }
  }

  /**
    * Returns the HTML representation of this Page.
    *
    * @return HTML representation
    */
  def html: Html = Html(MarkdownProcessor.markdownToHtml(contents))

  def isHome: Boolean = this.name.equals(HomeName)

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Page = this.copy(id = id, createdAt = theTime)

}

object Page extends ModelSet[PageTable, Page](classOf[Page]) {

  /**
    * The name of each Project's homepage.
    */
  val HomeName: String = PagesConf.getString("home.name").get

  /**
    * The template body for the Home page.
    */
  val HomeMessage: String = PagesConf.getString("home.message").get

  /**
    * The Markdown processor.
    */
  val MarkdownProcessor: PegDownProcessor = new PegDownProcessor(ALL & ~ANCHORLINKS)

  /**
    * The minimum amount of characters a page may have.
    */
  val MinLength: Int = PagesConf.getInt("min-len").get

  /**
    * The maximum amount of characters a page may have.
    */
  val MaxLength: Int = PagesConf.getInt("max-len").get

  /**
    * The maximum amount of characters a page name may have.
    */
  val MaxNameLength: Int = PagesConf.getInt("name.max-len").get

  /**
    * Returns a template for new Pages.
    *
    * @param title  Page title
    * @param body   Default message
    * @return       Template
    */
  def Template(title: String, body: String = ""): String = "# " + title + "\n" + body

}
