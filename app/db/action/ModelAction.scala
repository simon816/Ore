package db.action

import db.{Model, ModelService}
import slick.dbio.{DBIOAction, NoStream}
import util.Conf.debug

abstract class AbstractModelAction[R](val action: DBIOAction[R, NoStream, Nothing]) {
  def processResult(service: ModelService, result: R): R
}

case class ModelAction[M <: Model[_]](override val action: DBIOAction[M, NoStream, Nothing])
  extends AbstractModelAction(action) {
  def processResult(service: ModelService, result: M): M = ModelAction.process(service, result)
}

case class ModelSeqAction[M <: Model[_]](override val action: DBIOAction[Seq[M], NoStream, Nothing])
  extends AbstractModelAction(action) {
  def processResult(service: ModelService, result: Seq[M]) = for (model <- result) yield {
    ModelAction.process(service, model)
  }
}

object ModelAction {

  def process[M <: Model[_]](service: ModelService, model: M): M = {
    if (!model.isProcessed) {
      debug("Processing model: " + model + " " + model.hashCode())
      service.bindingsGenerator.generateFor(service, model)
      model.isProcessed = true
      debug("isProcessed = " + model.isProcessed)
    }
    model
  }

  implicit def unwrap[M](action: AbstractModelAction[M]): DBIOAction[M, NoStream, Nothing] = action.action

  implicit def wrapSingle[M <: Model[_]](action: DBIOAction[M, NoStream, Nothing]): ModelAction[M]
  = ModelAction(action)
  implicit def unwrapSingle[M <: Model[_]](action: ModelAction[M]): DBIOAction[M, NoStream, Nothing]
  = action.action

  implicit def wrapSeq[M <: Model[_]](action: DBIOAction[Seq[M], NoStream, Nothing]): ModelSeqAction[M]
  = ModelSeqAction(action)
  implicit def unwrapSeq[M <: Model[_]](action: ModelSeqAction[M]): DBIOAction[Seq[M], NoStream, Nothing]
  = action.action

}
