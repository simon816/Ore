package db.impl

import db.{Model, ModelService, ModelTable}
import db.impl.OrePostgresDriver.api._
import db.meta.TypeSetters.TypeSetter
import ore.Colors.Color
import ore.permission.role.RoleTypes.RoleType
import ore.project.Categories.Category
import ore.project.FlagReasons.FlagReason

import scala.concurrent.Future

object OreTypeSetters {

  // TODO: find better solution

  object ColorTypeSetter extends TypeSetter[Color] {
    override def apply[T <: ModelTable[M], M <: Model[_]](model: M, rep: (T) => Rep[Color], v: Color)
                                                         (implicit service: ModelService): Future[_] = service.DB.db.run {
      (for {
        m <- service.newModelAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  object RoleTypeTypeSetter extends TypeSetter[RoleType] {
    override def apply[T <: ModelTable[M], M <: Model[_]](model: M, rep: (T) => Rep[RoleType], v: RoleType)
                                                         (implicit service: ModelService): Future[_] = service.DB.db.run {
      (for {
        m <- service.newModelAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  object RoleTypeListTypeSetter extends TypeSetter[List[RoleType]] {
    override def apply[T <: ModelTable[M], M <: Model[_]](model: M, rep: (T) => Rep[List[RoleType]], v: List[RoleType])
                                                         (implicit service: ModelService): Future[_] = service.DB.db.run {
      (for {
        m <- service.newModelAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  object CategoryTypeSetter extends TypeSetter[Category] {
    override def apply[T <: ModelTable[M], M <: Model[_]](model: M, rep: (T) => Rep[Category], v: Category)
                                                         (implicit service: ModelService): Future[_] = service.DB.db.run {
      (for {
        m <- service.newModelAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  object FlagReasonTypeSetter extends TypeSetter[FlagReason] {
    override def apply[T <: ModelTable[M], M <: Model[_]](model: M, rep: (T) => Rep[FlagReason], v: FlagReason)
                                                         (implicit service: ModelService): Future[_] = service.DB.db.run {
      (for {
        m <- service.newModelAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

}
