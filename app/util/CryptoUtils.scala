package util

import java.math.BigInteger
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

import org.apache.commons.lang3.RandomStringUtils

/**
  * Helper class for managing crypto functions.
  */
object CryptoUtils {

  val Random = new SecureRandom()
  val Algo = "DESede"
  val InitializationVector = RandomStringUtils.randomAlphanumeric(8).getBytes
  val KeyLength = 24

  /**
    * Generates a new random "nonce" string.
    *
    * @return
    */
  def nonce: String = new BigInteger(130, Random).toString(32)

  /**
    * Encrypts the specified string using the given secret and DESede
    * algorithm.
    *
    * @param str    String to encrypt
    * @param secret Secret key
    * @return       Encrypted string
    */
  def encrypt(str: String, secret: String): String = {
    val cipher = Cipher.getInstance(Algo)
    cipher.init(Cipher.ENCRYPT_MODE, generateKey(secret))
    Base64.getEncoder.encodeToString(cipher.doFinal(str.getBytes))
  }

  /**
    * Decrypts the specified string using the given secret and DESede algorithm.
    *
    * @param str    String to decrypt
    * @param secret Secret key
    * @return       Decrypted string
    */
  def decrypt(str: String, secret: String): String = {
    val cipher = Cipher.getInstance(Algo)
    cipher.init(Cipher.DECRYPT_MODE, generateKey(secret))
    new String(cipher.doFinal(Base64.getDecoder.decode(str.getBytes)))
  }

  private def generateKey(secret: String) = new SecretKeySpec(secret.getBytes.slice(0, KeyLength), Algo)

}
