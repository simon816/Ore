package ore

import play.api.cache.CacheApi

/**
  * Represents something that can be added to the Cache.
  */
trait Cacheable {

  def cacheApi: CacheApi

  /**
    * Returns the key to this in the Cache.
    *
    * @return Key
    */
  def key: String

  /**
    * Caches this.
    */
  def cache() = this.cacheApi.set(this.key, this)

  /**
    * Removes this from the Cache.
    */
  def free() = this.cacheApi.remove(this.key)

}
