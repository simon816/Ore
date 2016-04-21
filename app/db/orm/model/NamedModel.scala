package db.orm.model

/**
  * Represents a Model with a name.
  */
abstract class NamedModel extends Model {

  /**
    * Returns the Model's name
    *
    * @return Model name
    */
  def name: String

}
