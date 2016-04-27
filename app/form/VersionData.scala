package form

/**
  * Represents submitted [[models.project.Version]] data.
  *
  * @param channelName      Name of channel
  * @param channelColorHex  Channel color hex
  * @param recommended      True if recommended version
  */
case class VersionData(override val channelName: String,
                       override protected val channelColorHex: String,
                       val recommended: Boolean)
                       extends TChannelData
