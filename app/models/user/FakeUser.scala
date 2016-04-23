package models.user

import util.C._

/**
  * Represents a "fake" User object for bypassing the standard authentication
  * method in a development environment.
  */
object FakeUser extends User(AppConf.getInt("fakeUser.id").get,
                             AppConf.getString("fakeUser.name").get,
                             AppConf.getString("fakeUser.username").get,
                             AppConf.getString("fakeUser.email").get) {

  /**
    * True if FakeUser should be used.
    */
  val IsEnabled: Boolean = AppConf.getBoolean("fakeUser.enabled").get

}
