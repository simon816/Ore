package db.impl

import db.impl.OrePostgresDriver.api._
import db.key._
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

  val OwnerId               =   IntKey[Project](_.userId, _.ownerId)
  val OwnerName             =   StringKey[Project](_.ownerName, _.ownerName)
  val Name                  =   StringKey[Project](_.name, _.name)
  val Slug                  =   StringKey[Project](_.slug, _.slug)
  val Category              =   TypeKey[Project, Category](_.category, _.category)
  val Downloads             =   IntKey[Project](_.downloads, _.downloads)
  val Issues                =   StringKey[Project](_.issues, _.issues.orNull)
  val Source                =   StringKey[Project](_.source, _.source.orNull)
  val Description           =   StringKey[Project](_.description, _.description.orNull)
  val TopicId               =   IntKey[Project](_.topicId, _.topicId.getOrElse(-1))
  val PostId                =   IntKey[Project](_.postId, _.postId.getOrElse(-1))
  val RecommendedVersionId  =   IntKey[Project](
                                  _.recommendedVersionId, _.recommendedVersion.id.getOrElse(-1))
  val LastUpdated           =   TimestampKey[Project](_.lastUpdated, _.lastUpdated)
  val IsVisible             =   BooleanKey[Project](_.isVisible, _.isVisible)

  val Username              =   StringKey[User](_.username, _.username)
  val Email                 =   StringKey[User](_.email, _.email.orNull)
  val Tagline               =   StringKey[User](_.tagline, _.tagline.orNull)
  val GlobalRoles           =   TypeListKey[User, RoleType](
                                  _.globalRoles, _.globalRoles.toList, roleTypeListTypeMapper)
  val JoinDate              =   TimestampKey[User](_.joinDate, _.joinDate.orNull)
  val AvatarUrl             =   StringKey[User](_.avatarUrl, _.avatarUrl.orNull)

  val IsReviewed            =   BooleanKey[Version](_.isReviewed, _.isReviewed)
  val ChannelId             =   IntKey[Version](_.channelId, _.channelId)

  val Color                 =   TypeKey[Channel, Color](_.color, _.color)

  val Contents              =   StringKey[Page](_.contents, _.contents)

  val RoleType              =   TypeKey[RoleModel, RoleType](_.roleType, _.roleType)
  val IsAccepted            =   BooleanKey[RoleModel](_.isAccepted, _.isAccepted)

  val IsResolved            =   BooleanKey[Flag](_.isResolved, _.isResolved)

  val UserId                =   IntKey[StatEntry[_]](_.userId, _.user.flatMap(_.id).getOrElse(-1))

  val Read                  =   BooleanKey[Notification](_.read, _.isRead)

}
