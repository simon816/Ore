package db.impl.action

import db.ModelService
import db.action.ModelActions
import db.impl.{OrganizationMembersTable, OrganizationTable}
import db.impl.OrePostgresDriver.api._
import db.impl.service.UserBase
import models.user.{Organization, User}
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Custom [[ModelActions]] implementation for [[Organization]]s to keep track
  * of it's members.
  *
  * @param service ModelService instance
  */
class OrganizationActions(override val service: ModelService)
  extends ModelActions[OrganizationTable, Organization](service, classOf[Organization], TableQuery[OrganizationTable]) {

  val Members = TableQuery[OrganizationMembersTable]
  private val users = this.service.access(classOf[UserBase])

  /**
    * Returns the [[User]]s that belong to the specified [[Organization]].
    *
    * @param org Organization
    * @return Users in Organization
    */
  def getMembers(org: Organization): Future[Seq[User]] = {
    this.service.DB.db.run {
      (for (member <- Members.filter(_.organizationId === org.id.get)) yield member.userId).result
    } map { userIds =>
      userIds.map(this.users.get(_).get)
    }
  }

  override def like(org: Organization) = this.find(_.name.toLowerCase === org.name.toLowerCase)

}
