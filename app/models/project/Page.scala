package models.project

import java.sql.Timestamp
import java.util.Date

import db.Storage
import models.project.Page._
import org.pegdown.Extensions._
import org.pegdown.PegDownProcessor

/**
  * Represents a documentation page within a project.
  *
  * @param id           Page ID
  * @param _createdAt   Timestamp of creation
  * @param projectId    Project ID
  * @param name         Page name
  * @param slug         Page URL slug
  * @param _contents     Markdown contents
  * @param isDeletable  True if can be deleted by the user
  */
case class Page(id: Option[Int], private var _createdAt: Option[Timestamp],
                projectId: Int, name: String, slug: String,
                private var _contents: String, isDeletable: Boolean) {

  def this(projectId: Int, name: String, content: String, isDeletable: Boolean = true) = {
    this(None, None, projectId, Project.sanitizeName(name), Project.slugify(name), content, isDeletable)
  }

  /**
    * Returns the Timestamp instant that this Page was created.
    *
    * @return Instant of creation
    */
  def createdAt: Option[Timestamp] = this._createdAt

  /**
    * Called when this Page is created.
    */
  def onCreate() = this._createdAt = Some(new Timestamp(new Date().getTime))

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
    Storage.now(Storage.updatePageString(this, _.contents, _contents)).get
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
  val HOME: String = "Home"

  /**
    * The Markdown processor.
    */
  val MD: PegDownProcessor = new PegDownProcessor(ALL & ~ANCHORLINKS)

}
