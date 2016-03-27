package plugin

import java.nio.file.{Path, Files}

import org.pegdown.PegDownProcessor
import util.Dirs._

object Pages {

  val FILE_EXTENSION      =   ".md"
  val HOME_PAGE           =   "Home"
  val DEFAULT_HOME_PAGE   =   MD_DIR.resolve(HOME_PAGE + FILE_EXTENSION)
  val DEFAULT_NEW_PAGE    =   MD_DIR.resolve("default" + FILE_EXTENSION)
  val FILE_ENCODING       =   "UTF-8"

  private val markdownProcessor = new PegDownProcessor

  case class Page(private val owner: String, private val projectName: String, private var name: String) {

    private val docsDir = getDocsDir(owner, projectName)

    def getOwner: String = this.owner

    def getProjectName: String = this.projectName

    def getName: String = this.name

    def getPath: Path = this.docsDir.resolve(this.name + FILE_EXTENSION)

    /**
      * Updates this documentation page to the specified new name and
      * new content.
      *
      * @param newName    New page name
      * @param newContent Page content
      * @return           New page Path
      */
    def update(newName: String, newContent: String) = {
      Files.deleteIfExists(getPath)
      this.name = newName
      Files.createFile(getPath)
      Files.write(getPath, newContent.getBytes(FILE_ENCODING))
    }

    def setContent(newContent: String) = {
      Files.deleteIfExists(getPath)
      Files.createFile(getPath)
      Files.write(getPath, newContent.getBytes(FILE_ENCODING))
    }

    /**
      * Returns this page's markdown contents.
      *
      * @return Page contents
      */
    def getContents: String = {
      new String(Files.readAllBytes(getPath))
    }

    def toHtml: String = markdownProcessor.markdownToHtml(getContents)

  }

  /**
    * Returns the specified project's documentation directory.
    *
    * @param owner        Owner name
    * @param projectName  Project name
    * @return             Documentation directory
    */
  def getDocsDir(owner: String, projectName: String): Path = {
    DOCS_DIR.resolve(owner).resolve(projectName)
  }

  def getOrCreate(owner: String, projectName: String, name: String): Page = {
    val page = Page(owner, projectName, name)
    val path = page.getPath
    if (notExists(owner, projectName, name)) {
      Files.createDirectories(path.getParent)
      Files.createFile(path)
      page.setContent(fillTemplate(page.getName))
    }
    page
  }

  def get(owner: String, projectName: String, name: String): Option[Page] = {
    val page = Page(owner, projectName, name)
    if (notExists(owner, projectName, name)) {
      None
    } else {
      Some(page)
    }
  }

  def getHome(owner: String, projectName: String): Page = get(owner, projectName, HOME_PAGE).get

  def getAll(owner: String, projectName: String): Array[Page] = {
    for (file <- getDocsDir(owner, projectName).toFile.listFiles)
      yield Page(owner, projectName, file.getName.substring(0, file.getName.lastIndexOf('.')))
  }

  def exists(owner: String, projectName: String, page: String): Boolean = {
    Files.exists(getDocsDir(owner, projectName).resolve(page + FILE_EXTENSION))
  }

  def notExists(owner: String, projectName: String, page: String): Boolean = {
    Files.notExists(getDocsDir(owner, projectName).resolve(page + FILE_EXTENSION))
  }

  def delete(owner: String, projectName: String, page: String) = {
    Files.deleteIfExists(getDocsDir(owner, projectName).resolve(page + FILE_EXTENSION))
  }

  protected[plugin] def createHomePage(owner: String, projectName: String): Page = {
    val path = getDocsDir(owner, projectName).resolve(DEFAULT_HOME_PAGE.getFileName)
    if (Files.notExists(path.getParent)) {
      Files.createDirectories(path.getParent)
    }
    Files.copy(DEFAULT_HOME_PAGE, path)
    get(owner, projectName, HOME_PAGE).get
  }

  private def fillTemplate(title: String): String = {
    new String(Files.readAllBytes(CONF_DIR.resolve("markdown/default.md"))).format(title)
  }

}
