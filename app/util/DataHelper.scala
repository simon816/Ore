package util

import javax.inject.Inject

import db.ModelService
import db.impl.OrePostgresDriver.api._
import db.impl.access.{ProjectBase, UserBase}
import discourse.impl.OreDiscourseApi
import ore.OreConfig
import ore.project.factory.ProjectFactory
import org.apache.commons.io.FileUtils
import play.api.cache.CacheApi

/**
  * Utility class for performing some bulk actions on the application data.
  * Typically for testing.
  */
final class DataHelper @Inject()(config: OreConfig,
                                 service: ModelService,
                                 factory: ProjectFactory,
                                 forums: OreDiscourseApi,
                                 cacheApi: CacheApi) {

  implicit private val projects: ProjectBase = this.service.getModelBase(classOf[ProjectBase])
  private val users: UserBase = this.service.getModelBase(classOf[UserBase])

  /**
    * Resets the application to factory defaults.
    */
  def reset() = {
    for (project <- this.projects.all) this.projects.delete(project)
    this.users.removeAll()
    FileUtils.deleteDirectory(this.factory.env.uploads.toFile)
  }

  /**
    * Fills the application with some dummy data.
    *
    * @param users Amount of users to create
    */
  def seed(users: Int, projects: Int, versions: Int, channels: Int) = {
    // Note: Dangerous as hell, handle with care
    // TODO: Rewrite this
//    this.reset()
//    var pluginFile = copyPlugin
//
//    debug("---- Seeding Ore ----")
//    debug("Users: " + users)
//    debug("Projects/User: " + projects)
//    debug("Versions/Project: " + versions)
//    debug("---------------------\n")
//
//    for (i <- 0 until users) {
//      debug("User: " + i + '/' + users)
//      val user = this.users.getOrCreate(User(id = Some(i), _username = "User-" + i))
//      for (j <- 0 until projects) {
//        debug("Project: " + j + '/' + projects)
//        while (!pluginFile.exists()) pluginFile = copyPlugin // /me throws up
//        var plugin = this.factory.processPluginFile(TemporaryFile(pluginFile), pluginFile.getName, user).get
//        val pluginId = "pluginId." + (i + j)
//
//        // Modify meta
//        var meta = plugin.meta.get
//        meta.setId(pluginId)
//        meta.setName(meta.getName + (i + j))
//
//        // Create project
//        var project = this.factory.projectFromMeta(user, meta)
//        project.description = "Test description"
//        project.category = Categories.Misc
//        project.forums.asInstanceOf[SpongeForums].isEnabled = false
//        project = PendingProject(
//          projects = this.projects,
//          factory = this.factory,
//          project = project,
//          file = plugin,
//          config = this.config,
//          roles = Set(),
//          cacheApi = this.cacheApi
//        ).complete.get
//
//        // Create channels
//        var channelSeq: Seq[Channel] = Seq.empty
//        for (k <- 0 until channels) {
//          channelSeq :+= project.addChannel("Channel" + (k + 1).toString, Channel.Colors(k))
//        }
//
//        // Create additional versions
//        for (l <- 0 until versions) {
//          debug("Version: " + l + '/' + versions)
//
//          for ((channel, m) <- channelSeq.zipWithIndex) {
//            // Initialize plugin
//            while (!pluginFile.exists()) pluginFile = copyPlugin
//            plugin = this.factory.processPluginFile(TemporaryFile(pluginFile), pluginFile.getName, user).get
//
//            // Modify meta
//            meta = plugin.meta.get
//            meta.setId(pluginId)
//            meta.setVersion(UUID.randomUUID.toString)
//
//            // Create version
//            val version = this.service.processor.process {
//              this.factory.versionFromFile(project, plugin).copy(channelId = channel.id.get)
//            }
//
//            PendingVersion(
//              projects = this.projects,
//              factory = this.factory,
//              project = project,
//              channelName = channel.name,
//              channelColor = this.config.defaultChannelColor,
//              version = version,
//              plugin = plugin,
//              cacheApi = this.cacheApi).complete.get
//          }
//        }
//      }
//    }
  }

  def migrate() = Unit

}
