package ore

import models.user.User

/** Represents anything that has a [[User]]. */
trait UserOwner {
  /** Returns the User ID */
  def userId: Int
  /** Returns the User */
  def user: User = User.withId(this.userId).get
}
