package util

import javax.inject.Inject

import play.api.Configuration

/**
  * A helper class for the Ore configuration.
  *
  * @param config Base configuration file
  */
final class OreConfig @Inject()(config: Configuration) {

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

  lazy val debugLevel = ore.getInt("debug-level").get
  /** Returns true if the application is running in debug mode. */
  def isDebug: Boolean = ore.getBoolean("debug").get
  /** Sends a debug message if in debug mode */
  def debug(msg: Any, level: Int = 1) = if (isDebug && (level == this.debugLevel || level == -1)) println(msg)
  /** Asserts that the application is in debug mode. */
  def checkDebug()
  = if(!isDebug) throw new UnsupportedOperationException("this function is supported in debug mode only")

}
