package form

import ore.project.util.ProjectFileManager
import util.OreConfig

/**
  * Represents submitted [[models.project.Version]] data.
  *
  * @param channelName      Name of channel
  * @param channelColorHex  Channel color hex
  * @param recommended      True if recommended version
  */
case class VersionData(recommended: Boolean,
                       override val channelName: String,
                       override protected val channelColorHex: String)
                      (implicit override val config: OreConfig,
                       override val fileManager: ProjectFileManager)
                       extends TChannelData
