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
import ore.user.Prompts.Prompt

/**
  * Collection of String keys used for table bindings within Models.
  */
object ModelKeys {

  val Name                  =   new StringKey[Named](_.name, _.name)
  val Downloads             =   new IntKey[Downloadable](_.downloads, _.downloadCount)
  val Description           =   new StringKey[Describable](_.description, _.description.orNull)

  val OwnerId               =   new IntKey[Project](_.userId, _.ownerId)
  val OwnerName             =   new StringKey[Project](_.ownerName, _.ownerName)
  val Slug                  =   new StringKey[Project](_.slug, _.slug)
  val Category              =   new MappedTypeKey[Project, Category](_.category, _.category)
  val Stars                 =   new IntKey[Project](_.stars, _.starCount)
  val Views                 =   new IntKey[Project](_.views, _.viewCount)
  val Issues                =   new StringKey[Project](_.issues, _.issues.orNull)
  val Source                =   new StringKey[Project](_.source, _.source.orNull)
  val TopicId               =   new IntKey[Project](_.topicId, _.topicId)
  val PostId                =   new IntKey[Project](_.postId, _.postId)
  val IsTopicDirty          =   new BooleanKey[Project](_.isTopicDirty, _.isTopicDirty)
  val RecommendedVersionId  =   new IntKey[Project](
                                  _.recommendedVersionId, _.recommendedVersion.id.getOrElse(-1))
  val LastUpdated           =   new TimestampKey[Project](_.lastUpdated, _.lastUpdated)
  val IsVisible             =   new BooleanKey[Project](_.isVisible, _.isVisible)

  val FullName              =   new StringKey[User](_.fullName, _.fullName.orNull)
  val Email                 =   new StringKey[User](_.email, _.email.orNull)
  val PGPPubKey             =   new StringKey[User](_.pgpPubKey, _.pgpPubKey.orNull)
  val LastPGPPubKeyUpdate   =   new TimestampKey[User](_.lastPgpPubKeyUpdate, _.lastPgpPubKeyUpdate.orNull)
  val Tagline               =   new StringKey[User](_.tagline, _.tagline.orNull)
  val GlobalRoles           =   new Key[User, List[RoleType]](_.globalRoles, _.globalRoles.toList)
  val JoinDate              =   new TimestampKey[User](_.joinDate, _.joinDate.orNull)
  val AvatarUrl             =   new StringKey[User](_.avatarUrl, _.avatarUrl.orNull)
  val ReadPrompts           =   new Key[User, List[Prompt]](_.readPrompts, _.readPrompts.toList)

  val IsReviewed            =   new BooleanKey[Version](_.isReviewed, _.isReviewed)
  val ChannelId             =   new IntKey[Version](_.channelId, _.channelId)

  val Color                 =   new MappedTypeKey[Channel, Color](_.color, _.color)

  val Contents              =   new StringKey[Page](_.contents, _.contents)

  val RoleType              =   new MappedTypeKey[RoleModel, RoleType](_.roleType, _.roleType)
  val IsAccepted            =   new BooleanKey[RoleModel](_.isAccepted, _.isAccepted)

  val IsResolved            =   new BooleanKey[Flag](_.isResolved, _.isResolved)

  val UserId                =   new IntKey[StatEntry[_]](_.userId, _.user.flatMap(_.id).getOrElse(-1))

  val Read                  =   new BooleanKey[Notification](_.read, _.isRead)

}
