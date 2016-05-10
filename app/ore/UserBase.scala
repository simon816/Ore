package ore

import javax.inject.Inject

import db.{ModelAccessible, ModelService}
import db.action.ModelAccess
import db.impl.UserTable
import forums.DiscourseApi
import models.user.User
import play.api.mvc.Session
import util.StringUtils._

/**
  * Represents a central location for all Users.
  */
trait UserBase extends ModelAccessible[UserTable, User] {

  implicit val service: ModelService
  implicit val forums: DiscourseApi

  /**
    * Returns the user with the specified username. If the specified username
    * is not found in the database, an attempt will be made to fetch the user
    * from Discourse.
    *
    * @param username Username of user
    * @return User if found, None otherwise
    */
  def withName(username: String): Option[User] = {
    this.find(equalsIgnoreCase(_.username, username)).orElse {
      service.await(forums.fetchUser(username)).get.map(getOrCreate)
    }
  }

  /**
    * Attempts to find the specified User in the database or creates a new User
    * if one does not exist.
    *
    * @param user User to find
    * @return     Found or new User
    */
  def getOrCreate(user: User): User = service.await(user.actions.getOrInsert(user)).get

  /**
    * Returns the currently authenticated User.
    *
    * @param session  Current session
    * @return         Authenticated user, if any, None otherwise
    */
  def current(implicit session: Session): Option[User] = session.get("username").map(withName).getOrElse(None)

}

class OreUserBase @Inject()(override val service: ModelService,
                            override val forums: DiscourseApi,
                            override val modelClass: Class[User] = classOf[User]) extends UserBase
