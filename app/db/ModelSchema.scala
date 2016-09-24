package db

import db.access.{ImmutableModelAccess, ModelAccess, ModelAssociationAccess}
import db.impl.OrePostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

/**
  * Base class for handling Model queries. ModelActions define how Models can
  * interact with the database.
  */
class ModelSchema[M <: Model](val service: ModelService,
                              val modelClass: Class[M],
                              val baseQuery: TableQuery[M#T]) {

  case class Associate[T <: AssociativeTable, A <: Model](tableClass: Class[T], modelClass: Class[A])

  private var associations: Map[Class[_ <: AssociativeTable], ModelAssociation[_]] = Map.empty
  private var associatedModels: Map[Class[_ <: AssociativeTable], Class[_ <: Model]] = Map.empty
  private var associativeSelfReferences: Map[Class[_ <: AssociativeTable], AssociativeTable => Rep[Int]] = Map.empty
  private var associativeOtherReferences: Map[Associate[_, _], AssociativeTable => Rep[Int]] = Map.empty

  private var children: Map[Class[_ <: Model], ModelTable[_] => Rep[Int]] = Map.empty
  private var siblings: Map[Class[_ <: Model], M => Int] = Map.empty

  /**
    * Adds a new [[ModelAssociation]] between two models. The order of the
    * model types must match the order in the table.
    *
    * @param association    ModelAssociation
    * @tparam Assoc   Association table
    * @return         This instance
    */
  def withAssociation[Assoc <: AssociativeTable, A <: Model](association: ModelAssociation[Assoc],
                                                             selfReference: Assoc => Rep[Int],
                                                             targetClass: Class[A],
                                                             targetReference: Assoc => Rep[Int]) = {
    val tableClass = association.tableClass
    this.associations += tableClass -> association
    this.associatedModels += tableClass -> targetClass
    this.associativeSelfReferences += tableClass -> selfReference.asInstanceOf[AssociativeTable => Rep[Int]]
    this.associativeOtherReferences += Associate[Assoc, A](tableClass, targetClass) ->
      targetReference.asInstanceOf[AssociativeTable => Rep[Int]]
    this
  }

  def getAssociation[Assoc <: AssociativeTable, A <: Model](assocTableClass: Class[Assoc],
                                                            model: M): ModelAssociationAccess[Assoc, A] = {
    val parentRef: AssociativeTable => Rep[Int] = this.associativeSelfReferences(assocTableClass)
    val otherClass: Class[A] = this.associatedModels(assocTableClass).asInstanceOf[Class[A]]
    val otherRef: AssociativeTable => Rep[Int] = this.associativeOtherReferences(Associate[Assoc, A](
      assocTableClass, otherClass))
    val association = this.associations(assocTableClass).asInstanceOf[ModelAssociation[Assoc]]
    new ModelAssociationAccess[Assoc, A](this.service, model, parentRef, otherClass, otherRef, association)
  }

  def withChildren[C <: Model](modelClass: Class[C], ref: C#T => Rep[Int]) = {
    this.children += modelClass -> ref.asInstanceOf[ModelTable[_] => Rep[Int]]
    this
  }

  def getChildren[C <: Model](modelClass: Class[C], model: M): ModelAccess[C] = {
    val ref: C#T => Rep[Int] = this.children(modelClass)
    ImmutableModelAccess(this.service.access[C](modelClass, ModelFilter[C](ref(_) === model.id.get)))
  }

  def withSibling[S <: Model](modelClass: Class[S], ref: M => Int) = {
    this.siblings += modelClass -> ref
    this
  }

  def getSibling[S <: Model](modelClass: Class[S], model: M): Future[Option[S]] = {
    val ref: M => Int = this.siblings(modelClass)
    this.service.get[S](modelClass, ref(model))
  }

  /**
    * Returns the specified model or creates it if it doesn't exist.
    *
    * @param model  Model to get or create
    * @return       Existing or newly created model
    */
  def getOrInsert(model: M): Future[M] = {
    val modelPromise = Promise[M]
    like(model).onComplete {
      case Failure(thrown) => modelPromise.failure(thrown)
      case Success(modelOpt) => modelOpt match {
        case Some(existing) => modelPromise.success(existing)
        case None => modelPromise.completeWith(service insert model)
      }
    }
    modelPromise.future
  }

  /**
    * Tries to find the specified model in it's table with an unset ID.
    *
    * @param model  Model to find
    * @return       Model if found
    */
  def like(model: M): Future[Option[M]] = Future(None)

}
