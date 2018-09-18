package ore.project

import scala.language.implicitConversions

import db.impl.OrePostgresDriver
import db.table.MappedType

import slick.jdbc.JdbcType

/**
  * Enumeration of Categories a Project may possess.
  */
object Categories extends Enumeration {

  val AdminTools = Category(0, "Admin Tools", "fa-server")
  val Chat       = Category(1, "Chat", "fa-comment")
  val DevTools   = Category(2, "Developer Tools", "fa-wrench")
  val Econ       = Category(3, "Economy", "fa-money")
  val Gameplay   = Category(4, "Gameplay", "fa-puzzle-piece")
  val Games      = Category(5, "Games", "fa-gamepad")
  val Protect    = Category(6, "Protection", "fa-lock")
  val Rp         = Category(7, "Role Playing", "fa-magic")
  val WorldMgmt  = Category(8, "World Management", "fa-globe")
  val Misc       = Category(9, "Miscellaneous", "fa-asterisk")
  val Undefined  = Category(10, "Undefined", null, isVisible = false)

  /**
    * Returns the visible Categories.
    *
    * @return Visible categories
    */
  def visible: Seq[Category] = this.values.filter(_.isVisible).toSeq.sortBy(_.id).map(_.asInstanceOf[Category])

  /**
    * Returns an Array of categories from a comma separated string of IDs.
    *
    * @param str  Comma separated string of IDs
    * @return     Array of Categories
    */
  def fromString(str: String): Array[Category] =
    (for (idStr <- str.split(",")) yield {
      var id: Int = -1
      try {
        id = Integer.parseInt(idStr)
      } catch {
        case _: NumberFormatException => ;
        case e: Exception             => throw e
      }
      if (id >= 0 && id < Categories.values.size) {
        Some(Categories(id).asInstanceOf[Category])
      } else {
        None
      }
    }).flatten

  /**
    * Represents a Project category.
    *
    * @param i      Index
    * @param title  Title to display
    * @param icon   Icon to display
    */
  case class Category(i: Int, title: String, icon: String, isVisible: Boolean = true)
      extends super.Val(i, title)
      with MappedType[Category] {
    implicit val mapper: JdbcType[Category] = OrePostgresDriver.api.categoryTypeMapper
  }
  implicit def convert(value: Value): Category = value.asInstanceOf[Category]

}
