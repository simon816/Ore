package db.model

import com.github.tminglei.slickpg.InetString
import db.driver.OrePostgresDriver.api._
import models.statistic.StatEntry

/**
  * Represents a table that represents statistics on a Model.
  *
  * @param tag          Table tag
  * @param name         Table name
  * @param modelIdName  Column name of model ID field
  */
abstract class StatTable[M <: StatEntry[_]](tag: Tag,
                                            name: String,
                                            modelIdName: String) extends ModelTable[M](tag, name) {

  def modelId = column[Int](modelIdName)
  def address = column[InetString]("address")
  def cookie = column[String]("cookie")
  def userId = column[Int]("user_id")

}
