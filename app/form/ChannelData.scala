package form

import ore.project.util.ProjectFileManager
import util.OreConfig

/**
  * Concrete counterpart to [[TChannelData]].
  *
  * @param channelName     Channel name
  * @param channelColorHex Channel color hex code
  */
case class ChannelData(override val channelName: String,
                       override protected val channelColorHex: String)
                      (implicit override val config: OreConfig,
                       override val fileManager: ProjectFileManager)
                       extends TChannelData
