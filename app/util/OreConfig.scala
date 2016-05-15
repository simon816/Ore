package util

import javax.inject.Inject

import models.project.Channel
import ore.Colors._
import org.spongepowered.plugin.meta.version.ComparableVersion
import org.spongepowered.plugin.meta.version.ComparableVersion.{ListItem, StringItem}
import play.api.Configuration
import util.StringUtils.{compact, firstString}

/**
  * A helper class for the Ore configuration.
  *
  * @param config Base configuration file
  */
final class OreConfig @Inject()(config: Configuration) {

  // Sub-configs
  lazy val root = config
  lazy val app = config.getConfig("application").get
  lazy val play = config.getConfig("play").get
  lazy val ore = config.getConfig("ore").get
  lazy val channels = ore.getConfig("channels").get
  lazy val pages = ore.getConfig("pages").get
  lazy val projects = ore.getConfig("projects").get
  lazy val users = ore.getConfig("users").get
  lazy val forums = root.getConfig("discourse").get
  lazy val sponge = root.getConfig("sponge").get

  /**
    * The default color used for Channels.
    */
  lazy val defaultChannelColor: Color = Channel.Colors(this.channels.getInt("color-default").get)

  /**
    * The default name used for Channels.
    */
  lazy val defaultChannelName: String = this.channels.getString("name-default").get

  /**
    * Returns true if the specified name is a valid Project name.
    *
    * @param name   Name to check
    * @return       True if valid name
    */
  def isValidProjectName(name: String): Boolean = {
    val sanitized = compact(name)
    sanitized.length >= 1 && sanitized.length <= this.projects.getInt("max-name-len").get
  }

  /**
    * Returns true if the specified string is a valid channel name.
    *
    * @param name   Name to check
    * @return       True if valid channel name
    */
  def isValidChannelName(name: String): Boolean = {
    val c = this.channels
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
  def getSuggestedNameForVersion(version: String): String
  = firstString(new ComparableVersion(version).getItems).getOrElse(this.defaultChannelName)

  lazy val debugLevel = ore.getInt("debug-level").get
  /** Returns true if the application is running in debug mode. */
  def isDebug: Boolean = ore.getBoolean("debug").get
  /** Sends a debug message if in debug mode */
  def debug(msg: Any, level: Int = 1) = if (isDebug && (level == this.debugLevel || level == -1)) println(msg)
  /** Asserts that the application is in debug mode. */
  def checkDebug()
  = if(!isDebug) throw new UnsupportedOperationException("this function is supported in debug mode only")

}
