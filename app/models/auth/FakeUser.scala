package models.auth

import java.sql.Timestamp
import java.util.Date

import play.api.Play

/**
  * Represents a "fake" User object for bypassing the standard authentication
  * method in a development environment.
  */
object FakeUser extends User(-1, new Timestamp(new Date().getTime), "Spongie", "Spongie", "spongie@spongepowered.org") {

  /**
    * True if FakeUser should be used.
    */
  val ENABLED: Boolean = Play.current.configuration.getBoolean("application.fakeUser").get

}
