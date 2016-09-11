package util

import java.math.BigInteger
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

import org.apache.commons.lang3.RandomStringUtils

object CryptoUtils {

  final val Random = new SecureRandom()
  final val Algo = "DESede"
  final val InitializationVector = RandomStringUtils.randomAlphanumeric(8).getBytes
  final val KeyLength = 24

  /**
    * Generates a new random "nonce" string.
    *
    * @return
    */
  def nonce: String = new BigInteger(130, Random).toString(32)

  def encrypt(str: String, secret: String): String = {
    val cipher = Cipher.getInstance(Algo)
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secret.getBytes.slice(0, KeyLength), Algo))
    Base64.getEncoder.encodeToString(cipher.doFinal(str.getBytes))
  }

  def decrypt(str: String, secret: String): String = {
    val cipher = Cipher.getInstance(Algo)
    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secret.getBytes.slice(0, KeyLength), Algo))
    new String(cipher.doFinal(Base64.getDecoder.decode(str.getBytes)))
  }

}
