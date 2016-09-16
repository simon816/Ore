package db

import db.impl.pg.OrePostgresDriver.api._

/**
  * A wrapper class for a T => Rep[Boolean] on a ModelTable. This allows for easier
  * chaining of filters on a ModelTable. ModelFilters can have their base
  * function lifted out of this wrapper implicitly.
  *
  * @param fn   Base filter function
  * @tparam T   Table type
  * @tparam M   Model type
  */
case class ModelFilter[T <: ModelTable[M], M <: Model](fn: T => Rep[Boolean] = null) {

  /**
    * Applies && to the wrapped function and returns a new filter.
    *
    * @param that Filter function to apply
    * @return New model filter
    */
  def &&(that: T => Rep[Boolean]): ModelFilter[T, M] = ModelFilter(m => trueIfNull(fn)(m) && trueIfNull(that)(m))

  /**
    * Applies && to the specified filter's function and returns the result
    * filter.
    *
    * @param that Filter to apply
    * @return New filter
    */
  def +&&(that: ModelFilter[T, M]): ModelFilter[T, M] = this && that.fn

  /**
    * Applies && to the specified filter's function and returns the result
    * filter function.
    *
    * @param that Filter function to apply
    * @return Filter function
    */
  def &&^(that: T => Rep[Boolean]): T => Rep[Boolean] = (this && that).fn

  /**
    * Applies && to the specified filter's function and returns the result
    * filter function.
    *
    * @param that Filter to apply
    * @return Filter function
    */
  def +&&^(that: ModelFilter[T, M]): T => Rep[Boolean] = (this +&& that).fn

  /**
    * Applies || to the wrapped function and returns a new filter.
    *
    * @param that Filter function to apply
    * @return New filter
    */
  def ||(that: T => Rep[Boolean]): ModelFilter[T, M] = ModelFilter(m => falseIfNull(fn)(m) || falseIfNull(that)(m))

  /**
    * Applies || to the specified filter's function and returns the result
    * filter.
    *
    * @param that Filter to apply
    * @return New filter
    */
  def +||(that: ModelFilter[T, M]): ModelFilter[T, M] = this || that.fn

  /**
    * Applies || to the specified filter's function and returns the result
    * filter function.
    *
    * @param that Filter function to apply
    * @return Filter function
    */
  def ||^(that: T => Rep[Boolean]): T => Rep[Boolean] = (this || that).fn

  /**
    * Applies || to the specified filter's function and returns the result
    * filter function.
    *
    * @param that Filter to apply
    * @return Filter function
    */
  def +||^(that: T => Rep[Boolean]): T => Rep[Boolean] = (this +|| that).fn

  private def trueIfNull(fn: T => Rep[Boolean]): T => Rep[Boolean] = if (fn == null) _ => true else fn
  private def falseIfNull(fn: T => Rep[Boolean]): T => Rep[Boolean] = if (fn == null) _ => false else fn

}

object ModelFilter {

  implicit def unwrapFilter[T <: ModelTable[M], M <: Model](filter: ModelFilter[T, M]): T => Rep[Boolean]
  = if (filter == null) null else filter.fn
  implicit def wrapFilter[T <: ModelTable[M], M <: Model](fn: T => Rep[Boolean]): ModelFilter[T, M]
  = ModelFilter(fn)

  /** Filters models by ID */
  def IdFilter[T <: ModelTable[M], M <: Model](id: Int): ModelFilter[T, M] = ModelFilter(_.id === id)

}
