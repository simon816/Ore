package models.project

import java.sql.Timestamp

import com.google.common.base.Preconditions._
import db.orm.dao.TModelSet
import db.orm.model.Model
import db.orm.model.ModelKeys._
import db.query.ModelQueries
import forums.SpongeForums
import ore.permission.scope.ProjectScope
import org.pegdown.Extensions._
import org.pegdown.PegDownProcessor
import play.twirl.api.Html
import util.C._
import util.StringUtils._

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
                val           name: String,
                val           slug: String,
                private var   _contents: String,
                val           isDeletable: Boolean = true)
                extends       Model
                with          ProjectScope { self =>

  import models.project.Page._

  override type M <: Page { type M = self.M }

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

  // Table bindings

  bind[String](Contents, _._contents, contents => Seq(ModelQueries.Pages.setString(this, _.contents, contents)))

}

object Page extends TModelSet[Page] {

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

  override def withId(id: Int): Option[Page] = ModelQueries.await(ModelQueries.Pages.get(id)).get

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
