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
  def key: String

  /**
    * Caches this.
    */
  def cache() = {
    Cache.set(this.key, this)
  }

  /**
    * Removes this from the Cache.
    */
  def free() = {
    Cache.remove(this.key)
  }

}
