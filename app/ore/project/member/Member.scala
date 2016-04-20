package ore.project.member

import com.google.common.base.MoreObjects
import db.OrePostgresDriver.api._
import models.project.Project
import models.user.{ProjectRole, User}
import ore.permission.scope.{Scope, ScopeSubject}

class Member(val project: Project, val name: String) extends ScopeSubject with Ordered[Member] {

  def user: User = User.withName(this.name).get

  def roles: Set[ProjectRole] = this.project.roles.filter(_.userId === this.user.id.get).toSet

  def headRole: ProjectRole = this.roles.toList.sorted.last

  override val scope: Scope = project.scope

  override def compare(that: Member) = this.headRole compare that.headRole

  override def toString: String = MoreObjects.toStringHelper(this).add("name", this.name).toString

  override def hashCode: Int = this.name.hashCode

  override def equals(o: Any): Boolean = {
    o.isInstanceOf[Member] && o.asInstanceOf[Member].name.equals(this.name)
  }

}
