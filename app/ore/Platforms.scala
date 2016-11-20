package ore

import scala.language.implicitConversions

object Platforms extends Enumeration {

  val Sponge = Platform(0, "Sponge")
  val Forge = Platform(1, "Forge")

  case class Platform(override val id: Int, name: String) extends super.Val(id, name)
  implicit def convert(v: Value): Platform = v.asInstanceOf[Platform]

}
