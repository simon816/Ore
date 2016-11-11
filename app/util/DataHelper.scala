package util

import java.nio.file.Files._
import java.nio.file.{Paths, StandardCopyOption}
import javax.inject.Inject

import com.google.common.base.Preconditions.checkArgument
import db.ModelService
import db.impl.OrePostgresDriver.api._
import db.impl.access.{ProjectBase, UserBase}
import discourse.OreDiscourseApi
import models.project.Channel
import models.user.User
import ore.OreConfig
import ore.project.factory.ProjectFactory
import ore.project.io.PluginFile
import org.apache.commons.io.FileUtils
import play.api.cache.CacheApi
import play.api.libs.Files.TemporaryFile

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

  val Logger = play.api.Logger("DataHelper")

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
    Logger.info("---- Seeding Ore ----")

    checkArgument(channels <= Channel.Colors.size,
      "cannot make more channels than there are colors", "")
    this.factory.isPgpEnabled = false

    Logger.info("Resetting Ore")
    this.reset()

    Logger.info("Initializing test plugin")
    val pluginPath = Paths.get(this.config.ore.getString("test-plugin").get)
    val tmpDir = createDirectories(pluginPath.getParent.resolve("tmp"))
    val filePath = tmpDir.resolve(pluginPath.getFileName)

    Logger.info(s"Test plugin path: $pluginPath.")

    def copyPluginFile() = {
      // Copy the original test plugin to a temporary working directory
      while (notExists(filePath))
        createFile(filePath)
      copy(pluginPath, filePath, StandardCopyOption.REPLACE_EXISTING)
    }

    copyPluginFile()

    // Create some users.
    Logger.info("Seeding...")
    for (i <- 0 until users) {
      Logger.info(Math.ceil(i / users.asInstanceOf[Float] * 100).asInstanceOf[Int].toString + "%")
      val user = this.users.add(User(id = Some(i), _username = s"User-$i"))

      // Processes the file at the 'tmp' working directory
      def doProcessPluginFile(index: Int): PluginFile = {
        val pluginFile = this.factory.processPluginFile(
          uploadedFile = TemporaryFile(filePath.toFile),
          name = filePath.getFileName.toString,
          owner = user
        )
        // Ensure the uniqueness of the metadata ID
        val meta = pluginFile.meta.get
        meta.setId(meta.getId + s"-$index")
        pluginFile
      }

      // Create some projects
      for (j <- 0 until projects) {
        val index = (i + 1) * (j + 1)
        val pluginFile = doProcessPluginFile(index)
        val project = this.factory.startProject(pluginFile).complete().get
        copyPluginFile()

        // Now create some additional versions for this project
        for (k <- 0 until channels) {
          val channelName = s"Channel$k"
          val channelColor = Channel.Colors(k)
          this.factory.createChannel(project, channelName, channelColor)

          for (l <- 0 until versions) {
            val versionFile = doProcessPluginFile(index)

            // Ensure the uniqueness of the metadata version
            val meta = pluginFile.meta.get
            meta.setVersion(meta.getVersion + s".$l")

            this.factory.startVersion(versionFile, project, channelName).complete()
            copyPluginFile()
          }
        }
      }
    }

    Logger.info("---- Seed complete ----")
  }

  def migrate() = Unit

}
