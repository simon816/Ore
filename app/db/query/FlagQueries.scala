package db.query

import java.sql.Timestamp

import db.OrePostgresDriver.api._
import db.FlagTable
import models.project.Flag

class FlagQueries extends Queries[FlagTable, Flag](TableQuery[FlagTable]) {
  override def copyInto(id: Option[Int], theTime: Option[Timestamp], model: Flag): Flag = {
    model.copy(id = id, createdAt = theTime)
  }
}
