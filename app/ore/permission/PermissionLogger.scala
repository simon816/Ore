package ore.permission

import db.ModelService
import models.user.User

object PermissionLogger {

  val Logger = play.api.Logger("Permissions")

  def shouldLog(p: Permission): Boolean = {
    p match {
      case EditChannels
           | EditPages
           | EditSettings
           | EditVersions
           | EditApiKeys
           | HideProjects
           | HardRemoveProject
           | ReviewFlags
           | ReviewProjects
           | ReviewVisibility
           | UserAdmin
           | ResetOre
           | SeedOre
           | MigrateOre
           | CreateProject
           | PostAsOrganization
      => true
      case _ => false
    }
  }

  def log(user: User, p: Permission, service: ModelService): Unit = {
    Logger.info(s"${user.name} did ${p.getClass.getSimpleName}.")
  }

}
