package db.impl.access

import db.{ModelBase, ModelService}
import discourse.OreDiscourseApi
import models.user.role.OrganizationRole
import models.user.{Notification, Organization, User}
import ore.OreConfig
import ore.permission.role.RoleTypes
import ore.user.notification.NotificationTypes
import org.apache.commons.lang3.RandomStringUtils
import play.api.i18n.MessagesApi
import security.SpongeAuthApi
import util.StringUtils

class OrganizationBase(override val service: ModelService,
                       forums: OreDiscourseApi,
                       auth: SpongeAuthApi,
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
  def create(name: String, ownerId: Int, members: Set[OrganizationRole]): Either[String, Organization] = {
    // Create the organization as a User on SpongeAuth. This will reserve the
    // name so that no new users or organizations can create an account with
    // that name. We will give the organization a dummy email for continuity.
    // By default we use "<org>@ore.spongepowered.org".
    val dummyEmail = name + '@' + this.config.orgs.getString("dummyEmailDomain").get
    val spongeResult = this.auth.createDummyUser(name, dummyEmail, verified = true)
    // Check for error
    if (spongeResult.isLeft)
      return Left(spongeResult.left.get)
    val spongeUser = spongeResult.right.get

    // Next we'll create a forum user for the organization. This requires a
    // password but we'll just make up a random one and not keep it. Since
    // users can't log into organization accounts the way they can with
    // user accounts, we don't need to (and shouldn't) keep the password.
    val dummyPassword = RandomStringUtils.randomAlphanumeric(60)
    val userResult = this.forums.await(this.forums.createUser(name, name, dummyEmail, dummyPassword))
    // Check for error
    if (userResult.isLeft)
      return Left(userResult.left.get.head)
    val forumUserId = userResult.right.get

    // Assign the organization group to the forum user. Note: This is not
    // really critical that this succeeds since the group will be added Ore
    // side anyways. For this reason, we won't bother checking for failure and
    // just assume it has succeeded.
    val groupId = this.config.orgs.getInt("groupId").get
    this.forums.addUserGroup(forumUserId, groupId)

    // Next we will create the Organization on Ore itself. This contains a
    // reference to the Sponge user ID, the organization's username and a
    // reference to the User owner of the organization.
    val org = this.add(Organization(
      id = Some(spongeUser.id),
      username = name,
      ownerId = ownerId))

    // Every organization model has a regular User companion. Organizations
    // are just normal users with additional information. Adding the
    // Organization global role signifies that this User is an Organization
    // and should be treated as such.
    val userOrg = org.toUser.refreshForumData()
    userOrg.globalRoles = userOrg.globalRoles + RoleTypes.Organization

    // Add the owner
    val owner: User = org.owner
    val dossier = org.memberships
    dossier.addRole(OrganizationRole(
      userId = owner.id.get,
      organizationId = org.id.get,
      _roleType = RoleTypes.OrganizationOwner,
      _isAccepted = true))

    // Invite the User members that the owner selected during creation.
    for (role <- members) {
      dossier.addRole(role.copy(organizationId = org.id.get))
      role.user.sendNotification(Notification(
        originId = org.id.get,
        notificationType = NotificationTypes.OrganizationInvite,
        message = this.messages("notification.organization.invite", role.roleType.title, org.username)
      ))
    }

    Right(userOrg.toOrganization)
  }

  /**
    * Returns an [[Organization]] with the specified name if it exists.
    *
    * @param name Organization name
    * @return     Organization with name if exists, None otherwise
    */
  def withName(name: String): Option[Organization] = this.find(StringUtils.equalsIgnoreCase(_.name, name))

}
