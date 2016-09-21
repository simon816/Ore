package form.project

import ore.OreConfig

/**
  * Concrete counterpart to [[TChannelData]].
  *
  * @param channelName     Channel name
  * @param channelColorHex Channel color hex code
  */
case class ChannelData(override val channelName: String,
                       override protected val channelColorHex: String)
                      (implicit override val config: OreConfig)
                       extends TChannelData
