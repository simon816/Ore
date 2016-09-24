package db.impl.table

import db.impl.OrePostgresDriver.api._
import db.impl.model.Downloadable
import db.table.ModelTable

/**
  * Represents a [[ModelTable]] column for keeping track of downloads.
  *
  * @tparam M Model type
  */
trait DownloadsColumn[M <: Downloadable] extends ModelTable[M] {

  /**
    * The column that keeps track of downloads.
    *
    * @return Column that keeps track of downloads
    */
  def downloads = column[Int]("downloads")

}
