package ore

/**
  * Represents a project category.
  */
object Categories extends Enumeration {

  val AdminTools  =   Category(0, "Admin Tools",       "fa-server")
  val Chat        =   Category(1, "Chat",              "fa-comment")
  val DevTools    =   Category(2, "Developer Tools",   "fa-wrench")
  val Econ        =   Category(3, "Economy",           "fa-money")
  val Gameplay    =   Category(4, "Gameplay",          "fa-puzzle-piece")
  val Games       =   Category(5, "Games",             "fa-gamepad")
  val Protect     =   Category(6, "Protection",        "fa-lock")
  val Rp          =   Category(7, "Role Playing",      "fa-magic")
  val WorldMgmt   =   Category(8, "World Management",  "fa-globe")
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
