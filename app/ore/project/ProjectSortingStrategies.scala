package ore.project

import java.sql.Timestamp

import db.impl.ProjectTable
import db.impl.pg.OrePostgresDriver.api._
import slick.lifted.ColumnOrdered

/**
  * Collection of sorting strategies used to sort the home page.
  */
object ProjectSortingStrategies {

  /** All sorting strategies. */
  val values: Seq[ProjectSortingStrategy] = Seq[ProjectSortingStrategy](
    MostStars, MostDownloads, MostViews, Newest, RecentlyUpdated
  )

  /** The default strategy. */
  val Default = Newest

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
    def fn: ProjectTable => ColumnOrdered[A]
    /** Display name */
    def title: String
    /** Unique ID */
    def id: Int
  }

  case object MostStars extends ProjectSortingStrategy {
    override type A = Int
    def fn = _.stars.desc
    def title = "Most stars"
    def id = 0
  }

  case object MostDownloads extends ProjectSortingStrategy {
    override type A = Int
    def fn = _.downloads.desc
    def title = "Most downloads"
    def id = 1
  }

  case object MostViews extends ProjectSortingStrategy {
    override type A = Int
    def fn = _.views.desc
    def title = "Most views"
    def id = 2
  }

  case object Newest extends ProjectSortingStrategy {
    override type A = Timestamp
    def fn = _.createdAt.desc
    def title = "Newest"
    def id = 3
  }

  case object RecentlyUpdated extends ProjectSortingStrategy {
    override type A = Timestamp
    def fn = _.lastUpdated.desc
    def title = "Recently Updated"
    def id = 4
  }

}
