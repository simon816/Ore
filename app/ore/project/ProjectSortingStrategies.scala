package ore.project

import java.sql.Timestamp

import db.OrePostgresDriver.api._
import db.ProjectTable
import slick.lifted.ColumnOrdered

/**
  * Collection of sorting strategies used to sort the home page.
  */
object ProjectSortingStrategies {

  /** All sorting strategies. */
  val values: Seq[ProjectSortingStrategy] = Seq[ProjectSortingStrategy](MostStars, MostDownloads, MostViews, New)

  /** The default strategy. */
  val Default = New

  /**
    * Returns the strategy with the specified ID.
    *
    * @param id ID to find
    * @return   Strategy with ID
    */
  def withId(id: Int): Option[ProjectSortingStrategy] = {
    this.values.find(_.id == id)
  }

  /**
    * Represents a strategy used to sort [[models.project.Project]]s.
    */
  sealed trait ProjectSortingStrategy {
    /** Type being sorted. */
    type A
    /** Sorting function */
    def f: ProjectTable => ColumnOrdered[A]
    /** Display name */
    def title: String
    /** Unique ID */
    def id: Int
  }

  case object MostStars extends ProjectSortingStrategy {
    override type A = Int
    def f = _.stars.desc
    def title = "Most stars"
    def id = 0
  }

  case object MostDownloads extends ProjectSortingStrategy {
    override type A = Int
    def f = _.downloads.desc
    def title = "Most downloads"
    def id = 1
  }

  case object MostViews extends ProjectSortingStrategy {
    override type A = Int
    def f = _.views.desc
    def title = "Most views"
    def id = 2
  }

  case object New extends ProjectSortingStrategy {
    override type A = Timestamp
    def f = _.createdAt.desc
    def title = "New"
    def id = 3
  }

}
