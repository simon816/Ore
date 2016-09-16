package db.meta

import db.impl.pg.OrePostgresDriver.api._
import db.{AssociativeTable, Model, ModelService}

/**
  * Represents an association between two models handled by an associative table.
  *
  * @param service      ModelService
  * @param tableClass   AssociativeTable class
  * @param assocTable   AssociativeTable TableQuery instance
  * @tparam Model1      First model in table
  * @tparam Model2      Second model in table
  * @tparam AssocTable  AssocitiveTable type
  */
class ModelAssociation[Model1 <: Model, Model2 <: Model, AssocTable <: AssociativeTable]
                      (service: ModelService,
                       ref1: AssocTable => Rep[Int],
                       ref2: AssocTable => Rep[Int],
                       val tableClass: Class[AssocTable],
                       val assocTable: TableQuery[AssocTable]) {

  /**
    * Associates the two models.
    *
    * @param model1 First model
    * @param model2 Second model
    * @return Future results
    */
  def assoc(model1: Model, model2: Model) = {
    val modelPair = orderModels(model1, model2)
    this.service.DB.db.run(this.assocTable += (modelPair._1.id.get, modelPair._2.id.get))
  }

  /**
    * Disassociates the two models.
    *
    * @param model1 First model
    * @param model2 Second model
    */
  def disassoc(model1: Model, model2: Model) = {
    val modelPair = orderModels(model1, model2)
    this.service.DB.db.run {
      this.assocTable.filter(t => ref1(t) === modelPair._1.id.get && ref2(t) === modelPair._2.id.get).delete
    }
  }

  private def orderModels(model1: Model, model2: Model): (Model, Model) = {
    model1 match {
      case _: Model1 =>
        model2 match {
          case _: Model2 =>
            (model1, model2)
          case _ =>
            throw ModelPairException
        }
      case _: Model2 =>
        model2 match {
          case _: Model1 =>
            (model2, model1)
          case _ =>
            throw ModelPairException
        }
      case _ =>
        throw ModelPairException
    }
  }

  private def ModelPairException = new RuntimeException("invalid model pair for association")

}
