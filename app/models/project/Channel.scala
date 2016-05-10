package models.project

import java.sql.Timestamp

import com.google.common.base.Preconditions._
import db.action.ModelActions
import db.impl.ModelKeys._
import db.impl.{ChannelTable, ModelKeys, OreModel, VersionTable}
import db.meta.{Actor, Bind, HasMany}
import ore.Colors._
import ore.permission.scope.ProjectScope
import ore.project.util.ProjectFileManager
import org.apache.commons.io.FileUtils
import org.spongepowered.plugin.meta.version.ComparableVersion
import org.spongepowered.plugin.meta.version.ComparableVersion.{ListItem, StringItem}
import util.OreConfig

import scala.annotation.meta.field

/**
  * Represents a release channel for Project Versions. Each project gets it's
  * own set of channels.
  *
  * @param id           Unique identifier
  * @param createdAt    Instant of creation
  * @param _name        Name of channel
  * @param _color       Color used to represent this Channel
  * @param projectId    ID of project this channel belongs to
  */
@Actor(classOf[ModelActions[ChannelTable, Channel]])
@HasMany(Array(classOf[Version]))
case class Channel(override val id: Option[Int] = None,
                   override val createdAt: Option[Timestamp] = None,
                   override val projectId: Int,
                   @(Bind @field) private var _name: String,
                   @(Bind @field) private var _color: Color)
                   extends OreModel(id, createdAt)
                     with Ordered[Channel]
                     with ProjectScope { self =>

  import models.project.Channel._

  def this(name: String, color: Color, projectId: Int) = this(_name=name, _color=color, projectId=projectId)

  /**
    * Returns the name of this Channel.
    *
    * @return Name of channel
    */
  def name: String = this._name

  /**
    * Sets the name of this channel and performs any additional actions
    * necessary.
    *
    * @param context  Project for context
    * @param _name    New channel name
    */
  def name_=(_name: String)(implicit context: Project, fileManager: ProjectFileManager) = Defined {
    checkArgument(context.id.get == this.projectId, "invalid context id", "")
    checkArgument(isValidName(name), "invalid name", "")
    fileManager.renameChannel(context.ownerName, context.name, this._name, name)
    this._name = name
    update(Name)
  }

  /**
    * Returns the [[Color]] that this Channel is represented by.
    *
    * @return Color channel is represented by
    */
  def color: Color = this._color

  /**
    * Sets the color of this channel.
    *
    * @param _color Color of channel
    */
  def color_=(_color: Color) = Defined {
    this._color = _color
    update(ModelKeys.Color)
  }

  /**
    * Returns all Versions in this channel.
    *
    * @return All versions
    */
  def versions = this.getRelated[VersionTable, Version](classOf[Version])

  /**
    * Irreversibly deletes this channel and all version associated with it.
    *
    * @param context  Project context
    */
  def delete()(implicit context: Project = null, fileManager: ProjectFileManager) = Defined {
    val proj = if (context != null) context else this.project
    checkArgument(proj.id.get == this.projectId, "invalid proj id", "")

    val channels = proj.channels.all
    checkArgument(channels.size > 1, "only one channel", "")
    checkArgument(this.versions.isEmpty || channels.count(c => c.versions.nonEmpty) > 1, "last non-empty channel", "")

    this.remove()
    FileUtils.deleteDirectory(fileManager.projectDir(proj.ownerName, proj.name).resolve(this._name).toFile)
  }

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Channel = this.copy(id = id, createdAt = theTime)
  override def compare(that: Channel): Int = this._name compare that._name
  override def hashCode: Int = this.id.get.hashCode
  override def equals(o: Any): Boolean = o.isInstanceOf[Channel] && o.asInstanceOf[Channel].id.get == this.id.get

}

object Channel {

  /**
    * The colors a Channel is allowed to have.
    */
  val Colors: Seq[Color] = Seq(Purple, Violet, Magenta, Blue, Aqua, Cyan, Green,
                               DarkGreen, Chartreuse, Amber, Orange, Red)

  /**
    * The default color used for Channels.
    */
  def DefaultColor(implicit config: OreConfig): Color = Colors(config.channels.getInt("color-default").get)

  /**
    * The default name used for Channels.
    */
  def DefaultName(implicit config: OreConfig): String = config.channels.getString("name-default").get

  /**
    * Returns true if the specified string is a valid channel name.
    *
    * @param name   Name to check
    * @return       True if valid channel name
    */
  def isValidName(name: String)(implicit config: OreConfig): Boolean = {
    val c = config.channels
    name.length >= 1 && name.length <= c.getInt("max-name-len").get && name.matches(c.getString("name-regex").get)
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
  def getSuggestedNameForVersion(version: String)(implicit config: OreConfig): String
  = firstString(new ComparableVersion(version).getItems).getOrElse(DefaultName)

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
