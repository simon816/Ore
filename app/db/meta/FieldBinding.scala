package db.meta

import db.Model

import scala.concurrent.Future

/**
  * A FieldBinding is a mapping of a Model field to a table field and consists
  * of a getter on the Model and setter on the table.
  *
  * @param getValue  Function to retrieve value from Model
  * @param setValue  Function to update table
  * @tparam M        Model
  * @tparam A        Field type
  */
case class FieldBinding[M <: Model, A](getValue: M => A, setValue: A => Future[_])
