package util

import java.security.MessageDigest

import org.apache.commons.codec.binary.Hex

object Gravatar {

  private val url = "http://www.gravatar.com/avatar/"

  def getAvatar(email: String): String = {
    val hash = Hex.encodeHexString(MessageDigest.getInstance("MD5").digest(email.trim().toLowerCase.getBytes("UTF-8")))
    this.url + hash + "?s=45"
  }

}
