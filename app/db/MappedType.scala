package db

import slick.jdbc.JdbcType

/**
  * Represents a special type that is mapped to a [[JdbcType]].
  */
trait MappedType[A] {

  implicit val mapper: JdbcType[A]

}
