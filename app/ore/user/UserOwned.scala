package ore.user

import db.impl.access.UserBase
import models.user.User

/** Represents anything that has a [[User]]. */
trait UserOwned {
  /** Returns the User ID */
  def userId: Int
  /** Returns the User */
  def user(implicit users: UserBase): User = users.get(this.userId).get
}
