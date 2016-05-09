package util

import play.api.cache.CacheApi

/**
  * Represents something that can be added to the Cache.
  */
trait Cacheable {

  val cacheApi: CacheApi

  /**
    * Returns the key to this in the Cache.
    *
    * @return Key
    */
  def key: String

  /**
    * Caches this.
    */
  def cache() = cacheApi.set(this.key, this)

  /**
    * Removes this from the Cache.
    */
  def free() = cacheApi.remove(this.key)

}
