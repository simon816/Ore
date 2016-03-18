package plugin

import java.nio.file.Files

import org.pegdown.PegDownProcessor
import plugin.ProjectManager._
import util.F._

object Pages {

  private val markdownProcessor = new PegDownProcessor

  /**
    * Updates the specified documentation page to the specified new name and
    * new content.
    *
    * @param owner        Project owner
    * @param projectName  Project name
    * @param oldName      Old page name
    * @param newName      New page name
    * @param content      Page content
    * @return             New page Path
    */
  def update(owner: String, projectName: String, oldName: String, newName: String, content: String) = {
    val docsDir = getDocsDir(owner, projectName)
    Files.deleteIfExists(docsDir.resolve(oldName + ".md"))
    val path = docsDir.resolve(newName + ".md")
    Files.createFile(path)
    Files.write(path, content.getBytes("UTF-8"))
  }

  def exists(owner: String, projectName: String, page: String): Boolean = {
    Files.exists(getDocsDir(owner, projectName).resolve(page + ".md"))
  }

  def delete(owner: String, projectName: String, page: String) = {
    Files.deleteIfExists(getDocsDir(owner, projectName).resolve(page + ".md"))
  }

  def getNames(owner: String, projectName: String): Array[String] = {
    for (file <- getDocsDir(owner, projectName).toFile.listFiles)
      yield file.getName.substring(0, file.getName.lastIndexOf('.'))
  }

  /**
    * Returns the specified page's contents.
    *
    * @param owner        Owner name
    * @param projectName  Project name
    * @param page         Page name
    * @return             Page contents
    */
  def getContents(owner: String, projectName: String, page: String): Option[String] = {
    val path = getDocsDir(owner, projectName).resolve(page + ".md")
    if (Files.exists(path)) {
      Some(new String(Files.readAllBytes(path)))
    } else {
      None
    }
  }

  def fillTemplate(title: String): String = {
    new String(Files.readAllBytes(CONF_DIR.resolve("markdown/default.md"))).format(title)
  }

  def toHtml(owner: String, projectName: String, page: String): Option[String] = {
    getContents(owner, projectName, page) match {
      case None => None
      case Some(contents) => Some(markdownProcessor.markdownToHtml(contents))
    }
  }

}
