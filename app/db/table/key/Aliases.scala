package db.table.key

import java.sql.{Blob, Clob, Date, Time, Timestamp}
import java.util.UUID

import db.Model

/**
  * Type aliases to mixin to make Key definition more manageable.
  */
trait Aliases {

  type TimestampKey[M <: Model] = db.table.key.Key[M, Timestamp]
  type TimeKey[M <: Model] = db.table.key.Key[M, Time]
  type DateKey[M <: Model] = db.table.key.Key[M, Date]
  type UUIDKey[M <: Model] = db.table.key.Key[M, UUID]
  type ClobKey[M <: Model] = db.table.key.Key[M, Clob]
  type BlobKey[M <: Model] = db.table.key.Key[M, Blob]
  type StringKey[M <: Model] = db.table.key.Key[M, String]
  type BigDecimalKey[M <: Model] = db.table.key.Key[M, BigDecimal]
  type LongKey[M <: Model] = db.table.key.Key[M, Long]
  type DoubleKey[M <: Model] = db.table.key.Key[M, Double]
  type FloatKey[M <: Model] = db.table.key.Key[M, Float]
  type IntKey[M <: Model] = db.table.key.Key[M, Int]
  type ShortKey[M <: Model] = db.table.key.Key[M, Short]
  type CharKey[M <: Model] = db.table.key.Key[M, Char]
  type ByteKey[M <: Model] = db.table.key.Key[M, Short]
  type ByteArrayKey[M <: Model] = db.table.key.Key[M, Array[Byte]]
  type BooleanKey[M <: Model] = db.table.key.Key[M, Boolean]

}
