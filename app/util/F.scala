package util

import play.api.Play
import play.api.Play.current

object F {

  lazy val ROOT_DIR      =   Play.application.path.toPath
  lazy val CONF_DIR      =   ROOT_DIR.resolve("conf")
  lazy val UPLOADS_DIR   =   ROOT_DIR.resolve("uploads")
  lazy val DOCS_DIR      =   UPLOADS_DIR.resolve("docs")
  lazy val PLUGIN_DIR    =   UPLOADS_DIR.resolve("plugins")
  lazy val TEMP_DIR      =   UPLOADS_DIR.resolve("tmp")

}
