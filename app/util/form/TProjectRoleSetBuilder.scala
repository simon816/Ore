package util.form

import models.user.ProjectRole
import ore.permission.role.RoleTypes

/**
  * Takes form data and builds an uninitialized set of [[ProjectRole]].
  */
trait TProjectRoleSetBuilder {

  /**
    * Returns the user IDs to use in building the set.
    *
    * @return User IDs
    */
  def users: List[Int]

  /**
    * Returns the role names to use in building the set.
    *
    * @return Role names
    */
  def roles: List[String]

  /**
    * Builds the result set from the form data.
    *
    * @return Result set
    */
  def build(): Set[ProjectRole] = (for ((userId, i) <- users.zipWithIndex) yield {
    new ProjectRole(userId, RoleTypes.withName(roles(i)), -1)
  }).toSet

}
