package ore

import db.ModelService
import models.user.User

/** Represents anything that has a [[User]]. */
trait UserOwner {
  /** Returns the User ID */
  def userId: Int
  /** Returns the User */
  def user(implicit service: ModelService): User = User.withId(this.userId).get
}
