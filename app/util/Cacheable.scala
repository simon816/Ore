package util

import play.api.Play.current
import play.api.cache.Cache

/**
  * Represents something that can be added to the Cache.
  */
trait Cacheable {

  /**
    * Returns the key to this in the Cache.
    *
    * @return Key
    */
  def getKey: String

  /**
    * Caches this.
    */
  def cache() = {
    Cache.set(getKey, this)
  }

  /**
    * Removes this from the Cache.
    */
  def free() = {
    Cache.remove(getKey)
  }

}
