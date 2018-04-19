package form.project

import models.project.{Channel, Project}
import ore.Colors.Color
import ore.OreConfig
import ore.project.factory.ProjectFactory
import scala.concurrent.{ExecutionContext, Future}

import util.functional.{EitherT, OptionT}
import util.instances.future._

/**
  * Represents submitted [[Channel]] data.
  */
trait TChannelData {

  val config: OreConfig
  val factory: ProjectFactory

  /** The [[Channel]] [[Color]] **/
  val color: Color = Channel.Colors.find(_.hex.equalsIgnoreCase(channelColorHex)).get

  /** Channel name **/
  def channelName: String

  /** Channel color hex **/
  protected def channelColorHex: String

  val nonReviewed: Boolean

  /**
    * Attempts to add this ChannelData as a [[Channel]] to the specified
    * [[Project]].
    *
    * @param project  Project to add Channel to
    * @return         Either the new channel or an error message
    */
  def addTo(project: Project)(implicit ec: ExecutionContext): EitherT[Future, String, Channel] = {
    EitherT.liftF(project.channels.all)
      .filterOrElse(_.size >= config.projects.get[Int]("max-channels"), "A project may only have up to five channels.")
      .filterOrElse(_.exists(_.name.equalsIgnoreCase(this.channelName)), "A channel with that name already exists.")
      .filterOrElse(_.exists(_.color == this.color), "A channel with that color already exists.")
      .semiFlatMap(_ => this.factory.createChannel(project, this.channelName, this.color, this.nonReviewed))
  }

  /**
    * Attempts to save this ChannelData to the specified [[Channel]] name in
    * the specified [[Project]].
    *
    * @param oldName  Channel name to save to
    * @param project  Project of channel
    * @return         Error, if any
    */
  def saveTo(oldName: String)(implicit project: Project, ec: ExecutionContext): OptionT[Future, String] = {
    OptionT(
      project.channels.all.map { channels =>
        val channel = channels.find(_.name.equalsIgnoreCase(oldName)).get
        val colorChan = channels.find(_.color.equals(this.color))
        val colorTaken = colorChan.exists(_ != channel)
        if (colorTaken) {
          Some("A channel with that color already exists.")
        } else {
          val nameChan = channels.find(_.name.equalsIgnoreCase(this.channelName))
          val nameTaken = nameChan.exists(_ != channel)
          if (nameTaken) {
            Some("A channel with that name already exists.")
          } else {
            val reviewedChannels = channels.filter(!_.isNonReviewed)
            if (this.nonReviewed && reviewedChannels.size <= 1 && reviewedChannels.contains(channel)) {
              Some("There must be at least one reviewed channel.")
            } else {
              channel.setName(this.channelName)
              channel.setColor(this.color)
              channel.setNonReviewed(this.nonReviewed)
              None
            }
          }
        }
      }
    )
  }

}
