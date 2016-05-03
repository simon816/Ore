package models.user

import java.sql.Timestamp
import java.util.Date

import ore.permission.role.RoleTypes
import util.Conf._

/**
  * Represents a "fake" User object for bypassing the standard authentication
  * method in a development environment.
  */
object FakeUser extends User(id           =   AppConf.getInt("fakeUser.id"),
                             _name        =   AppConf.getString("fakeUser.name"),
                             _username    =   AppConf.getString("fakeUser.username").get,
                             _email       =   AppConf.getString("fakeUser.email"),
                             _joinDate    =   Some(new Timestamp(new Date().getTime)),
                             _globalRoles =   List(RoleTypes.Admin)) {

  /**
    * True if FakeUser should be used.
    */
  val IsEnabled: Boolean = AppConf.getBoolean("fakeUser.enabled").get

}
