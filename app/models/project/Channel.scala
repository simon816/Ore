package models.project

import java.sql.Timestamp

import com.google.common.base.Preconditions._
import db.action.ModelActions
import db.impl.ModelKeys._
import db.impl.{ChannelTable, ModelKeys, OreModel, VersionTable}
import db.meta.{Actions, Bind, HasMany}
import ore.Colors._
import ore.permission.scope.ProjectScope

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
@Actions(classOf[ModelActions[ChannelTable, Channel]])
@HasMany(Array(classOf[Version]))
case class Channel(override val id: Option[Int] = None,
                   override val createdAt: Option[Timestamp] = None,
                   override val projectId: Int,
                   @(Bind @field) private var _name: String,
                   @(Bind @field) private var _color: Color)
                   extends OreModel(id, createdAt)
                     with Ordered[Channel]
                     with ProjectScope {

  def this(name: String, color: Color, projectId: Int) = this(_name=name, _color=color, projectId=projectId)

  /**
    * Returns the name of this Channel.
    *
    * @return Name of channel
    */
  def name: String = this._name

  /**
    * Sets the name of this channel.
    *
    * @param _name    New channel name
    */
  def name_=(_name: String)(implicit project: Project) = Defined {
    checkArgument(project.id.get == this.projectId, "invalid context id", "")
    checkArgument(this.config.isValidChannelName(_name), "invalid name", "")
    this._name = _name
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
  def versions = this.oneToMany[VersionTable, Version](classOf[Version])

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(id = id, createdAt = theTime)
  override def compare(that: Channel) = this._name compare that._name
  override def hashCode() = this.id.get.hashCode
  override def equals(o: Any) = o.isInstanceOf[Channel] && o.asInstanceOf[Channel].id.get == this.id.get

}

object Channel {

  /**
    * The colors a Channel is allowed to have.
    */
  val Colors: Seq[Color] = Seq(Purple, Violet, Magenta, Blue, Aqua, Cyan, Green,
                               DarkGreen, Chartreuse, Amber, Orange, Red)

}
