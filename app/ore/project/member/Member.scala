package ore.project.member

import com.google.common.base.MoreObjects
import models.project.Project
import models.user.User
import ore.permission.scope.{Scope, ScopeSubject}

class Member(val project: Project, val name: String) extends ScopeSubject {

  /**
    * Attempts the resolve this Author as a User. Not all Member's are
    * necessarily Users.
    *
    * @return User of Member if exists
    */
  def user: Option[User] = User.withName(this.name)

  override val scope: Scope = project.scope

  override def toString: String = MoreObjects.toStringHelper(this).add("name", this.name).toString

  override def hashCode: Int = this.name.hashCode

  override def equals(o: Any): Boolean = {
    o.isInstanceOf[Member] && o.asInstanceOf[Member].name.equals(this.name)
  }

}
