package models.project

import java.sql.Timestamp

import com.google.common.base.Preconditions._
import db.impl.ModelKeys._
import db.impl.{OreModel, PageTable}
import db.impl.action.PageActions
import db.meta.{Actor, Bind}
import forums.DiscourseApi
import ore.permission.scope.ProjectScope
import org.pegdown.Extensions._
import org.pegdown.PegDownProcessor
import play.twirl.api.Html
import util.OreConfig
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
  * @param _contents    Markdown contents
  * @param isDeletable  True if can be deleted by the user
  */
@Actor(classOf[PageActions])
case class Page(override val id: Option[Int] = None,
                override val createdAt: Option[Timestamp] = None,
                override val projectId: Int,
                name: String,
                slug: String,
                isDeletable: Boolean = true,
                @(Bind @field) private var _contents: String)
                extends OreModel(id, createdAt)
                  with ProjectScope { self =>

  override type M = Page
  override type T = PageTable
  override type A = PageActions

  import models.project.Page._

  checkNotNull(this.name, "name cannot be null", "")
  checkNotNull(this._contents, "contents cannot be null", "")

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
    * Sets the Markdown contents of this Page and updates the associated forum
    * topic if this is the home page.
    *
    * @param _contents Markdown contents
    */
  def contents_=(_contents: String) = {
    checkArgument(_contents.length <= MaxLength, "contents too long", "")
    checkArgument(_contents.length >= MinLength, "contents not long enough", "")
    this._contents = _contents
    if (isDefined) {
      val project = this.project
      if (this.name.equals(HomeName) && project.topicId.isDefined) this.forums.embed.updateTopic(project)
      update(Contents)
    }
  }

  /**
    * Returns the HTML representation of this Page.
    *
    * @return HTML representation
    */
  def html: Html = Html(MarkdownProcessor.markdownToHtml(contents))

  /**
    * Returns true if this is the home page.
    *
    * @return True if home page
    */
  def isHome: Boolean = this.name.equals(HomeName)

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Page = this.copy(id = id, createdAt = theTime)

}

object Page {

  /**
    * The Markdown processor.
    */
  val MarkdownProcessor: PegDownProcessor = new PegDownProcessor(ALL & ~ANCHORLINKS)

  /**
    * The name of each Project's homepage.
    */
  def HomeName(implicit config: OreConfig): String = config.pages.getString("home.name").get

  /**
    * The template body for the Home page.
    */
  def HomeMessage(implicit config: OreConfig): String = config.pages.getString("home.message").get

  /**
    * The minimum amount of characters a page may have.
    */
  def MinLength(implicit config: OreConfig): Int = config.pages.getInt("min-len").get

  /**
    * The maximum amount of characters a page may have.
    */
  def MaxLength(implicit config: OreConfig): Int = config.pages.getInt("max-len").get

  /**
    * Returns a template for new Pages.
    *
    * @param title  Page title
    * @param body   Default message
    * @return       Template
    */
  def Template(title: String, body: String = ""): String = "# " + title + "\n" + body

}
