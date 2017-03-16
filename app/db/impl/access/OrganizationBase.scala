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
import security.spauth.SpongeAuthApi
import util.StringUtils

import scala.concurrent.TimeoutException
import scala.concurrent.ExecutionContext.Implicits.global

class OrganizationBase(override val service: ModelService,
                       forums: OreDiscourseApi,
                       auth: SpongeAuthApi,
                       config: OreConfig,
                       messages: MessagesApi,
                       implicit val users: UserBase)
                       extends ModelBase[Organization] {

  override val modelClass = classOf[Organization]

  val Logger = play.api.Logger("Organizations")

  /**
    * Creates a new [[Organization]]. This method creates a new user on the
    * forums to represent the Organization.
    *
    * @param name     Organization name
    * @param ownerId  User ID of the organization owner
    * @return         New organization if successful, None otherwise
    */
  def create(name: String, ownerId: Int, members: Set[OrganizationRole]): Either[String, Organization] = {
    Logger.info("Creating Organization...")
    Logger.info("Name     : " + name)
    Logger.info("Owner ID : " + ownerId)
    Logger.info("Members  : " + members.size)

    // Create the organization as a User on SpongeAuth. This will reserve the
    // name so that no new users or organizations can create an account with
    // that name. We will give the organization a dummy email for continuity.
    // By default we use "<org>@ore.spongepowered.org".
    Logger.info("Creating on SpongeAuth...")
    val dummyEmail = name + '@' + this.config.orgs.getString("dummyEmailDomain").get
    val spongeResult = this.auth.createDummyUser(name, dummyEmail, verified = true)
    // Check for error
    if (spongeResult.isLeft) {
      val error = spongeResult.left.get
      Logger.info("<FAILURE> " + error)
      return Left(error)
    }
    val spongeUser = spongeResult.right.get
    Logger.info("<SUCCESS> " + spongeUser)

    // Next we'll create a forum user for the organization. This requires a
    // password but we'll just make up a random one and not keep it. Since
    // users can't log into organization accounts the way they can with
    // user accounts, we don't need to (and shouldn't) keep the password.
    if (this.forums.isEnabled) {
      Logger.info("Creating on Discourse...")
      val dummyPassword = RandomStringUtils.randomAlphanumeric(60)
      val userResult = this.forums.await(this.forums.createUser(name, name, dummyEmail, dummyPassword).recover {
        case toe: TimeoutException =>
          Left(List("error.discourse.connect"))
        case e: Exception =>
          Left(List("error.discourse.unexpected"))
      })

      // Check for error
      if (userResult.isLeft) {
        val error = userResult.left.get.head
        Logger.info("<FAILURE> " + error)
        Logger.info("Deleting user on SpongeAuth...")
        val deleteResult = this.auth.deleteUser(spongeUser.username)
        if (deleteResult.isLeft)
          Logger.warn("Failed to delete user from SpongeAuth (id " + spongeUser.id + ")")
        return Left(error)
      }
      val forumUserId = userResult.right.get
      Logger.info("<SUCCESS> New user ID : " + forumUserId)


      // Assign the organization group to the forum user. Note: This is not
      // really critical that this succeeds since the group will be added Ore
      // side anyways. For this reason, we won't bother checking for failure and
      // just assume it has succeeded.
      val groupId = this.config.orgs.getInt("groupId").get
      this.forums.addUserGroup(forumUserId, groupId)
    }

    // Next we will create the Organization on Ore itself. This contains a
    // reference to the Sponge user ID, the organization's username and a
    // reference to the User owner of the organization.
    Logger.info("Creating on Ore...")
    val org = this.add(Organization(
      id = Some(spongeUser.id),
      username = name,
      ownerId = ownerId))

    // Every organization model has a regular User companion. Organizations
    // are just normal users with additional information. Adding the
    // Organization global role signifies that this User is an Organization
    // and should be treated as such.
    val userOrg = org.toUser.pullForumData().pullSpongeData()
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
    Logger.info("Inviting members...")
    for (role <- members) {
      dossier.addRole(role.copy(organizationId = org.id.get))
      role.user.sendNotification(Notification(
        originId = org.id.get,
        notificationType = NotificationTypes.OrganizationInvite,
        message = this.messages("notification.organization.invite", role.roleType.title, org.username)
      ))
    }

    val result = userOrg.toOrganization
    Logger.info("<SUCCESS> " + result)
    pendingOrg.remove()
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
