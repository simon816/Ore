package ore.user

import db.impl.access.UserBase
import db.impl.pg.OrePostgresDriver.api._
import db.impl.{OreModel, UserColumn, UserTable}
import db.{AssociativeTable, ModelAccess}
import models.user.User
import models.user.role.RoleModel

/**
  * Handles and keeps track of [[User]] "memberships" for an [[OreModel]].
  */
trait MembershipDossier {

  type ModelType <: OreModel
  type RoleType <: RoleModel
  type RoleTable <: UserColumn[RoleType]
  type MemberType <: Member[RoleType]
  type MembersTable <: AssociativeTable

  val model: ModelType
  val roleClass: Class[RoleType]
  val membersTableClass: Class[MembersTable]

  implicit def userBase: UserBase = this.model.userBase

  private def association = this.model.actions.getAssociation(this.membersTableClass)
  private def roles: ModelAccess[RoleTable, RoleType] = this.model.oneToMany[RoleTable, RoleType](this.roleClass)
  private def addMember(user: User) = this.association.assoc(model, user)

  private def clearRoles(user: User) = {
    this.model.oneToMany[RoleTable, RoleType](this.roleClass).removeAll(_.userId === user.id.get)
  }

  /**
    * Constructs a new member object of the MemberType.
    *
    * @param userId User ID of member
    * @return       New Member
    */
  def newMember(userId: Int): MemberType

  /**
    * Returns all members of the model. This includes members that have not
    * yet accepted membership.
    *
    * @return All members
    */
  def members: Set[MemberType] = {
    this.model.manyToMany[MembersTable, UserTable, User](classOf[User], this.membersTableClass).all.map { user =>
      newMember(user.id.get)
    }
  }

  /**
    * Adds a new role to the dossier and adds the user as a member if not already.
    *
    * @param role Role to add
    */
  def addRole(role: RoleType) = {
    val user = role.user
    if (!this.roles.exists(_.userId === user.id.get))
      addMember(user)
    this.roles.add(role)
  }

  /**
    * Returns all roles for the specified [[User]].
    *
    * @param user User to get roles for
    * @return     User roles
    */
  def getRoles(user: User): Set[RoleType] = this.roles.filter(_.userId === user.id.get).toSet

  /**
    * Removes a role from the dossier and removes the member if last role.
    *
    * @param role Role to remove
    */
  def removeRole(role: RoleType) = {
    this.roles.remove(role)
    val user = role.user
    if (!this.roles.exists(_.userId === user.id.get))
      removeMember(user)
  }

  /**
    * Clears all user roles and removes the user from the dossier.
    *
    * @param user User to remove
    * @return
    */
  def removeMember(user: User) = {
    clearRoles(user)
    this.association.disassoc(model, user)
  }

}
