package util.form

/**
  * Concrete counterpart to [[TChannelData]].
  *
  * @param channelName     Channel name
  * @param channelColorHex Channel color hex code
  */
case class ChannelData(override val channelName: String,
                       override protected val channelColorHex: String)
                       extends TChannelData
