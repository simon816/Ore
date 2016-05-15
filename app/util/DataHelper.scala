package util

import java.nio.file.Files._
import java.nio.file.Paths
import javax.inject.Inject

import db.ModelService
import db.impl.service.{ProjectBase, UserBase}
import forums.{DisabledDiscourseApi, DiscourseApi}
import models.project.{Channel, Version}
import models.user.User
import ore.project.Categories
import ore.project.util.{PendingProject, PendingVersion, ProjectFactory, ProjectManager}
import org.apache.commons.io.FileUtils
import play.api.cache.CacheApi
import play.api.libs.Files.TemporaryFile

/**
  * Utility class for performing some bulk actions on the application data.
  * Typically for testing.
  */
class DataHelper @Inject()(implicit config: OreConfig,
                           service: ModelService,
                           manager: ProjectManager,
                           factory: ProjectFactory,
                           forums: DiscourseApi,
                           cacheApi: CacheApi) {

  import config.debug

  private val pluginPath = Paths.get(config.ore.getString("test-plugin").get)
  implicit private val projects: ProjectBase = service.access(classOf[ProjectBase])
  private val users: UserBase = service.access(classOf[UserBase])

  /**
    * Resets the application to factory defaults.
    */
  def reset() = {
    for (project <- this.projects.all) this.manager.deleteProject(project)
    this.users.removeAll()
    FileUtils.deleteDirectory(factory.env.uploads.toFile)
  }

  /**
    * Fills the application with some dummy data.
    *
    * @param users Amount of users to create
    */
  def seed(users: Int, versions: Int, channels: Int) = {
    // Note: Dangerous as hell, handle with care
    this.reset()
    var pluginFile = copyPlugin
    for (i <- 0 until users) {

      debug("User: " + i + '/' + users)

      // Initialize plugin
      val user = this.users.getOrCreate(new User(id = Some(i), _username = "User-" + i))
      while (!pluginFile.exists()) pluginFile = copyPlugin // /me throws up
      var plugin = this.factory.processPluginFile(TemporaryFile(pluginFile), pluginFile.getName, user).get
      val pluginId = "pluginId." + i

      // Modify meta
      var meta = plugin.meta.get
      meta.setId(pluginId)

      // Create project
      var project = this.factory.projectFromMeta(user, meta).copy(_category = Categories.Misc)
      project.config = this.config
      project.description = "Test description"
      project.forums = new DisabledDiscourseApi
      project = PendingProject(
        manager = this.manager,
        factory = this.factory,
        project = project,
        file = plugin,
        config = this.config,
        roles = Set(),
        cacheApi = this.cacheApi
      ).complete.get

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
          plugin = this.factory.processPluginFile(TemporaryFile(pluginFile), pluginFile.getName, user).get

          // Modify meta
          meta = plugin.meta.get
          meta.setId(pluginId)
          meta.setVersion(i.toString)

          // Create version
          val version = this.factory.versionFromFile(project, plugin).copy(channelId=channel.id.get)
          PendingVersion(
            manager = this.manager,
            factory = this.factory,
            owner = user.username,
            projectSlug = project.slug,
            channelName = channel.name,
            channelColor = Channel.DefaultColor,
            version = version,
            plugin = plugin,
            cacheApi = this.cacheApi).complete.get
        }
      }
    }
  }

  def migrate() = Unit

  private def copyPlugin = {
    val path = this.pluginPath.getParent.resolve("plugin.jar")
    if (notExists(path)) copy(this.pluginPath, this.pluginPath.getParent.resolve("plugin.jar")).toFile
    path.toFile
  }

}
