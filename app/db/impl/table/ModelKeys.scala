package db.impl.table

import db.Named
import db.impl.OrePostgresDriver.api._
import db.impl.model.{Describable, Downloadable}
import db.table.key._
import models.project._
import models.statistic.StatEntry
import models.user.role.RoleModel
import models.user.{Notification, User}
import ore.Colors.Color
import ore.permission.role.RoleTypes.RoleType
import ore.project.Categories.Category

/**
  * Collection of String keys used for table bindings within Models.
  */
object ModelKeys {

  val Name                  =   StringKey[Named](_.name, _.name)
  val Downloads             =   IntKey[Downloadable](_.downloads, _.downloadCount)
  val Description           =   StringKey[Describable](_.description, _.description.orNull)

  val OwnerId               =   IntKey[Project](_.userId, _.ownerId)
  val OwnerName             =   StringKey[Project](_.ownerName, _.ownerName)
  val Slug                  =   StringKey[Project](_.slug, _.slug)
  val Category              =   MappedTypeKey[Project, Category](_.category, _.category)
  val Stars                 =   IntKey[Project](_.stars, _.starCount)
  val Views                 =   IntKey[Project](_.views, _.viewCount)
  val Issues                =   StringKey[Project](_.issues, _.issues.orNull)
  val Source                =   StringKey[Project](_.source, _.source.orNull)
  val TopicId               =   IntKey[Project](_.topicId, _.topicId.getOrElse(-1))
  val PostId                =   IntKey[Project](_.postId, _.postId.getOrElse(-1))
  val RecommendedVersionId  =   IntKey[Project](
                                  _.recommendedVersionId, _.recommendedVersion.id.getOrElse(-1))
  val LastUpdated           =   TimestampKey[Project](_.lastUpdated, _.lastUpdated)
  val IsVisible             =   BooleanKey[Project](_.isVisible, _.isVisible)

  val FullName              =   StringKey[User](_.fullName, _.fullName.orNull)
  val Email                 =   StringKey[User](_.email, _.email.orNull)
  val Tagline               =   StringKey[User](_.tagline, _.tagline.orNull)
  val GlobalRoles           =   TypeKey[User, List[RoleType]](_.globalRoles, _.globalRoles.toList)
  val JoinDate              =   TimestampKey[User](_.joinDate, _.joinDate.orNull)
  val AvatarUrl             =   StringKey[User](_.avatarUrl, _.avatarUrl.orNull)

  val IsReviewed            =   BooleanKey[Version](_.isReviewed, _.isReviewed)
  val ChannelId             =   IntKey[Version](_.channelId, _.channelId)

  val Color                 =   MappedTypeKey[Channel, Color](_.color, _.color)

  val Contents              =   StringKey[Page](_.contents, _.contents)

  val RoleType              =   MappedTypeKey[RoleModel, RoleType](_.roleType, _.roleType)
  val IsAccepted            =   BooleanKey[RoleModel](_.isAccepted, _.isAccepted)

  val IsResolved            =   BooleanKey[Flag](_.isResolved, _.isResolved)

  val UserId                =   IntKey[StatEntry[_]](_.userId, _.user.flatMap(_.id).getOrElse(-1))

  val Read                  =   BooleanKey[Notification](_.read, _.isRead)

}
