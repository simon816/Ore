package db.model.annotation

import java.lang.reflect.Field
import java.sql.Timestamp

import db.driver.OrePostgresDriver.api._
import db.model.{Model, ModelTable}
import db.query.ModelQueries

import scala.concurrent.Future

/**
  * A registry for custom field binding types.
  */
object TypeSetters {

  private var setters: Map[Class[_], TypeSetter[_]] = Map(
    classOf[Int] -> IntTypeTypeSetter,
    classOf[String] -> StringTypeSetter,
    classOf[Boolean] -> BooleanTypeSetter,
    classOf[List[Int]] -> IntListTypeSetter,
    classOf[Timestamp] -> TimestampTypeSetter
  )

  /**
    * Registers a new type setter for Models.
    *
    * @param clazz  Type class
    * @param setter TypeSetter instance
    * @tparam A     Type
    */
  def register[A](clazz: Class[A], setter: TypeSetter[A]) = setters += clazz -> setter

  /**
    * Returns the TypeSetter for the specified type, if any.
    *
    * @param clazz Type class
    * @tparam A Type
    * @return TypeSetter, if any, None otherwise
    */
  def get[A](clazz: Class[A]): Option[TypeSetter[A]]
  = setters.find(_._1.isAssignableFrom(clazz)).map(_._2.asInstanceOf[TypeSetter[A]])

  private def getRep[A](name: String, table: ModelTable[_])
  = table.getClass.getDeclaredMethod(name).invoke(table).asInstanceOf[Rep[A]]

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
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[A], v: A): Seq[Future[_]]

    /**
      * Binds the specified field to the specified model within this TypeSetter.
      *
      * @param model  Model to bind
      * @param key    Key to use
      * @param field  Field to use
      * @tparam T     Table
      * @tparam M     Model
      */
    def bindTo[T <: ModelTable[M], M <: Model](model: M, key: String, field: Field) = {
      field.setAccessible(true)
      val v: model.M => A = m => field.get(m) match {
        case opt: Option[A] => opt.getOrElse(null.asInstanceOf[A])
        case a: A => a
        case _ => throw new RuntimeException("Invalid type mapping for key " + key + " in model " + model)
      }
      model.bind[A](key, v, v => this.apply[T, M](model, t => getRep[A](key, t), v))
    }
  }

  // Basic types

  case object IntTypeTypeSetter extends TypeSetter[Int] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[Int], v: Int)
    = Seq(ModelQueries.setInt[T, M](model, rep, v))
  }

  case object StringTypeSetter extends TypeSetter[String] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[String], v: String)
    = Seq(ModelQueries.setString[T, M](model, rep, v))
  }

  case object BooleanTypeSetter extends TypeSetter[Boolean] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[Boolean], v: Boolean)
    = Seq(ModelQueries.setBoolean[T, M](model, rep, v))
  }

  case object IntListTypeSetter extends TypeSetter[List[Int]] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[List[Int]], v: List[Int])
    = Seq(ModelQueries.setIntList[T, M](model, rep, v))
  }

  case object TimestampTypeSetter extends TypeSetter[Timestamp] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[Timestamp], v: Timestamp)
    = Seq(ModelQueries.setTimestamp[T, M](model, rep, v))
  }

}
