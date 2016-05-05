package util

import db.driver.OrePostgresDriver.api._
import java.text.SimpleDateFormat
import java.util.Date

import util.Conf._

/**
  * Helper class for handling User input.
  */
object StringUtils {

  val DateFormat = new SimpleDateFormat(OreConf.getString("date-format").get)

  /**
    * Formats the specified date into the standard application form.
    *
    * @param date Date to format
    * @return     Standard formatted date
    */
  def prettyDate(date: Date) = DateFormat.format(date)

  /**
    * Returns a URL readable string from the specified string.
    *
    * @param str  String to create slug for
    * @return     Slug of string
    */
  def slugify(str: String): String = {
    compact(str).replace(' ', '-')
  }

  /**
    * Returns the specified String with all consecutive spaces removed.
    *
    * @param str  String to compact
    * @return     Compacted string
    */
  def compact(str: String): String = {
    str.trim.replaceAll(" +", " ")
  }

  /**
    * Returns null if the specified String is empty, returns the trimmed string
    * otherwise.
    *
    * @param str String to check
    * @return Null if empty, trimmed otherwise
    */
  def nullIfEmpty(str: String): String = {
    val trimmed = str.trim
    if (trimmed.nonEmpty) trimmed else null
  }

  /**
    * Compares a Rep[String] to a String after converting them to lower case.
    *
    * @param str1 String 1
    * @param str2 String 2
    * @return     Result
    */
  def equalsIgnoreCase[T <: Table[_]](str1: T => Rep[String], str2: String): T => Rep[Boolean]
  = str1(_).toLowerCase === str2

}
