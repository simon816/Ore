package db.orm

import db.OrePostgresDriver.api._
import db.orm.model.NamedModel
import slick.lifted.Tag

abstract class NamedModelTable[M <: NamedModel](tag: Tag, name: String) extends ModelTable[M](tag, name) {

  /**
    * Returns the column that hold the model name.
    *
    * @return Rep of name
    */
  def modelName: Rep[String]

}
