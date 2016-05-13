package forums

import java.math.BigInteger
import java.net.{URLDecoder, URLEncoder}
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import db.ModelService
import db.impl.service.UserBase
import models.user.User
import org.apache.commons.codec.binary.Hex

/**
  * Handles single-sign-on authentication to a Discourse forum.
  */
trait DiscourseSSO {

  protected val api: DiscourseApi
  protected val users: UserBase
  protected val url: String
  protected val secret: String

  protected val charEncoding = "UTF-8"
  protected val random = new SecureRandom
  protected val algo = "HmacSHA256"

  protected def nonce: String = new BigInteger(130, this.random).toString(32)

  protected def hmac_sha256(data: Array[Byte]): String = {
    val hmac = Mac.getInstance(this.algo)
    val keySpec = new SecretKeySpec(this.secret.getBytes(this.charEncoding), this.algo)
    hmac.init(keySpec)
    Hex.encodeHexString(hmac.doFinal(data))
  }

  /**
    * Returns the redirect to the Discourse forum to perform authentication.
    *
    * @param returnUrl  URL to tell Discourse to return to
    * @return           Redirect URL
    */
  def toForums(returnUrl: String): String = {
    val payload = "require_validation=true&return_sso_url=" + returnUrl + "&nonce=" + nonce
    val encoded = new String(Base64.getEncoder.encode(payload.getBytes(this.charEncoding)))
    val urlEncoded = URLEncoder.encode(encoded, this.charEncoding)
    val hmac = hmac_sha256(encoded.getBytes(this.charEncoding))
    this.url + "?sso=" + urlEncoded + "&sig=" + hmac
  }

  /**
    * Verifies an incoming payload from Discourse SSO and extracts the
    * necessary data.
    *
    * @param sso  SSO payload
    * @param sig  Signature to verify
    * @return     User data
    */
  def authenticate(sso: String, sig: String)(implicit service: ModelService): User = {
    // check sig
    val hmac = hmac_sha256(sso.getBytes(this.charEncoding))
    if (!hmac.equals(sig)) throw new Exception("Invalid signature.")

    // decode payload
    val decoded = URLDecoder.decode(new String(Base64.getMimeDecoder.decode(sso)), this.charEncoding)

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
        case other => ;
      }
    }

    if (externalId == -1) throw new IllegalStateException("id not found")

    // Send another request to get more info to fill the user with
    users.withName(username).get.fill(service.await(api.fetchUser(username)).get.get.copy(
      id = Some(externalId),
      _name = Some(name),
      _username = username,
      _email = Some(email)
    ))
  }

}
