package ore.project.io

import java.io.BufferedReader

import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import models.project.{Tag, TagColors}
import ore.project.Dependency

import scala.collection.mutable.ListBuffer

/**
  * The metadata within a [[PluginFile]]
  *
  * @author phase
  * @param data the data within a [[PluginFile]]
  */
class PluginFileData(var data: List[DataValue[_]]) {

  data = data.groupBy(_.key).flatMap { case (key, value) =>
    // combine dependency lists that may come from different files
    if (value.size > 1 && value.head.isInstanceOf[DependencyDataValue]) {
      val dependencies = value.flatMap(_.value.asInstanceOf[List[Dependency]])
      List(DependencyDataValue(key, dependencies))
    } else value
  }.toList

  def getId: Option[String] = {
    get[String]("id")
  }

  def getVersion: Option[String] = {
    get[String]("version")
  }

  def getAuthors: List[String] = {
    get[List[String]]("authors").getOrElse(List())
  }

  def getDependencies: List[Dependency] = {
    get[List[Dependency]]("dependencies").getOrElse(List())
  }

  def get[T](key: String): Option[T] = {
    data.filter(_.key == key).filter(_.isInstanceOf[DataValue[T]]).map(_.asInstanceOf[DataValue[T]].value).headOption
  }

  def isValidPlugin: Boolean = {
    data.exists(_.isInstanceOf[StringDataValue]) &&
      data.exists(_.isInstanceOf[StringDataValue])
  }

  def getGhostTags: List[Tag] = {
    val buffer = new ListBuffer[Tag]

    if (containsMixins) {
      val mixinTag = Tag(None, List(), "Mixin", "", TagColors.Mixin)
      buffer += mixinTag
    }

    println("PluginFileData#getGhostTags: " + buffer.toList)
    buffer.toList
  }

  /**
    * A mod using Mixins will contain the "MixinConfigs" attribute in their MANIFEST
    *
    * @return
    */
  def containsMixins: Boolean = {
    data.exists(p => p.key == "MixinConfigs" && p.isInstanceOf[StringDataValue])
  }

}

object PluginFileData {
  val fileTypes: List[FileType] = List(ModInfo, Manifest, ModToml)

  def getFileNames: List[String] = fileTypes.map(_.fileName).distinct

  def getData(fileName: String, stream: BufferedReader): List[DataValue[_]] = {
    fileTypes.filter(_.fileName == fileName).flatMap(_.getData(stream))
  }

}

/**
  * A data element in a data file
  *
  * @param key   the key for the value
  * @param value the value extracted from the file
  * @tparam T the type of the value
  */
sealed class DataValue[T](val key: String, val value: T)

/**
  * A data element that is a String, such as the plugin id or version
  *
  * @param value the value extracted from the file
  */
case class StringDataValue(override val key: String, override val value: String)
  extends DataValue[String](key, value)

/**
  * A data element that is a list of strings, such as an authors list
  *
  * @param value the value extracted from the file
  */
case class StringListValue(override val key: String, override val value: List[String])
  extends DataValue[List[String]](key, value)

/**
  * A data element that is a list of [[Dependency]]
  *
  * @param value the value extracted from the file
  */
case class DependencyDataValue(override val key: String, override val value: List[Dependency])
  extends DataValue[List[Dependency]](key, value)

sealed abstract case class FileType(fileName: String) {
  def getData(bufferedReader: BufferedReader): List[DataValue[_]]
}

object ModInfo extends FileType("mcmod.info") {
  override def getData(bufferedReader: BufferedReader): List[DataValue[_]] = {

    val dataValues = new ListBuffer[DataValue[_]]

    try {
      val parser = new JsonParser().parse(new JsonReader(bufferedReader))

      val modArray = parser.getAsJsonArray
      for (i <- 0 until modArray.size()) {
        val modObject = modArray.get(i).getAsJsonObject

        // collect members

        if (modObject.has("modid"))
          dataValues += StringDataValue("id", modObject.get("modid").getAsString)
        if (modObject.has("name"))
          dataValues += StringDataValue("name", modObject.get("name").getAsString)
        if (modObject.has("version"))
          dataValues += StringDataValue("version", modObject.get("version").getAsString)
        if (modObject.has("description"))
          dataValues += StringDataValue("description", modObject.get("description").getAsString)
        if (modObject.has("url"))
          dataValues += StringDataValue("url", modObject.get("url").getAsString)

        if (modObject.has("authors")) {
          val authors = new ListBuffer[String]
          val authorsJson = modObject.getAsJsonArray("authors")
          for (j <- 0 until authorsJson.size()) {
            val element = authorsJson.get(j)
            authors += element.getAsString
          }
          dataValues += StringListValue("authors", authors.toList)
        }

        // collect dependencies

        val dependencies = new ListBuffer[Dependency]

        if (modObject.has("requiredMods")) {
          val dependenciesJson = modObject.getAsJsonArray("requiredMods")
          for (j <- 0 until dependenciesJson.size()) {
            var dependencyId = ""
            var dependencyVersion = ""

            val dependencyJson = dependenciesJson.get(j)
            val parts = dependencyJson.getAsString.split("@")
            if (parts.nonEmpty) {
              dependencyId = parts(0)
            }
            if (parts.length > 1) {
              dependencyVersion = parts(1)
            }

            if (dependencyId != null && !dependencyId.isEmpty) {
              dependencies += Dependency(dependencyId, dependencyVersion)
            }
          }
        }

        dataValues += DependencyDataValue("dependencies", dependencies.toList)
      }
    } catch {
      case e: Exception => e.printStackTrace()
    }

    dataValues.toList
  }
}

object Manifest extends FileType("META-INF/MANIFEST.MF") {
  override def getData(bufferedReader: BufferedReader): List[DataValue[_]] = {
    val dataValues = new ListBuffer[DataValue[_]]

    val lines = Stream.continually(bufferedReader.readLine()).takeWhile(_ != null)
    for (line <- lines) {
      // Check for Mixins
      if (line.startsWith("MixinConfigs: ")) {
        val mixinConfigs = line.split(": ")(1)
        dataValues += StringDataValue("MixinConfigs", mixinConfigs)
      }
    }

    dataValues.toList
  }
}

object ModToml extends FileType("mod.toml") {
  override def getData(bufferedReader: BufferedReader): List[DataValue[_]] = {
    // TODO: Get format from Forge once it has been decided on
    List()
  }
}
