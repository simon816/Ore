package db.action

import db.{Model, ModelService}
import slick.dbio.{DBIOAction, NoStream}

import scala.reflect.runtime.universe._

object ModelAction {

  /**
    * Represents a DBIOAction that returns one or multiple Models. ModelActions
    * are implicitly wrapped and unwrapped.
    *
    * @param action DBIOAction to wrap
    * @tparam R     Return type
    */
  abstract class AbstractModelAction[R](val action: DBIOAction[R, NoStream, Nothing]) {
    def processResult(service: ModelService, result: R): R
  }

  /**
    * Represents a ModelAction that returns a single model.
    *
    * @param action DBIOAction to wrap
    * @tparam M     Model type
    */
  case class ModelAction[M <: Model: TypeTag](override val action: DBIOAction[M, NoStream, Nothing])
    extends AbstractModelAction(action) {
    def processResult(service: ModelService, result: M): M = process(service, result)
  }

  /**
    * Represents a ModelAction that returns multiple models.
    *
    * @param action DBIOAction to wrap
    * @tparam M     Model type
    */
  case class ModelSeqAction[M <: Model: TypeTag](override val action: DBIOAction[Seq[M], NoStream, Nothing])
    extends AbstractModelAction(action) {
    def processResult(service: ModelService, result: Seq[M]) = for (model <- result) yield {
      process(service, model)
    }
  }

  implicit def unwrap[M](action: AbstractModelAction[M]): DBIOAction[M, NoStream, Nothing] = action.action
  implicit def wrapSingle[M <: Model: TypeTag](action: DBIOAction[M, NoStream, Nothing]): ModelAction[M]
  = ModelAction(action)
  implicit def unwrapSingle[M <: Model: TypeTag](action: ModelAction[M]): DBIOAction[M, NoStream, Nothing]
  = action.action
  implicit def wrapSeq[M <: Model: TypeTag](action: DBIOAction[Seq[M], NoStream, Nothing]): ModelSeqAction[M]
  = ModelSeqAction(action)
  implicit def unwrapSeq[M <: Model: TypeTag](action: ModelSeqAction[M]): DBIOAction[Seq[M], NoStream, Nothing]
  = action.action

  private def process[M <: Model: TypeTag](service: ModelService, model: M): M = {
    if (!model.isProcessed)
      service.processor.process(model)
    model
  }

}
