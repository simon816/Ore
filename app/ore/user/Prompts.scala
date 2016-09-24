package ore.user

import db.MappedType
import db.impl.OrePostgresDriver
import slick.jdbc.JdbcType

object Prompts extends Enumeration {

  val ChangeAvatar = Prompt(0, "prompt.changeAvatar.title", "prompt.changeAvatar.message")

  case class Prompt(i: Int, titleId: String, messageId: String) extends super.Val(i, titleId) with MappedType[Prompt] {
    implicit val mapper: JdbcType[Prompt] = OrePostgresDriver.api.promptTypeMapper
  }
  implicit def convert(value: Value): Prompt = value.asInstanceOf[Prompt]

}
