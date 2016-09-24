package ore.user

import db.AssociativeTable
import db.access.ModelAccess
import db.impl.OreModel
import db.impl.OrePostgresDriver.api._
import db.impl.access.UserBase
import models.user.User
import models.user.role.RoleModel

/**
  * Handles and keeps track of [[User]] "memberships" for an [[OreModel]].
  */
trait MembershipDossier {

  type ModelType <: OreModel
  type RoleType <: RoleModel
  type MemberType <: Member[RoleType]
  type MembersTable <: AssociativeTable

  val model: ModelType
  val roleClass: Class[RoleType]
  val membersTableClass: Class[MembersTable]

  implicit def userBase: UserBase = this.model.userBase

  implicit def convertModel(model: ModelType): this.model.M = model.asInstanceOf[this.model.M]

  private def association
  = this.model.schema.getAssociation[MembersTable, User](this.membersTableClass, this.model)
  private def roles: ModelAccess[RoleType] = this.model.schema.getChildren[RoleType](this.roleClass, this.model)
  private def addMember(user: User) = this.association.add(user)

  private def clearRoles(user: User) = {
    this.model.schema.getChildren[RoleType](this.roleClass, this.model)
      .removeAll(_.userId === user.id.get)
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
    this.model.schema.getAssociation[MembersTable, User](this.membersTableClass, this.model).all.map { user =>
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
    this.association.remove(user)
  }

}

object MembershipDossier {

  val STATUS_DECLINE = "decline"
  val STATUS_ACCEPT = "accept"
  val STATUS_UNACCEPT = "unaccept"

}
