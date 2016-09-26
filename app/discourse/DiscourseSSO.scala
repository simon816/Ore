package discourse

import java.math.BigInteger
import java.net.{URLDecoder, URLEncoder}
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import discourse.model.DiscourseUser
import org.apache.commons.codec.binary.Hex

/**
  * Handles single-sign-on authentication to a Discourse forum.
  */
trait DiscourseSSO {

  val ssoUrl: String
  val secret: String

  val CharEncoding = "UTF-8"
  val Algo = "HmacSHA256"
  val Random = new SecureRandom()

  /**
    * Returns the redirect to the Discourse forum to perform authentication.
    *
    * @param returnUrl  URL to tell Discourse to return to
    * @return           Redirect URL
    */
  def toForums(returnUrl: String): String = {
    val payload = "require_validation=true&return_sso_url=" + returnUrl + "&nonce=" + nonce
    val encoded = new String(Base64.getEncoder.encode(payload.getBytes(this.CharEncoding)))
    val urlEncoded = URLEncoder.encode(encoded, this.CharEncoding)
    val hmac = hmac_sha256(encoded.getBytes(this.CharEncoding))
    this.ssoUrl + "?sso=" + urlEncoded + "&sig=" + hmac
  }

  /**
    * Verifies an incoming payload from Discourse SSO and extracts the
    * necessary data.
    *
    * @param sso  SSO payload
    * @param sig  Signature to verify
    * @return     User data
    */
  def authenticate(sso: String, sig: String): DiscourseUser = {
    // check sig
    val hmac = hmac_sha256(sso.getBytes(this.CharEncoding))
    if (!hmac.equals(sig))
      throw new IllegalArgumentException("Invalid signature.")

    // decode payload
    val decoded = URLDecoder.decode(new String(Base64.getMimeDecoder.decode(sso)), this.CharEncoding)

    // extract info
    val params = decoded.split('&')
    var externalId: Int = -1
    var name: String = null
    var username: String = null
    var email: String = null

    for (param <- params) {
      val data = param.split('=')
      val value = if (data.length > 1) data(1) else null
      data(0) match {
        case "external_id" => externalId = Integer.parseInt(value)
        case "name" => name = value
        case "username" => username = value
        case "email" => email = value
        case _ =>
      }
    }

    if (externalId == -1)
      throw new IllegalStateException("id not found")
    if (username == null)
      throw new IllegalStateException("username not found")

    DiscourseUser(
      id = externalId,
      username = username,
      fullName = Option(name),
      email = Option(email)
    )
  }

  /**
    * Generates a new random "nonce" string.
    *
    * @return
    */
  protected def nonce: String = new BigInteger(130, Random).toString(32)

  private def hmac_sha256(data: Array[Byte]): String = {
    val hmac = Mac.getInstance(this.Algo)
    val keySpec = new SecretKeySpec(this.secret.getBytes(this.CharEncoding), this.Algo)
    hmac.init(keySpec)
    Hex.encodeHexString(hmac.doFinal(data))
  }

}
