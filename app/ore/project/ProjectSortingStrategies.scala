package ore.project

import java.sql.Timestamp

import db.OrePostgresDriver.api._
import db.ProjectTable
import slick.lifted.ColumnOrdered

object ProjectSortingStrategies {

  val values: Seq[ProjectSortingStrategy] = Seq[ProjectSortingStrategy](MostStars, MostDownloads, MostViews, New)

  val Default = MostStars

  def withId(id: Int): Option[ProjectSortingStrategy] = {
    this.values.find(_.id == id)
  }

  sealed trait ProjectSortingStrategy {
    type A
    def f: ProjectTable => ColumnOrdered[A]
    def title: String
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
