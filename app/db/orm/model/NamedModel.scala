package db.orm.model

abstract class NamedModel extends Model {

  /**
    * Returns the Model's name
    *
    * @return Model name
    */
  def name: String

}
