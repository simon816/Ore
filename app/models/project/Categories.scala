package models.project

/**
  * Represents a project category.
  */
object Categories extends Enumeration {

  val AdminTools  =   Category(0, "Admin Tools",       "fa-server")
  val DevTools    =   Category(1, "Developer Tools",   "fa-wrench")
  val Chat        =   Category(2, "Chat",              "fa-comment")
  val Econ        =   Category(3, "Economy",           "fa-money")
  val Protect     =   Category(4, "Protection",        "fa-lock")
  val Games       =   Category(5, "Games",             "fa-gamepad")
  val Rp          =   Category(6, "Role Playing",      "fa-magic")
  val WorldMgmt   =   Category(7, "World Management",  "fa-globe")
  val Gameplay    =   Category(8, "Gameplay",          "fa-puzzle-piece")
  val Misc        =   Category(9, "Miscellaneous",     "fa-asterisk")

  /**
    * Returns an Array of categories from a comma separated string of IDs.
    *
    * @param str  Comma separated string of IDs
    * @return     Array of Categories
    */
  def fromString(str: String): Array[Category] = (for (idStr <- str.split(",")) yield {
    var id: Int = -1
    try {
      id = Integer.parseInt(idStr)
    } catch {
      case nfe: NumberFormatException => ;
      case e: Exception => throw e
    }
    if (id >= 0 && id < Categories.values.size) {
      Some(Categories(id).asInstanceOf[Category])
    } else {
      None
    }
  }).flatten

  case class Category(i: Int, title: String, icon: String) extends super.Val(i, title)
  implicit def convert(value: Value): Category = value.asInstanceOf[Category]

}
