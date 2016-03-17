package util

import java.security.MessageDigest

import org.apache.commons.codec.binary.Hex

object Gravatar {

  val URL = "http://www.gravatar.com/avatar/"

  /**
    * Returns the "Gravatar" avatar for the specified email.
    *
    * @param email  Email to get avatar for
    * @param size   Size of avatar to get
    * @return       Avatar of email, or default avatar if not found
    */
  def getAvatar(email: String, size: Int): String = {
    val hash = Hex.encodeHexString(MessageDigest.getInstance("MD5").digest(email.trim().toLowerCase.getBytes("UTF-8")))
    this.URL + hash + "?s=" + size
  }

}
