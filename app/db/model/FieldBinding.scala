package db.model

import scala.concurrent.Future

/**
  * A FieldBinding is a mapping of a Model field to a table field and consists
  * of a getter on the Model and setter on the table.
  *
  * @param valueFunc  Function to retrieve value from Model
  * @param updateFunc Function to update table
  * @tparam M         Model
  * @tparam A         Field type
  */
case class FieldBinding[M <: Model, A](valueFunc: M => A, updateFunc: A => Seq[Future[_]])
