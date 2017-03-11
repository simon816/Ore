package models.project

import java.sql.Timestamp

import com.google.common.base.Preconditions.{checkArgument, checkNotNull}
import db.ModelService
import db.access.ModelAccess
import db.impl.OrePostgresDriver.api._
import db.impl.VersionTable
import db.impl.model.OreModel
import db.impl.model.common.{Describable, Downloadable}
import db.impl.schema.VersionSchema
import db.impl.table.ModelKeys._
import models.statistic.VersionDownload
import ore.Visitable
import ore.permission.scope.ProjectScope
import ore.project.Dependency
import org.apache.commons.io.FileUtils
import play.twirl.api.Html

/**
  * Represents a single version of a Project.
  *
  * @param id               Unique identifier
  * @param createdAt        Instant of creation
  * @param versionString    Version string
  * @param dependencyIds    List of plugin dependencies with the plugin ID and
  *                         version separated by a ':'
  * @param _description     User description of version
  * @param assets           Path to assets directory within plugin
  * @param projectId        ID of project this version belongs to
  * @param channelId        ID of channel this version belongs to
  */
case class Version(override val id: Option[Int] = None,
                   override val createdAt: Option[Timestamp] = None,
                   override val projectId: Int,
                   versionString: String,
                   dependencyIds: List[String] = List(),
                   assets: Option[String] = None,
                   channelId: Int = -1,
                   fileSize: Long,
                   hash: String,
                   private var _description: Option[String] = None,
                   private var _downloads: Int = 0,
                   private var _isReviewed: Boolean = false,
                   fileName: String,
                   signatureFileName: String)
                   extends OreModel(id, createdAt)
                     with ProjectScope
                     with Describable
                     with Downloadable
                     with Visitable {

  override type M = Version
  override type T = VersionTable
  override type S = VersionSchema

  /**
    * Returns the name of this Channel.
    *
    * @return Name of channel
    */
  override def name: String = this.versionString

  /**
    * Returns the channel this version belongs to.
    *
    * @return Channel
    */
  def channel: Channel = this.service.access[Channel](classOf[Channel]).get(this.channelId).get

  /**
    * Returns the channel this version belongs to from the specified collection
    * of channels if present.
    *
    * @param channels   Channels to search
    * @return           Channel if present, None otherwise
    */
  def findChannelFrom(channels: Seq[Channel]): Option[Channel] = Defined {
    if (channels == null)
      return None
    channels.find(_.id.get == this.channelId)
  }

  /**
    * Returns this Version's description.
    *
    * @return Version description
    */
  override def description: Option[String] = this._description

  /**
    * Sets this Version's description.
    *
    * @param _description Version description
    */
  def description_=(_description: String) = {
    checkArgument(_description == null || _description.length <= Page.MaxLength, "content too long", "")
    this._description = Option(_description)
    if (isDefined) update(Description)
  }

  /**
    * Returns this Version's markdown description in HTML.
    *
    * @return Description in html
    */
  def descriptionHtml: Html
  = this.description.map(str => Html(Page.MarkdownProcessor.markdownToHtml(str))).getOrElse(Html(""))

  /**
    * Returns the base URL for this Version.
    *
    * @return Base URL for version
    */
  override def url: String = this.project.url + "/versions/" + this.versionString

  /**
    * Returns true if this version has been reviewed by the moderation staff.
    *
    * @return True if reviewed
    */
  def isReviewed: Boolean = this._isReviewed

  /**
    * Sets whether this version has been reviewed by the moderation staff.
    *
    * @param reviewed True if reviewed
    */
  def setReviewed(reviewed: Boolean) = {
    this._isReviewed = reviewed
    if (isDefined) update(IsReviewed)
  }

  /**
    * Returns this Versions plugin dependencies.
    *
    * @return Plugin dependencies
    */
  def dependencies: List[Dependency] = {
    for (depend <- this.dependencyIds) yield {
      val data = depend.split(":")
      Dependency(data(0), data(1))
    }
  }

  /**
    * Returns true if this version has a dependency on the specified plugin ID.
    *
    * @param pluginId Id to check for
    * @return         True if has dependency on ID
    */
  //noinspection ComparingUnrelatedTypes
  def hasDependency(pluginId: String) = this.dependencies.exists(_.pluginId.equals(pluginId))

  /**
    * Returns the amount of unique downloads this Version has.
    *
    * @return Amount of unique downloads
    */
  override def downloadCount: Int = this._downloads

  /**
    * Adds a download to the amount of unique downloads this Version has.
    */
  def addDownload() = Defined {
    this._downloads += 1
    update(Downloads)
  }

  /**
    * Returns [[ModelAccess]] to the recorded unique downloads.
    *
    * @return Recorded downloads
    */
  def downloadEntries = this.schema.getChildren[VersionDownload](classOf[VersionDownload], this)

  /**
    * Returns a human readable file size for this Version.
    *
    * @return Human readable file size
    */
  def humanFileSize: String = FileUtils.byteCountToDisplaySize(this.fileSize)

  /**
    * Returns true if a project ID is defined on this Model, there is no
    * matching hash in the Project, and there is no duplicate version with
    * the same name in the Project.
    *
    * @return True if exists
    */
  def exists: Boolean = {
    this.projectId != -1 && (this.service.await(this.schema.hashExists(this.projectId, this.hash)).get
      || this.project.versions.exists(_.versionString.toLowerCase === this.versionString.toLowerCase))
  }

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(id = id, createdAt = theTime)
  override def hashCode() = this.id.hashCode
  override def equals(o: Any) = o.isInstanceOf[Version] && o.asInstanceOf[Version].id.get == this.id.get

}

object Version {

  /**
    * A helper class for easily building new Versions.
    *
    * @param service ModelService to process with
    */
  case class Builder(service: ModelService) {

    private var _versionString: String = _
    private var _dependencyIds: List[String] = List()
    private var _description: String = _
    private var _projectId: Int = -1
    private var _fileSize: Long = -1
    private var _hash: String = _
    private var _fileName: String = _
    private var _signatureFileName: String = _

    def versionString(versionString: String) = {
      this._versionString = versionString
      this
    }

    def dependencyIds(dependencyIds: List[String]) = {
      this._dependencyIds = dependencyIds
      this
    }

    def description(description: String) = {
      this._description = description
      this
    }

    def projectId(projectId: Int) = {
      this._projectId = projectId
      this
    }

    def fileSize(fileSize: Long) = {
      this._fileSize = fileSize
      this
    }

    def hash(hash: String) = {
      this._hash = hash
      this
    }

    def fileName(fileName: String) = {
      this._fileName = fileName
      this
    }

    def signatureFileName(signatureFileName: String) = {
      this._signatureFileName = signatureFileName
      this
    }

    def build() = {
      checkArgument(this._fileSize != -1, "invalid file size", "")
      this.service.processor.process(Version(
        versionString = checkNotNull(this._versionString, "name null", ""),
        dependencyIds = this._dependencyIds,
        _description = Option(this._description),
        projectId = this._projectId,
        fileSize = this._fileSize,
        hash = checkNotNull(this._hash, "hash null", ""),
        fileName = checkNotNull(this._fileName, "file name null", ""),
        signatureFileName = checkNotNull(this._signatureFileName, "signature file name null", "")))
    }

  }

}
