package db.query

import java.sql.Timestamp

import db.OrePostgresDriver.api._
import db.query.ModelQueries._
import db.orm.ModelTable
import db.orm.model.Model

trait Setters {

  /**
    * Sets an Int field on the Model.
    *
    * @param model  Model to update
    * @param key    Key to update
    * @param value  Value to set
    */
  def setInt[T <: ModelTable[M], M <: Model](model: M, key: T => Rep[Int], value: Int)
  = run((for { m <- modelQuery[T, M](model.getClass) if m.id === model.id.get } yield key(m)).update(value))

  /**
    * Sets a String field on the Model.
    *
    * @param model  Model to update
    * @param key    Key to update
    * @param value  Value to set
    */
  def setString[T <: ModelTable[M], M <: Model](model: M, key: T => Rep[String], value: String)
  = run((for { m <- modelQuery[T, M](model.getClass) if m.id === model.id.get } yield key(m)).update(value))

  /**
    * Sets a Boolean field on the Model.
    *
    * @param model  Model to update
    * @param key    Key to update
    * @param value  Value to set
    */
  def setBoolean[T <: ModelTable[M], M <: Model](model: M, key: T => Rep[Boolean], value: Boolean)
  = run((for { m <- modelQuery[T, M](model.getClass) if m.id === model.id.get } yield key(m)).update(value))

  /**
    * Sets an int array field on the Model.
    *
    * @param model  Model to update
    * @param key    Key to update
    * @param value  Value
    */
  def setIntList[T <: ModelTable[M], M <: Model](model: M, key: T => Rep[List[Int]], value: List[Int])
  = run((for { m <- modelQuery[T, M](model.getClass) if m.id === model.id.get } yield key(m)).update(value))

  /**
    * Sets a Timestamp field on the Model.
    *
    * @param model  Model to update
    * @param key    Key to update
    * @param value  Value
    */
  def setTimestamp[T <: ModelTable[M], M <: Model](model: M, key: T => Rep[Timestamp], value: Timestamp)
  = run((for { m <- modelQuery[T, M](model.getClass) if m.id === model.id.get } yield key(m)).update(value))


}
