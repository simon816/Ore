package util

import java.math.BigInteger
import java.net.{URLDecoder, URLEncoder}
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import org.apache.commons.codec.binary.Hex
import play.api.Play.{current => my}

object DiscourseSSO {

  private val url = my.configuration.getString("discourse.sso.url").get
  private val secret = my.configuration.getString("discourse.sso.secret").get.getBytes("UTF-8")
  private val returnUrl = my.configuration.getString("application.baseUrl").get + "/login"
  private val random = new SecureRandom
  private val algo = "HmacSHA256"

  private def nonce: String = {
    new BigInteger(130, random).toString(32)
  }

  private def hmac_sha256(data: Array[Byte]): String = {
    val hmac = Mac.getInstance(this.algo)
    val keySpec = new SecretKeySpec(this.secret, this.algo)
    hmac.init(keySpec)
    Hex.encodeHexString(hmac.doFinal(data))
  }

  def getRedirect: String = {
    val payload = "require_validation=true&return_sso_url=" + this.returnUrl + "&nonce=" + nonce
    val encoded = new String(Base64.getEncoder.encode(payload.getBytes("UTF-8")))
    val urlEncoded = URLEncoder.encode(encoded, "UTF-8")
    val hmac = hmac_sha256(encoded.getBytes("UTF-8"))
    this.url + "?sso=" + urlEncoded + "&sig=" + hmac
  }

  def authenticate(sso: String, sig: String): (Int, String, String, String) = {
    // check sig
    val hmac = hmac_sha256(sso.getBytes("UTF-8"))
    if (!hmac.equals(sig)) {
      throw new Exception("Invalid signature.")
    }

    // decode payload
    val decoded = URLDecoder.decode(new String(Base64.getMimeDecoder.decode(sso)), "UTF-8")

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
    (externalId, name, username, email)
  }

}
