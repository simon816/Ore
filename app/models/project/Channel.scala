package models.project

import java.nio.file.Files
import java.sql.Timestamp

import com.google.common.base.Preconditions._
import db.OrePostgresDriver.api._
import db.orm.dao.{ModelDAO, ModelSet}
import db.orm.model.ModelKeys._
import db.orm.model.{Model, ModelKeys}
import db.query.Queries
import db.query.Queries.await
import db.{ChannelTable, VersionTable}
import ore.Colors._
import ore.permission.scope.ProjectScope
import ore.project.util.ProjectFiles
import org.apache.commons.io.FileUtils
import org.spongepowered.plugin.meta.version.ComparableVersion
import org.spongepowered.plugin.meta.version.ComparableVersion.{ListItem, StringItem}
import util.C._

/**
  * Represents a release channel for Project Versions. Each project gets it's
  * own set of channels.
  *
  * TODO: Max channels per-project
  *
  * @param id           Unique identifier
  * @param createdAt    Instant of creation
  * @param _name        Name of channel
  * @param _color       Color used to represent this Channel
  * @param projectId    ID of project this channel belongs to
  */
case class Channel(override val   id: Option[Int] = None,
                   override val   createdAt: Option[Timestamp] = None,
                   private var    _name: String,
                   private var    _color: Color,
                   override val   projectId: Int)
                   extends        Model
                   with           Ordered[Channel]
                   with           ProjectScope { self =>

  import models.project.Channel._

  override type M <: Channel { type M = self.M }

  def this(name: String, color: Color, projectId: Int) = this(_name=name, _color=color, projectId=projectId)

  /**
    * Returns the name of this Channel.
    *
    * @return Name of channel
    */
  def name: String = this._name

  /**
    * Sets the name of this channel for.
    *
    * @param context  Project for context
    * @param _name     New channel name
    * @return         Future result
    */
  def name_=(_name: String)(implicit context: Project) = assertDefined {
    checkArgument(context.id.get == this.projectId, "invalid context id", "")
    checkArgument(isValidName(name), "invalid name", "")
    ProjectFiles.renameChannel(context.ownerName, context.name, this._name, name)
    this._name = name
    update(Name)
  }

  /**
    * Returns the ChannelColor that this Channel is represented by.
    *
    * @return Color channel is represented by
    */
  def color: Color = this._color

  /**
    * Sets the color of this channel.
    *
    * @param _color  Color of channel
    * @return       Future result
    */
  def color_=(_color: Color) = assertDefined {
    this._color = _color
    update(ModelKeys.Color)
  }

  /**
    * Returns all Versions in this channel.
    *
    * @return All versions
    */
  def versions: ModelSet[ChannelTable, Channel, VersionTable, Version] = assertDefined {
    Queries.Channels.getVersions(this)
  }

  /**
    * Deletes the specified Version within this channel.
    *
    * @param version  Version to delete
    * @param context  Project for context
    * @return         Result
    */
  def deleteVersion(version: Version)(implicit context: Project) = assertDefined {
    checkArgument(context.versions.size > 1, "only one version", "")
    checkArgument(context.id.get == this.projectId, "invalid context id", "")
    val rv = context.recommendedVersion
    await(Queries.Versions delete version).get
    // Set recommended version to latest version if the deleted version was the rv
    if (version.equals(rv)) context.recommendedVersion = context.versions.sorted(_.createdAt.desc, limit = 1).head
    Files.delete(ProjectFiles.uploadPath(context.ownerName, context.name, version.versionString, this._name))
  }

  /**
    * Irreversibly deletes this channel and all version associated with it.
    *
    * @param context  Project context
    * @return         Result
    */
  def delete(context: Project) = assertDefined {
    checkArgument(context.id.get == this.projectId, "invalid context id", "")

    val channels = context.channels.values
    checkArgument(channels.size > 1, "only one channel", "")
    checkArgument(this.versions.isEmpty || channels.count(c => c.versions.nonEmpty) > 1, "last non-empty channel", "")

    await(Queries.Channels delete this).get
    FileUtils.deleteDirectory(ProjectFiles.projectDir(context.ownerName, context.name).resolve(this._name).toFile)
  }

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Channel = this.copy(id = id, createdAt = theTime)

  override def compare(that: Channel): Int = this._name compare that._name

  override def hashCode: Int = this.id.get.hashCode

  override def equals(o: Any): Boolean = {
    o.isInstanceOf[Channel] && o.asInstanceOf[Channel].id.get == this.id.get
  }

  // Table bindings

  bind[String](Name, _._name, name => Seq(Queries.Channels.setString(this, _.name, name)))
  bind[Color](ModelKeys.Color, _._color, color => Seq(Queries.Channels.setColor(this, color)))

}

object Channel extends ModelDAO[Channel] {

  /**
    * The colors a Channel is allowed to have.
    */
  val Colors: Seq[Color] = Seq(Purple, Violet, Magenta, Blue, Aqua, Cyan, Green,
                               DarkGreen, Chartreuse, Amber, Orange, Red)

  /**
    * The maximum name size of a Channel.
    */
  val MaxNameLength = ChannelsConf.getInt("max-name-len").get

  /**
    * Regular expression for permitted Channel characters.
    */
  val NameRegex = ChannelsConf.getString("name-regex").get

  /**
    * The default color used for Channels.
    */
  val DefaultColor: Color = Colors(ChannelsConf.getInt("color-default").get)

  /**
    * The default name used for Channels.
    */
  val DefaultName: String = ChannelsConf.getString("name-default").get

  /**
    * Returns true if the specified string is a valid channel name.
    *
    * @param name   Name to check
    * @return       True if valid channel name
    */
  def isValidName(name: String): Boolean = {
    name.length >= 1 && name.length <= MaxNameLength && name.matches(NameRegex)
  }

  /**
    * Attempts to determine a Channel name from the specified version string.
    * This is attained using a ComparableVersion and finding the first
    * StringItem within the parsed version. (e.g. 1.0.0-alpha) would return
    * "alpha".
    *
    * @param version  Version string to parse
    * @return         Suggested channel name
    */
  def getSuggestedNameForVersion(version: String): String = {
    firstString(new ComparableVersion(version).getItems).getOrElse(DefaultName)
  }

  override def withId(id: Int): Option[Channel] = await(Queries.Channels.get(id)).get

  private def firstString(items: ListItem): Option[String] = {
    // Get the first non-number component in the version string
    var str: Option[String] = None
    var i = 0
    while (str.isEmpty && i < items.size()) {
      items.get(i) match {
        case item: StringItem => str = Some(item.getValue)
        case item: ListItem => str = firstString(item)
        case _ => ;
      }
      i += 1
    }
    str
  }

}
