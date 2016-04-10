package models.user

import play.api.Play.current
import play.api.Play.{configuration => config}

/**
  * Represents a "fake" User object for bypassing the standard authentication
  * method in a development environment.
  */
object FakeUser extends User(config.getInt("application.fakeUser.id").get,
                             config.getString("application.fakeUser.name").get,
                             config.getString("application.fakeUser.username").get,
                             config.getString("application.fakeUser.email").get) {

  /**
    * True if FakeUser should be used.
    */
  val ENABLED: Boolean = config.getBoolean("application.fakeUser.enabled").get

}
