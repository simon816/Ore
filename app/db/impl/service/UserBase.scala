package db.impl.service

import java.sql.Timestamp

import db.impl.UserTable
import db.impl.action.ProjectActions
import db.impl.service.UserBase.{ORDER_JOIN_DATE, ORDER_PROJECTS, ORDER_USERNAME}
import db.{ModelBase, ModelService}
import forums.DiscourseApi
import models.user.User
import play.api.mvc.Session
import util.OreConfig
import util.StringUtils._

/**
  * Represents a central location for all Users.
  */
class UserBase(override val service: ModelService,
               forums: DiscourseApi,
               config: OreConfig)
               extends ModelBase[UserTable, User] {

  override val modelClass = classOf[User]

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
      this.service.await(forums.fetchUser(username)).get.map(getOrCreate)
    }
  }

  /**
    * Returns all [[User]]s with at least one [[models.project.Project]].
    *
    * @return Users with at least one project
    */
  def authors(ordering: String = ORDER_PROJECTS, page: Int = 1): Seq[User] = {
    // determine ordering
    var sort = ordering
    val reverse = if (sort.startsWith("-")) {
      sort = sort.substring(1)
      false
    } else true

    // get authors
    var users: Seq[User] = this.service.await {
      this.service.getActions(classOf[ProjectActions]).distinctAuthors
    }.get

    // sort
    ordering match {
      case ORDER_PROJECTS => users = users.sortBy(u => (u.projects.size, u.username))
      case ORDER_JOIN_DATE => users = users.sortBy(u => (u.joinDate.getOrElse(u.createdAt.get), u.username))
      case ORDER_USERNAME => users = users.sortBy(_.username)
    }

    // get page slice
    val pageSize = this.config.users.getInt("author-page-size").get
    val offset = (page - 1) * pageSize
    users = users.slice(offset, offset + pageSize)
    if (reverse) users.reverse else users
  }

  /**
    * Attempts to find the specified User in the database or creates a new User
    * if one does not exist.
    *
    * @param user User to find
    * @return     Found or new User
    */
  def getOrCreate(user: User): User = this.service.await(user.actions(service).getOrInsert(user)).get

  /**
    * Returns the currently authenticated User.
    *
    * @param session  Current session
    * @return         Authenticated user, if any, None otherwise
    */
  def current(implicit session: Session): Option[User] = session.get("username").map(withName).getOrElse(None)

  implicit def ordered: Ordering[Timestamp] = new Ordering[Timestamp] {
    def compare(x: Timestamp, y: Timestamp) = x compareTo y
  }

}

object UserBase {

  val ORDER_PROJECTS = "projects"
  val ORDER_USERNAME = "username"
  val ORDER_JOIN_DATE = "join_date"

}
