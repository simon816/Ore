package ore.permission

import com.github.tminglei.slickpg.InetString
import db.ModelService
import models.user.{User, UserAction}

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
    // TODO: get user's address
    service.insert(new UserAction(None, None, user.userId, InetString("127.0.0.1"),  s"${p.getClass.getSimpleName}"))
  }

}
