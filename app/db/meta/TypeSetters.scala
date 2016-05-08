package db.meta

import java.lang.reflect.Field
import java.sql.Timestamp

import db.{Model, ModelService, ModelTable}
import db.impl.OrePostgresDriver.api._
import db.meta.BindingsGenerator.getRep

import scala.concurrent.Future

/**
  * A registry for custom field binding types.
  */
object TypeSetters {

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
    def apply[T <: ModelTable[M], M <: Model[_]](model: M, rep: T => Rep[A], v: A)
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
    def bindTo[T <: ModelTable[M], M <: Model[_]](model: M, key: String, field: Field)(implicit service: ModelService) = {
      field.setAccessible(true)
      val v: model.M => A = m => field.get(m) match {
        case opt: Option[A] => opt.getOrElse(null.asInstanceOf[A])
        case a: A => a
        case _ => throw new RuntimeException("Invalid type mapping for key " + key + " in model " + model)
      }
      model.bind[A](key, v, v => this.apply[T, M](model, getRep[A](key, _), v))
    }
  }

  // Basic types

  case object IntTypeSetter extends TypeSetter[Int] {
    def apply[T <: ModelTable[M], M <: Model[_]](model: M, rep: T => Rep[Int], v: Int)
                                                (implicit service: ModelService) = service.run {
      (for {
        m <- service.modelQuery[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  case object StringTypeSetter extends TypeSetter[String] {
    def apply[T <: ModelTable[M], M <: Model[_]](model: M, rep: T => Rep[String], v: String)
                                                (implicit service: ModelService) = service.run {
      (for {
        m <- service.modelQuery[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  case object BooleanTypeSetter extends TypeSetter[Boolean] {
    def apply[T <: ModelTable[M], M <: Model[_]](model: M, rep: T => Rep[Boolean], v: Boolean)
                                                (implicit service: ModelService) = service.run {
      (for {
        m <- service.modelQuery[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  case object IntListTypeSetter extends TypeSetter[List[Int]] {
    def apply[T <: ModelTable[M], M <: Model[_]](model: M, rep: T => Rep[List[Int]], v: List[Int])
                                                (implicit service: ModelService) = service.run {
      (for {
        m <- service.modelQuery[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  case object TimestampTypeSetter extends TypeSetter[Timestamp] {
    def apply[T <: ModelTable[M], M <: Model[_]](model: M, rep: T => Rep[Timestamp], v: Timestamp)
                                                (implicit service: ModelService) = service.run {
      (for {
        m <- service.modelQuery[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

}
