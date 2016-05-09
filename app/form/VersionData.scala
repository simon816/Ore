package form

import db.ModelService
import ore.project.util.ProjectFileManager
import util.OreConfig

/**
  * Represents submitted [[models.project.Version]] data.
  *
  * @param channelName      Name of channel
  * @param channelColorHex  Channel color hex
  * @param recommended      True if recommended version
  */
case class VersionData(override val channelName: String,
                       override protected val channelColorHex: String,
                       recommended: Boolean)
                      (implicit override val service: ModelService,
                       override val config: OreConfig,
                       override val fileManager: ProjectFileManager)
                       extends TChannelData
