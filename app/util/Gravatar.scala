package util

import java.security.MessageDigest

import org.apache.commons.codec.binary.Hex

/**
  * Helper class for retrieving user avatars from the Gravatar service.
  */
object Gravatar {

  /**
    * Gravatar service URL
    */
  val URL = "https://www.gravatar.com/avatar/"

  /**
    * Returns the "Gravatar" avatar for the specified email.
    *
    * @param email  Email to get avatar for
    * @return       Avatar of email, or default avatar if not found
    */
  def get(email: String): String = {
    val hash = Hex.encodeHexString(MessageDigest.getInstance("MD5").digest(email.trim().toLowerCase.getBytes("UTF-8")))
    this.URL + hash
  }

}
