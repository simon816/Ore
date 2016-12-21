package db.impl.access

import java.io.File
import java.nio.file.Files._
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import db.access.{ImmutableModelAccess, ModelAccess}
import db.impl.OrePostgresDriver.api._
import db.{ModelBase, ModelFilter, ModelService}
import models.competition.Competition
import ore.OreConfig

import scala.collection.JavaConverters._

/**
  * Handles competition based database actions.
  *
  * @param service ModelService instance
  */
class CompetitionBase(override val service: ModelService, config: OreConfig) extends ModelBase[Competition] {

  override val modelClass = classOf[Competition]

  val uploadsDir = Paths.get(this.config.app.getString("uploadsDir").get)

  /**
    * Returns [[ModelAccess]] to all active competitions.
    *
    * @return Access to active competitions
    */
  def active: ModelAccess[Competition] = {
    val now = this.service.theTime
    ImmutableModelAccess(this.service, this.modelClass, ModelFilter[Competition] { competition =>
      competition.startDate <= now && competition.endDate > now
    })
  }

  /**
    * Saves the specified file as the specified competition's banner.
    *
    * @param competition  Competition to set banner for
    * @param file         Banner file
    * @param fileName     Banner file name
    */
  def saveBanner(competition: Competition, file: File, fileName: String) = {
    val path = getBannerDir(competition).resolve(fileName)
    if (notExists(path.getParent))
      createDirectories(path.getParent)
    list(path.getParent).iterator().asScala.foreach(delete)
    copy(file.toPath, path, StandardCopyOption.REPLACE_EXISTING)
  }

  /**
    * Returns the directory that contains the specified competition's banner.
    *
    * @param competition  Competition
    * @return             Banner directory
    */
  def getBannerDir(competition: Competition): Path
  = this.uploadsDir.resolve("competitions").resolve(competition.id.get.toString)

  /**
    * Returns the path to the specified competition's banner, if any.
    *
    * @param competition  Competition
    * @return             Banner path, if any, none otherwise
    */
  def getBannerPath(competition: Competition): Option[Path] = {
    val dir = getBannerDir(competition)
    if (exists(dir))
      Option(list(dir).findAny().orElse(null))
    else
      None
  }

}
