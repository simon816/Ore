package db.key

import db.Model

trait Key[M <: Model] {

  def update(model: M)

}
