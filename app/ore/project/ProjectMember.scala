package ore.project

import com.google.common.base.{MoreObjects, Objects}
import db.impl.access.UserBase
import db.impl.pg.OrePostgresDriver.api._
import models.project.Project
import models.user.role.ProjectRole
import ore.permission.scope.Scope
import ore.user.Member

/**
  * Represents a member of a [[Project]].
  *
  * @param project  Project this Member is a part of
  * @param userId   Member user ID
  */
class ProjectMember(val project: Project, override val userId: Int)(implicit users: UserBase)
                    extends Member[ProjectRole](userId) {

  override def roles: Set[ProjectRole] = this.project.memberships.getRoles(this.user)
  override val scope: Scope = this.project.scope
  override def toString: String = MoreObjects.toStringHelper(this).add("name", this.fullName).toString
  override def hashCode: Int = Objects.hashCode(this.project, this.fullName)

  override def equals(o: Any): Boolean = {
    o match {
      case that: ProjectMember => that.project.equals(this.project) && that.user.equals(this.user)
      case _ => false
    }
  }

}

object ProjectMember {

  implicit def ordering[A <: ProjectMember] = Member.ordering[A, ProjectRole]

}
