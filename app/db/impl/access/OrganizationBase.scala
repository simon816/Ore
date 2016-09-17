package db.impl.access

import db.impl.OrganizationTable
import db.{ModelBase, ModelService}
import forums.DiscourseApi
import models.user.Organization
import ore.permission.role.RoleTypes
import org.apache.commons.lang3.RandomStringUtils
import util.{CryptoUtils, OreConfig, StringUtils}

class OrganizationBase(override val service: ModelService, forums: DiscourseApi, config: OreConfig)
                       extends ModelBase[OrganizationTable, Organization] {

  import this.service.await

  override val modelClass = classOf[Organization]

  /**
    * Creates a new [[Organization]]. This method creates a new user on the
    * forums to represent the Organization.
    *
    * @param name     Organization name
    * @param ownerId  User ID of the organization owner
    * @return         New organization if successful, None otherwise
    */
  def create(name: String, ownerId: Int): Organization = {
    val password = RandomStringUtils.randomAlphanumeric(60)
    val encryptedPassword = CryptoUtils.encrypt(password, this.config.play.getString("crypto.secret").get)
    val email = name + '@' + this.config.orgs.getString("dummyEmailDomain").get

    val userId = await(this.forums.createUser(name, name, email, password)).get
    val groupId = this.config.orgs.getInt("groupId").get
    await(this.forums.addUserGroup(userId, groupId)).recover {
      case e: Exception =>
        this.forums.sync.scheduleRetry(() => this.forums.addUserGroup(userId, groupId))
    }

    val organization = Organization(Some(userId), None, name, encryptedPassword, ownerId)
    this.service.access[OrganizationTable, Organization](this.modelClass).add(organization)
  }

  /**
    * Returns an [[Organization]] with the specified name if it exists.
    *
    * @param name Organization name
    * @return     Organization with name if exists, None otherwise
    */
  def withName(name: String): Option[Organization] = this.find(StringUtils.equalsIgnoreCase(_.name, name))

}
