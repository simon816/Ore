package util

import java.nio.file.Files._

import db.ModelService
import db.impl.OrePostgresDriver.api._
import db.impl.UserTable
import db.impl.action.user.UserActions
import forums.SpongeForums
import models.project.{Channel, Project, Version}
import models.user.User
import ore.project.Categories
import ore.project.util.{PendingProject, PendingVersion, ProjectFactory}
import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.ws.WSClient
import util.Conf._
import util.Sys._

/**
  * Utility class for performing some bulk actions on the application data.
  * Typically for testing.
  */
object DataUtils {

  implicit private var ws: WSClient = null
  private val pluginPath = RootDir.resolve(OreConf.getString("test-plugin").get)

  def enable()(implicit ws: WSClient) = this.ws = ws

  /**
    * Resets the application to factory defaults.
    */
  def reset()(implicit service: ModelService) = {
    for (project <- Project.values) project.delete
    service.await(service.deleteWhere[UserTable, User](classOf[User], _ => true))
    FileUtils.deleteDirectory(UploadsDir.toFile)
  }

  /**
    * Fills the application with some dummy data.
    *
    * @param users Amount of users to create
    */
  def seed(users: Int, versions: Int, channels: Int)(implicit service: ModelService) = {
    // Note: Dangerous as hell, handle with care
    SpongeForums.disable() // Disable topic creation
    this.reset()
    var pluginFile = copyPlugin
    for (i <- 0 until users) {

      println("User: " + i + '/' + users)

      // Initialize plugin
      val user = User.getOrCreate(new User(id = Some(i), _username = "User-" + i))
      while (!pluginFile.exists()) pluginFile = copyPlugin // /me throws up
      var plugin = ProjectFactory.initUpload(TemporaryFile(pluginFile), pluginFile.getName, user).get
      val pluginId = "pluginId." + i

      // Modify meta
      var meta = plugin.meta.get
      meta.setId(pluginId)

      // Create project
      val project = PendingProject(Project.fromMeta(user, meta).copy(_category = Categories.Misc), plugin).complete.get

      // Create channels
      var channelSeq: Seq[Channel] = Seq.empty
      for (i <- 0 until channels) {
        channelSeq :+= project.addChannel("Channel" + (i + 1).toString, Channel.Colors(i))
      }

      // Create additional versions
      for (i <- 0 until versions) {
        println("Version: " + i + '/' + versions)

        for ((channel, j) <- channelSeq.zipWithIndex) {
          // Initialize plugin
          while (!pluginFile.exists()) pluginFile = copyPlugin
          plugin = ProjectFactory.initUpload(TemporaryFile(pluginFile), pluginFile.getName, user).get

          // Modify meta
          meta = plugin.meta.get
          meta.setId(pluginId)
          meta.setVersion(i.toString)

          // Create version
          val version = Version.fromMeta(project, plugin).copy(channelId=channel.id.get)
          PendingVersion(user.username, project.slug, channel.name, version=version, plugin=plugin).complete.get
        }
      }
    }
    SpongeForums.enable() // Re-enable forum hooks
  }

  def migrate() = Unit

  private def copyPlugin = {
    val path = this.pluginPath.getParent.resolve("plugin.jar")
    if (notExists(path)) {
      copy(this.pluginPath, this.pluginPath.getParent.resolve("plugin.jar")).toFile
    }
    path.toFile
  }

}
