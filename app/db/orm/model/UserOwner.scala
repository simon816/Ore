package db.orm.model

import models.user.User

trait UserOwner {
  def userId: Int
  def user: User = User.withId(this.userId).get
}
