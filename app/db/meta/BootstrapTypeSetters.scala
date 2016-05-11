package db.meta

import java.sql.Timestamp

import db.impl.OrePostgresDriver.api._
import db.{Model, ModelService, ModelTable}

/**
  * A collection of basic TypeSetters.
  */
object BootstrapTypeSetters {

  /**
    * Utility function to retrieve a Rep[A] of the specified name from a
    * ModelTable using reflection.
    *
    * @param name   Rep name
    * @param table  Table
    * @tparam A     Rep type
    * @return       Rep from table
    */
  def getRep[A](name: String, table: ModelTable[_]): Rep[A]
  = table.getClass.getMethod(name).invoke(table).asInstanceOf[Rep[A]]

  case object IntTypeSetter extends TypeSetter[Int] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[Int], v: Int)
                                                (implicit service: ModelService) = service.DB.db.run {
      (for {
        m <- service.newAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  case object StringTypeSetter extends TypeSetter[String] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[String], v: String)
                                                (implicit service: ModelService) = service.DB.db.run {
      (for {
        m <- service.newAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  case object BooleanTypeSetter extends TypeSetter[Boolean] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[Boolean], v: Boolean)
                                                (implicit service: ModelService) = service.DB.db.run {
      (for {
        m <- service.newAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  case object TimestampTypeSetter extends TypeSetter[Timestamp] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[Timestamp], v: Timestamp)
                                                (implicit service: ModelService) = service.DB.db.run {
      (for {
        m <- service.newAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  case object IntListTypeSetter extends TypeSetter[List[Int]] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[List[Int]], v: List[Int])
                                                (implicit service: ModelService) = service.DB.db.run {
      (for {
        m <- service.newAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  case object StringListTypeSetter extends TypeSetter[List[String]] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[List[String]], v: List[String])
                                                (implicit service: ModelService) = service.DB.db.run {
      (for {
        m <- service.newAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  case object BooleanListTypeSetter extends TypeSetter[List[Boolean]] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[List[Boolean]], v: List[Boolean])
                                                (implicit service: ModelService) = service.DB.db.run {
      (for {
        m <- service.newAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

  case object TimestampListTypeSetter extends TypeSetter[List[Timestamp]] {
    def apply[T <: ModelTable[M], M <: Model](model: M, rep: T => Rep[List[Timestamp]], v: List[Timestamp])
                                                (implicit service: ModelService) = service.DB.db.run {
      (for {
        m <- service.newAction[T, M](model.getClass)
        if m.id === model.id.get
      } yield rep(m)).update(v)
    }
  }

}
