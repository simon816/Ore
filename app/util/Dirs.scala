package util

import play.api.Play
import play.api.Play.current

/**
  * Helper class for getting commonly used Paths.
  */
object Dirs {

  lazy val Root     =   Play.application.path.toPath
  lazy val Uploads  =   Root.resolve("uploads")
  lazy val Plugins  =   Uploads.resolve("plugins")
  lazy val Tmp      =   Uploads.resolve("tmp")

}
