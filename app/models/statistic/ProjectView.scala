package models.statistic

import java.sql.Timestamp

import com.github.tminglei.slickpg.InetString
import com.google.common.base.Preconditions._
import controllers.Requests.ProjectRequest
import db.impl.ProjectViewsTable
import db.impl.access.UserBase
import models.project.Project
import ore.StatTracker._
import ore.permission.scope.ProjectScope

/**
  * Represents a unique view on a Project.
  *
  * @param id         Unique ID of entry
  * @param createdAt  Timestamp instant of creation
  * @param modelId    ID of model the stat is on
  * @param address    Client address
  * @param cookie     Browser cookie
  * @param userId     User ID
  */
case class ProjectView(override val id: Option[Int] = None,
                       override val createdAt: Option[Timestamp] = None,
                       override val modelId: Int,
                       override val address: InetString,
                       override val cookie: String,
                       private var userId: Option[Int] = None)
                       extends StatEntry[Project](id, createdAt, modelId, address, cookie, userId)
                         with ProjectScope {

  override type M = ProjectView
  override type T = ProjectViewsTable

  override def projectId: Int = this.modelId
  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(id = id, createdAt = theTime)

}

object ProjectView {

  /**
    * Creates a new ProjectView to be (or not be) recorded from an incoming
    * request.
    *
    * @param request  Request to bind
    * @return         New ProjectView
    */
  def bindFromRequest()(implicit request: ProjectRequest[_], users: UserBase): ProjectView = {
    checkNotNull(request, "null request", "")
    checkNotNull(users, "null user base", "")
    val userId = users.current.flatMap(_.id)
    val view = ProjectView(
      modelId = request.project.id.get,
      address = InetString(remoteAddress),
      cookie = getStatCookie,
      userId = userId
    )
    view.userBase = users
    view
  }

}
