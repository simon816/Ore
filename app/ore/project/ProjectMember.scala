package ore.project

import com.google.common.base.MoreObjects
import db.ModelService
import db.impl.OrePostgresDriver.api._
import models.project.Project
import models.user.{ProjectRole, User}
import ore.permission.scope.{Scope, ScopeSubject}

/**
  * Represents a single or collection of members of a [[Project]].
  *
  * @param project  Project this Member is a part of
  * @param name     Member name
  */
class ProjectMember(val project: Project,
                    val name: String)(implicit service: ModelService)
                    extends ScopeSubject with Ordered[ProjectMember] {

  /**
    * Returns the [[User]] this Member belongs to.
    *
    * @return User member belongs to
    */
  def user: User = User.withName(this.name).get

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

  override def compare(that: ProjectMember) = this.headRole compare that.headRole

  override def toString: String = MoreObjects.toStringHelper(this).add("name", this.name).toString

  override def hashCode: Int = this.name.hashCode

  override def equals(o: Any): Boolean
  = o.isInstanceOf[ProjectMember] && o.asInstanceOf[ProjectMember].name.equals(this.name)

}
