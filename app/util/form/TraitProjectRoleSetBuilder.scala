package util.form

import models.user.ProjectRole
import ore.permission.role.RoleTypes

/**
  * Takes form data and builds an uninitialized set of [[ProjectRole]].
  */
trait TraitProjectRoleSetBuilder {

  def users: List[Int]

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
