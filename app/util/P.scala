package util

import play.api.Play
import play.api.Play.current

/**
  * Helper class for getting commonly used Paths.
  */
object P {

  lazy val RootDir      =   Play.application.path.toPath
  lazy val UploadsDir   =   RootDir.resolve("uploads")
  lazy val PluginsDir   =   UploadsDir.resolve("plugins")
  lazy val TempDir      =   UploadsDir.resolve("tmp")

}
