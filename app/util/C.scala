package util

import play.api.Play.{current, configuration => config}

object C {

  lazy val RootConf = config
  lazy val AppConf = config.getConfig("application").get
  lazy val PlayConf = config.getConfig("play").get
  lazy val OreConf = config.getConfig("ore").get
  lazy val ChannelsConf = OreConf.getConfig("channels").get
  lazy val PagesConf = OreConf.getConfig("pages").get
  lazy val ProjectsConf = OreConf.getConfig("projects").get
  lazy val UsersConf = OreConf.getConfig("users").get
  lazy val GitConf = OreConf.getConfig("git").get
  lazy val DiscourseConf = RootConf.getConfig("discourse").get
  lazy val SpongeConf = RootConf.getConfig("sponge").get

  lazy val level = OreConf.getInt("debug-level").get
  /** Returns true if the application is running in debug mode. */
  def isDebug: Boolean = OreConf.getBoolean("debug").get
  /** Sends a debug message if in debug mode */
  def debug(msg: Any, level: Int = 1) = if (isDebug && (level == this.level || level == -1)) println(msg)
  /** Asserts that the application is in debug mode. */
  def checkDebug()
  = if(!isDebug) throw new UnsupportedOperationException("this function is supported in debug mode only")

}
