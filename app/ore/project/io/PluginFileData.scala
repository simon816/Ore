package ore.project.io

import java.io.BufferedReader

import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import models.project.{Tag, TagColors}
import ore.project.Dependency

import scala.collection.mutable.ArrayBuffer

/**
  * The metadata within a [[PluginFile]]
  *
  * @author phase
  * @param data the data within a [[PluginFile]]
  */
class PluginFileData(data: Seq[DataValue[_]]) {

  val dataValues = data.groupBy(_.key).flatMap { case (key, value) =>
    // combine dependency lists that may come from different files
    if (value.size > 1 && value.head.isInstanceOf[DependencyDataValue]) {
      val dependencies = value.flatMap(_.value.asInstanceOf[Seq[Dependency]])
      Seq(DependencyDataValue(key, dependencies))
    } else value
  }.toSeq

  def getId: Option[String] = {
    get[String]("id")
  }

  def getVersion: Option[String] = {
    get[String]("version")
  }

  def getAuthors: Seq[String] = {
    get[Seq[String]]("authors").getOrElse(Seq())
  }

  def getDependencies: Seq[Dependency] = {
    get[Seq[Dependency]]("dependencies").getOrElse(Seq())
  }

  def get[T](key: String): Option[T] = {
    dataValues.filter(_.key == key).filter(_.isInstanceOf[DataValue[T]]).map(_.asInstanceOf[DataValue[T]].value).headOption
  }

  def isValidPlugin: Boolean = {
    dataValues.exists(_.isInstanceOf[StringDataValue]) &&
      dataValues.exists(_.isInstanceOf[StringDataValue])
  }

  def getGhostTags: Seq[Tag] = {
    val buffer = new ArrayBuffer[Tag]

    if (containsMixins) {
      val mixinTag = Tag(None, List(), "Mixin", "", TagColors.Mixin)
      buffer += mixinTag
    }

    println("PluginFileData#getGhostTags: " + buffer)
    buffer
  }

  /**
    * A mod using Mixins will contain the "MixinConfigs" attribute in their MANIFEST
    *
    * @return
    */
  def containsMixins: Boolean = {
    dataValues.exists(p => p.key == "MixinConfigs" && p.isInstanceOf[StringDataValue])
  }

}

object PluginFileData {
  val fileTypes: Seq[FileTypeHandler] = Seq(ModInfoHandler, ManifestHandler, ModTomlHandler)

  def getFileNames: Seq[String] = fileTypes.map(_.fileName).distinct

  def getData(fileName: String, stream: BufferedReader): Seq[DataValue[_]] = {
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
sealed trait DataValue[T] {
  val key: String
  val value: T
}

/**
  * A data element that is a String, such as the plugin id or version
  *
  * @param value the value extracted from the file
  */
case class StringDataValue(key: String, value: String)
  extends DataValue[String]

/**
  * A data element that is a list of strings, such as an authors list
  *
  * @param value the value extracted from the file
  */
case class StringSeqValue(key: String, value: Seq[String])
  extends DataValue[Seq[String]]

/**
  * A data element that is a list of [[Dependency]]
  *
  * @param value the value extracted from the file
  */
case class DependencyDataValue(key: String, value: Seq[Dependency])
  extends DataValue[Seq[Dependency]]

sealed abstract case class FileTypeHandler(fileName: String) {
  def getData(bufferedReader: BufferedReader): Seq[DataValue[_]]
}

object ModInfoHandler extends FileTypeHandler("mcmod.info") {
  override def getData(bufferedReader: BufferedReader): Seq[DataValue[_]] = {

    val dataValues = new ArrayBuffer[DataValue[_]]

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
          val authors = new ArrayBuffer[String]
          val authorsJson = modObject.getAsJsonArray("authors")
          for (j <- 0 until authorsJson.size()) {
            val element = authorsJson.get(j)
            authors += element.getAsString
          }
          dataValues += StringSeqValue("authors", authors)
        }

        // collect dependencies

        val dependencies = new ArrayBuffer[Dependency]

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

        dataValues += DependencyDataValue("dependencies", dependencies)
      }
    } catch {
      case e: Exception => e.printStackTrace()
    }

    dataValues
  }
}

object ManifestHandler extends FileTypeHandler("META-INF/MANIFEST.MF") {
  override def getData(bufferedReader: BufferedReader): Seq[DataValue[_]] = {
    val dataValues = new ArrayBuffer[DataValue[_]]

    val lines = Stream.continually(bufferedReader.readLine()).takeWhile(_ != null)
    for (line <- lines) {
      // Check for Mixins
      if (line.startsWith("MixinConfigs: ")) {
        val mixinConfigs = line.split(": ")(1)
        dataValues += StringDataValue("MixinConfigs", mixinConfigs)
      }
    }

    dataValues
  }
}

object ModTomlHandler extends FileTypeHandler("mod.toml") {
  override def getData(bufferedReader: BufferedReader): Seq[DataValue[_]] = {
    // TODO: Get format from Forge once it has been decided on
    Seq()
  }
}
