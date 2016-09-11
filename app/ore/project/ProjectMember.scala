package ore.project

import com.google.common.base.{MoreObjects, Objects}
import db.impl.OrePostgresDriver.api._
import db.impl.service.UserBase
import models.project.Project
import models.user.{ProjectRole, User}
import ore.permission.scope.{Scope, ScopeSubject}

/**
  * Represents a member of a [[Project]].
  *
  * @param project  Project this Member is a part of
  * @param name     Member name
  */
class ProjectMember(val project: Project, val name: String)(implicit users: UserBase) extends ScopeSubject
                                                                                        with Ordered[ProjectMember] {

  /**
    * Returns the [[User]] this Member belongs to.
    *
    * @return User member belongs to
    */
  def user: User = this.users.withName(this.name).get

  /**
    * Returns the Member's [[ProjectRole]]s in the [[Project]].
    *
    * @return Roles in project
    */
  def roles: Set[ProjectRole] = this.project.roles.filter(_.userId === this.user.id.get).toSet

  /**
    * Returns the Member's highest ranking [[ProjectRole]] in the [[Project]].
    *
    * @return Highest ranking role
    */
  def headRole: ProjectRole = this.roles.toList.sorted.last

  override val scope: Scope = project.scope
  override def toString: String = MoreObjects.toStringHelper(this).add("name", this.name).toString
  override def hashCode: Int = Objects.hashCode(this.project, this.name)

  override def compare(that: ProjectMember) = {
    if (!this.headRole.equals(that.headRole))
      this.headRole compare that.headRole
    else
      this.name compare that.name
  }

  override def equals(o: Any): Boolean = {
    o match {
      case that: ProjectMember => that.project.equals(this.project) && that.name.equals(this.name)
      case _ => false
    }
  }

}
