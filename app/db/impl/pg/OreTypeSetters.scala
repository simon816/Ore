package db.impl.pg

import db.meta.TypeSetter
import db.{Model, ModelService, ModelTable}
import db.impl.pg.OrePostgresDriver.api._
import ore.Colors.Color
import ore.NotificationTypes.NotificationType
import ore.permission.role.RoleTypes.RoleType
import ore.project.Categories.Category
import ore.project.FlagReasons.FlagReason

import scala.concurrent.Future

object OreTypeSetters {

  // TODO: find better solution

  object ColorTypeSetter extends TypeSetter[Color] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: (T) => Rep[Color], v: Color)
                                             (implicit service: ModelService): Future[_] = service.DB.db.run {
      (for {
        m <- service.newAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  object RoleTypeTypeSetter extends TypeSetter[RoleType] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: (T) => Rep[RoleType], v: RoleType)
                                             (implicit service: ModelService): Future[_] = service.DB.db.run {
      (for {
        m <- service.newAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  object RoleTypeListTypeSetter extends TypeSetter[List[RoleType]] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: (T) => Rep[List[RoleType]], v: List[RoleType])
                                             (implicit service: ModelService): Future[_] = service.DB.db.run {
      (for {
        m <- service.newAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  object CategoryTypeSetter extends TypeSetter[Category] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: (T) => Rep[Category], v: Category)
                                             (implicit service: ModelService): Future[_] = service.DB.db.run {
      (for {
        m <- service.newAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  object FlagReasonTypeSetter extends TypeSetter[FlagReason] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: (T) => Rep[FlagReason], v: FlagReason)
                                             (implicit service: ModelService): Future[_] = service.DB.db.run {
      (for {
        m <- service.newAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  object NotificationTypeTypeSetter extends TypeSetter[NotificationType] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: (T) => Rep[NotificationType], v: NotificationType)
                                             (implicit service: ModelService): Future[_] = service.DB.db.run {
      (for {
        m <- service.newAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

}
