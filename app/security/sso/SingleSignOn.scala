package security.sso

import java.math.BigInteger
import java.net.{URLDecoder, URLEncoder}
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

import ore.OreConfig
import org.apache.commons.codec.binary.Hex
import play.api.http.Status
import play.api.libs.ws.WSClient

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Manages authentication to Sponge services.
  */
trait SingleSignOn {

  val ws: WSClient
  val loginUrl: String
  val signupUrl: String
  val verifyUrl: String
  val secret: String
  val timeout: Duration

  val CharEncoding = "UTF-8"
  val Algo = "HmacSHA256"
  val Random = new SecureRandom

  /**
    * Returns a future result of whether SSO is available.
    *
    * @return True if available
    */
  def isAvailable: Boolean = Await.result(this.ws.url(this.loginUrl).get().map(_.status == Status.OK).recover {
    case e: Exception => false
  }, this.timeout)

  /**
    * Returns the login URL with a generated SSO payload to the SSO instance.
    *
    * @param returnUrl  URL to return to after authentication
    * @return           URL to SSO
    */
  def getLoginUrl(returnUrl: String): String = getUrl(returnUrl, this.loginUrl)

  /**
    * Returns the signup URL with a generated SSO payload to the SSO instance.
    *
    * @param returnUrl  URL to return to after authentication
    * @return           URL to SSO
    */
  def getSignupUrl(returnUrl: String): String = getUrl(returnUrl, this.signupUrl)

  /**
    * Returns the verify URL with a generated SSO payload to the SSO instance.
    *
    * @param returnUrl  URL to return to after authentication
    * @return           URL to SSO
    */
  def getVerifyUrl(returnUrl: String): String = getUrl(returnUrl, this.verifyUrl)

  private def getUrl(returnUrl: String, baseUrl: String) = {
    val payload = "return_sso_url=" + returnUrl + "&nonce=" + nonce
    val encoded = new String(Base64.getEncoder.encode(payload.getBytes(this.CharEncoding)))
    val urlEncoded = URLEncoder.encode(encoded, this.CharEncoding)
    val hmac = hmac_sha256(encoded.getBytes(this.CharEncoding))
    baseUrl + "?sso=" + urlEncoded + "&sig=" + hmac
  }

  /**
    * Validates an incoming payload and extracts user information. The
    * incoming payload indicates that the User was authenticated successfully
    * off-site.
    *
    * @param payload  Incoming SSO payload
    * @param sig      Incoming SSO signature
    * @return         [[SpongeUser]] if successful
    */
  def authenticate(payload: String, sig: String): Option[SpongeUser] = {
    if (!hmac_sha256(payload.getBytes(this.CharEncoding)).equals(sig))
      return None

    // decode payload
    val decoded = URLDecoder.decode(new String(Base64.getMimeDecoder.decode(payload)), this.CharEncoding)

    // extract info
    val params = decoded.split('&')
    var externalId: Int = -1
    var username: String = null
    var email: String = null

    for (param <- params) {
      val data = param.split('=')
      val value = if (data.length > 1) data(1) else null
      data(0) match {
        case "external_id" => externalId = Integer.parseInt(value)
        case "username" => username = value
        case "email" => email = value
        case _ =>
      }
    }

    if (externalId == -1 || username == null || email == null)
      return None

    Some(SpongeUser(externalId, username, email))
  }

  protected def nonce: String = new BigInteger(130, Random).toString(32)

  private def hmac_sha256(data: Array[Byte]): String = {
    val hmac = Mac.getInstance(this.Algo)
    val keySpec = new SecretKeySpec(this.secret.getBytes(this.CharEncoding), this.Algo)
    hmac.init(keySpec)
    Hex.encodeHexString(hmac.doFinal(data))
  }

}

class SpongeSingleSignOn @Inject()(override val ws: WSClient, config: OreConfig) extends SingleSignOn {

  override val loginUrl = this.config.security.getString("sso.loginUrl").get
  override val signupUrl = this.config.security.getString("sso.signupUrl").get
  override val verifyUrl = this.config.security.getString("sso.verifyUrl").get
  override val secret = this.config.security.getString("sso.secret").get
  override val timeout = this.config.security.getLong("sso.timeout").get.millis

}
