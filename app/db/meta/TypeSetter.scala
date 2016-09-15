package db.meta

import java.lang.reflect.Field

import db.impl.pg.OrePostgresDriver.api._
import db.{Model, ModelService, ModelTable}

import scala.concurrent.Future

/**
  * A class to handle delegation of binding update functions to models.
  *
  * @tparam A Type to set
  */
abstract class TypeSetter[A] {

  /**
    * Performs the update on the model.
    *
    * @param model  Model
    * @param rep    Update column
    * @param v      Value
    * @tparam T     Table
    * @tparam M     Model
    * @return       Futures
    */
  def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[A], v: A)
                                           (implicit service: ModelService): Future[_]

  /**
    * Binds the specified field to the specified model within this TypeSetter.
    *
    * @param model  Model to bind
    * @param key    Key to use
    * @param field  Field to use
    * @tparam T     Table
    * @tparam M     Model
    */
  def bindTo[T <: ModelTable[M], M <: Model](model: M, key: String, field: Field)
                                            (implicit service: ModelService) = {
    field.setAccessible(true)
    val v: model.M => A = m => field.get(m) match {
      case opt: Option[A] => opt.getOrElse(null.asInstanceOf[A])
      case a: A => a
      case _ => throw new RuntimeException("Invalid type mapping for key " + key + " in model " + model)
    }
    model.bind[A](key, v, v => this.apply[T, M](model, BootstrapTypeSetters.getRep[A](key, _), v))
  }

}
