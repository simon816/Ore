package db.query

import java.sql.Timestamp

import db.driver.OrePostgresDriver.api._
import db.model.{Model, ModelTable}
import db.query.ModelQueries._
import ore.Colors.Color
import ore.permission.role.RoleTypes.RoleType
import ore.project.Categories.Category
import ore.project.FlagReasons.FlagReason

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

  def setColor[T <: ModelTable[M], M <: Model](model: M, key: T => Rep[Color], value: Color)
  = run((for { m <- modelQuery[T, M](model.getClass) if m.id === model.id.get } yield key(m)).update(value))

  def setRoleType[T <: ModelTable[M], M <: Model](model: M, key: T => Rep[RoleType], value: RoleType)
  = run((for { m <- modelQuery[T, M](model.getClass) if m.id === model.id.get } yield key(m)).update(value))

  def setRoleTypeList[T <: ModelTable[M], M <: Model](model: M, key: T => Rep[List[RoleType]], value: List[RoleType])
  = run((for { m <- modelQuery[T, M](model.getClass) if m.id === model.id.get } yield key(m)).update(value))

  def setCategory[T <: ModelTable[M], M <: Model](model: M, key: T => Rep[Category], value: Category)
  = run((for { m <- modelQuery[T, M](model.getClass) if m.id === model.id.get } yield key(m)).update(value))

  def setFlagReason[T <: ModelTable[M], M <: Model](model: M, key: T => Rep[FlagReason], value: FlagReason)
  = run((for { m <- modelQuery[T, M](model.getClass) if m.id === model.id.get } yield key(m)).update(value))

}
