package models.project

/**
  * Represents a project category.
  */
object Category extends Enumeration {

  val AdminTools  =   CategoryVal(0, "Admin Tools",       "fa-server")
  val DevTools    =   CategoryVal(1, "Developer Tools",   "fa-wrench")
  val Chat        =   CategoryVal(2, "Chat",              "fa-comment")
  val Econ        =   CategoryVal(3, "Economy",           "fa-money")
  val Protect     =   CategoryVal(4, "Protection",        "fa-lock")
  val Games       =   CategoryVal(5, "Games",             "fa-gamepad")
  val Rp          =   CategoryVal(6, "Role Playing",      "fa-magic")
  val WorldMgmt   =   CategoryVal(7, "World Management",  "fa-globe")
  val Gameplay    =   CategoryVal(8, "Gameplay",          "fa-puzzle-piece")
  val Misc        =   CategoryVal(9, "Miscellaneous",     "fa-asterisk")

  protected case class CategoryVal(i: Int, title: String, icon: String) extends super.Val(i, title)
  implicit def convert(value: Value): CategoryVal = value.asInstanceOf[CategoryVal]

}
