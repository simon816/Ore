package db.impl.access

import db.impl.OrganizationTable
import db.{ModelBase, ModelService}
import forums.DiscourseApi
import models.user.Organization
import org.apache.commons.lang3.RandomStringUtils
import util.{CryptoUtils, OreConfig}

class OrganizationBase(override val service: ModelService, forums: DiscourseApi, config: OreConfig)
                       extends ModelBase[OrganizationTable, Organization] {

  override val modelClass = classOf[Organization]

  def create(name: String, ownerId: Int): Organization = {
    val password = RandomStringUtils.randomAlphanumeric(60)
    val encryptedPassword = CryptoUtils.encrypt(password, this.config.play.getString("crypto.secret").get)
    val email = name + '@' + this.config.orgs.getString("dummyEmailDomain").get

    val userId = this.service.await(this.forums.createUser(name, name, email, password)).get
    val organization = Organization(Some(userId), None, name, encryptedPassword, ownerId)
    this.service.access[OrganizationTable, Organization](this.modelClass).add(organization)
  }

}
