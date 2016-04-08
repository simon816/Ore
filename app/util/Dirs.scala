package util

import play.api.Play
import play.api.Play.current

/**
  * Helper class for getting commonly used Paths.
  */
object Dirs {

  lazy val ROOT_DIR      =   Play.application.path.toPath
  lazy val CONF_DIR      =   ROOT_DIR.resolve("conf")
  lazy val MD_DIR        =   CONF_DIR.resolve("markdown")
  lazy val UPLOADS_DIR   =   ROOT_DIR.resolve("uploads")
  lazy val DOCS_DIR      =   UPLOADS_DIR.resolve("docs")
  lazy val PLUGIN_DIR    =   UPLOADS_DIR.resolve("plugins")
  lazy val TEMP_DIR      =   UPLOADS_DIR.resolve("tmp")

}
