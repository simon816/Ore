package db.impl.access

import db.{ModelBase, ModelService}
import discourse.impl.OreDiscourseApi
import models.user.role.OrganizationRole
import models.user.{Notification, Organization, User}
import ore.OreConfig
import ore.permission.role.RoleTypes
import ore.user.notification.NotificationTypes
import org.apache.commons.lang3.RandomStringUtils
import org.spongepowered.play.CryptoUtils
import play.api.i18n.MessagesApi
import util.StringUtils

import scala.concurrent.ExecutionContext.Implicits.global

class OrganizationBase(override val service: ModelService,
                       forums: OreDiscourseApi,
                       config: OreConfig,
                       messages: MessagesApi,
                       implicit val users: UserBase)
                       extends ModelBase[Organization] {

  override val modelClass = classOf[Organization]

  /**
    * Creates a new [[Organization]]. This method creates a new user on the
    * forums to represent the Organization.
    *
    * @param name     Organization name
    * @param ownerId  User ID of the organization owner
    * @return         New organization if successful, None otherwise
    */
  def create(name: String, ownerId: Int, members: Set[OrganizationRole]): Organization = {
    // Generate the organizations forum parameters
    val password = RandomStringUtils.randomAlphanumeric(60)
    val encryptedPassword = CryptoUtils.encrypt(password, this.config.play.getString("crypto.secret").get)
    val email = name + '@' + this.config.orgs.getString("dummyEmailDomain").get

    // Create on forums
    // Let this throw an exception if the user creation fails
    val userId = this.forums.await(this.forums.createUser(name, name, email, password).map(_.right.get))

    // Add group ID
    // TODO: Handle the case where this fails
    val groupId = this.config.orgs.getInt("groupId").get
    this.forums.addUserGroup(userId, groupId)

    // Create on Ore
    val org = this.add(Organization(
      id = Some(userId),
      username = name,
      password = encryptedPassword,
      ownerId = ownerId))

    // Initialize user companion
    val userOrg = org.toUser
    userOrg.globalRoles = userOrg.globalRoles + RoleTypes.Organization

    // Invite members
    // Add the owner
    val owner: User = org.owner
    val dossier = org.memberships
    dossier.addRole(OrganizationRole(
      userId = owner.id.get,
      organizationId = org.id.get,
      _roleType = RoleTypes.OrganizationOwner,
      _isAccepted = true))

    // Invite the others
    for (role <- members) {
      dossier.addRole(role.copy(organizationId = org.id.get))
      role.user.sendNotification(Notification(
        originId = org.id.get,
        notificationType = NotificationTypes.OrganizationInvite,
        message = this.messages("notification.organization.invite", role.roleType.title, org.username)
      ))
    }

    userOrg.toOrganization
  }

  /**
    * Returns an [[Organization]] with the specified name if it exists.
    *
    * @param name Organization name
    * @return     Organization with name if exists, None otherwise
    */
  def withName(name: String): Option[Organization] = this.find(StringUtils.equalsIgnoreCase(_.name, name))

}
