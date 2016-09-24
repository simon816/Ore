package db

import db.impl.OrePostgresDriver.api._

/**
  * Represents an association between two models handled by an associative table.
  *
  * @param service      ModelService
  * @param tableClass   AssociativeTable class
  * @param assocTable   AssociativeTable TableQuery instance
  * @tparam AssocTable  AssociativeTable type
  */
class ModelAssociation[AssocTable <: AssociativeTable]
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
    val firstModel = this.assocTable.baseTableRow.firstClass
    val secondModel = this.assocTable.baseTableRow.secondClass
    val clazz1 = model1.getClass
    val clazz2 = model2.getClass
    if (clazz1.equals(firstModel)) {
      if (clazz2.equals(secondModel)) {
        (model1, model2)
      } else {
        throw ModelPairException
      }
    } else if (clazz1.equals(secondModel)) {
      if (clazz2.equals(firstModel)) {
        (model2, model1)
      } else {
        throw ModelPairException
      }
    } else
      throw ModelPairException
  }

  private def ModelPairException = new RuntimeException("invalid model pair for association")

}
