package ore.user

import scala.language.implicitConversions

import db.impl.OrePostgresDriver
import db.table.MappedType

import slick.jdbc.JdbcType

object Prompts extends Enumeration {

  val ChangeAvatar = Prompt(0, "prompt.changeAvatar.title", "prompt.changeAvatar.message")
  val PGP          = Prompt(1, "prompt.pgp.title", "prompt.pgp.message")

  case class Prompt(i: Int, titleId: String, messageId: String) extends super.Val(i, titleId) with MappedType[Prompt] {
    implicit val mapper: JdbcType[Prompt] = OrePostgresDriver.api.promptTypeMapper
  }
  implicit def convert(value: Value): Prompt = value.asInstanceOf[Prompt]

}
