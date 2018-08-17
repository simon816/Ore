package ore

import models.project.{Tag, TagColors}
import models.project.TagColors.TagColor
import ore.project.Dependency

import scala.language.implicitConversions

/**
  * The Platform a plugin/mod runs on
  *
  * @author phase
  */
object Platforms extends Enumeration {

  val Sponge = Platform(0, "Sponge", SpongeCategory, 0, "spongeapi", TagColors.Sponge)
  val SpongeForge = Platform(2, "SpongeForge", SpongeCategory, 2, "spongeforge", TagColors.SpongeForge)
  val SpongeVanilla = Platform(3, "SpongeVanilla", SpongeCategory, 2, "spongevanilla", TagColors.SpongeVanilla)
  val SpongeCommon = Platform(4, "SpongeCommon", SpongeCategory, 1, "sponge", TagColors.SpongeCommon)
  val Lantern = Platform(5, "Lantern", SpongeCategory, 2, "lantern", TagColors.Lantern)
  val Forge = Platform(1, "Forge", ForgeCategory, 0, "forge", TagColors.Forge)

  case class Platform(override val id: Int,
                      name: String,
                      platformCategory: PlatformCategory,
                      priority: Int,
                      dependencyId: String,
                      tagColor: TagColor
                     ) extends super.Val(id, name) {

    def toGhostTag(version: String): Tag = Tag(None, List(), name, version, tagColor)

  }

  implicit def convert(v: Value): Platform = v.asInstanceOf[Platform]

  def getPlatforms(dependencyIds: List[String]): List[Platform] = {
    Platforms.values
      .filter(p => dependencyIds.contains(p.dependencyId))
      .groupBy[PlatformCategory](p => p.platformCategory)
      .flatMap(map => map._2.groupBy(p => p.priority).maxBy(_._1)._2)
      .map(p => p.asInstanceOf[Platform])
      .toList
  }

  def getPlatformGhostTags(dependencies: List[Dependency]): List[Tag] = {
    Platforms.values
      .filter(p => dependencies.map(_.pluginId).contains(p.dependencyId))
      .groupBy[PlatformCategory](p => p.platformCategory)
      .flatMap(map => map._2.groupBy(p => p.priority).maxBy(_._1)._2)
      .map(p => p.toGhostTag(dependencies.find(d => d.pluginId == p.dependencyId).get.version))
      .toList
  }

}

/**
  * The category of a platform.
  * Examples would be
  *
  * Sponge <- SpongeAPI, SpongeForge, SpongeVanilla
  * Forge <- Forge (maybe Rift if that doesn't die?)
  * Bukkit <- Bukkit, Spigot, Paper
  * Canary <- Canary, Neptune
  *
  * @author phase
  */
sealed trait PlatformCategory {
  val name: String

  def getPlatforms: List[Platforms.Value] = {
    Platforms.values.filter(p => p.platformCategory == this).toList
  }
}

case object SpongeCategory extends PlatformCategory {
  override val name = "Sponge Plugins"
}

case object ForgeCategory extends PlatformCategory {
  override val name = "Forge Mods"
}

object PlatformCategory {
  def getPlatformCategories: List[PlatformCategory] = List(SpongeCategory, ForgeCategory)
}
