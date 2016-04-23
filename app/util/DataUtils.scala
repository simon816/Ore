package util

import java.nio.file.Files

import db.OrePostgresDriver.api._
import db.query.Queries
import db.query.Queries.DB._
import db.query.Queries.now
import models.project.Project
import models.user.User
import ore.project.ProjectFactory
import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.ws.WSClient
import util.C._
import util.P._
import util.forums.SpongeForums

/**
  * Utility class for performing some bulk actions on the application data.
  * Typically for testing.
  */
object DataUtils {

  implicit private var ws: WSClient = null

  def apply(implicit ws: WSClient) = this.ws = ws

  /**
    * Resets the application to factory defaults.
    */
  def reset() = {
    for (project <- now(Queries.Projects.collect()).get) project.delete
    now(run(Queries.Users.models.delete)).get
    FileUtils.deleteDirectory(UploadsDir.toFile)
  }

  /**
    * Fills the application with some dummy data.
    *
    * @param users Amount of users to create
    */
  def seed(users: Int = 200) = {
    SpongeForums.disable()
    this.reset()
    val pluginPath = RootDir.resolve(OreConf.getString("test-plugin").get)
    for (i <- 0 until users) {
      val pluginFile = Files.copy(pluginPath, pluginPath.getParent.resolve("plugin.jar")).toFile
      val user = now(Queries.Users.getOrInsert(new User(i, null, "User-" + i, null))).get
      val plugin = ProjectFactory.initUpload(TemporaryFile(pluginFile), pluginFile.getName, user).get
      val project = Project.fromMeta(user, plugin.meta.get).copy(pluginId = "pluginId." + i)
      Project.setPending(project, plugin).complete.get
    }
    SpongeForums.apply
  }

}
