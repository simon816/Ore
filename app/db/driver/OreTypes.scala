package db.driver

import db.driver.OrePostgresDriver.api._
import db.model.annotation.TypeSetters
import db.model.annotation.TypeSetters.TypeSetter
import db.model.{Model, ModelTable}
import db.query.ModelQueries
import ore.Colors.Color
import ore.permission.role.RoleTypes.RoleType
import ore.project.Categories.Category
import ore.project.FlagReasons.FlagReason

trait OreTypes {

  // TODO: modularize with annotations

  TypeSetters.register[Color](classOf[Color], new TypeSetter[Color] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[Color], v: Color)
    = Seq(ModelQueries.setColor[T, M](model, rep, v))
  })

  TypeSetters.register[RoleType](classOf[RoleType], new TypeSetter[RoleType] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[RoleType], v: RoleType)
    = Seq(ModelQueries.setRoleType[T, M](model, rep, v))
  })

  TypeSetters.register[List[RoleType]](classOf[List[RoleType]], new TypeSetter[List[RoleType]] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[List[RoleType]], v: List[RoleType])
    = Seq(ModelQueries.setRoleTypeList[T, M](model, rep, v))
  })

  TypeSetters.register[Category](classOf[Category], new TypeSetter[Category] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[Category], v: Category)
    = Seq(ModelQueries.setCategory[T, M](model, rep, v))
  })

  TypeSetters.register[FlagReason](classOf[FlagReason], new TypeSetter[FlagReason] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[FlagReason], v: FlagReason)
    = Seq(ModelQueries.setFlagReason[T, M](model, rep, v))
  })

}
