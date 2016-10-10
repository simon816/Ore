package security.pgp

import java.io.ByteArrayInputStream
import java.util.Date

import org.bouncycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection
import org.bouncycastle.openpgp.{PGPSignature, PGPUtil}

case class PGPPublicKeyInfo(raw: String, userName: String, email: String, id: String, createdAt: Date)

object PGPPublicKeyInfo {

  val Logger = play.api.Logger("PGP")

  def decode(raw: String) = {
    Logger.info(s"Decoding public key:\n$raw\n")

    val in = PGPUtil.getDecoderStream(new ByteArrayInputStream(raw.getBytes))
    val keyRings = new JcaPGPPublicKeyRingCollection(in)
    in.close()

    val keyRingIter = keyRings.iterator()
    var keyRingNum = 0
    var masterKey: PGPPublicKeyInfo  = null
    while (keyRingIter.hasNext) {
      keyRingNum += 1
      Logger.info("Key ring: " + keyRingNum)

      val keyRing = keyRingIter.next()
      val keyIter = keyRing.iterator()
      var keyNum = 0
      while (keyIter.hasNext) {
        keyNum += 1
        val key = keyIter.next()
        val hexId = java.lang.Long.toHexString(key.getKeyID)
        val createdAt = key.getCreationTime
        val isRevoked = key.hasRevocation
        val isEncryption = key.isEncryptionKey
        val isMaster = key.isMasterKey

        Logger.info("Key: " + keyNum)
        Logger.info("ID: " + hexId)
        Logger.info("Created at: " + createdAt)
        Logger.info("Revoked: " + isRevoked)
        Logger.info("Encryption: " + isEncryption)
        Logger.info("Master: " + isMaster)

        Logger.info("Users:")
        val userIter = key.getUserIDs
        var firstUser: String = null
        var userNum = 0
        while (userIter.hasNext) {
          val user = userIter.next()
          Logger.info("\t" + user)
          if (userNum == 0)
            firstUser = user.toString
          userNum += 1
        }

        Logger.info("Signatures:")
        val sigIter = key.getSignatures
        while (sigIter.hasNext) {
          val sig: PGPSignature = sigIter.next().asInstanceOf[PGPSignature]
          Logger.info("\tCreated at: " + sig.getCreationTime)
          Logger.info("\tCertification: " + sig.isCertification)
        }

        if (key.isMasterKey) {
          val emailIndexStart = firstUser.indexOf('<')
          val emailIndexEnd = firstUser.indexOf('>')
          if (emailIndexStart == -1 || emailIndexEnd == -1)
            throw new IllegalStateException("invalid user format?")
          val userName = firstUser.substring(0, emailIndexStart).trim()
          val email = firstUser.substring(emailIndexStart + 1, emailIndexEnd)

          Logger.info("User name: " + userName)
          Logger.info("Email: " + email)

          if (!isEncryption)
            throw new IllegalStateException("Master key is not encryption key?")
          if (isRevoked)
            throw new IllegalStateException("Key is revoked?")

          masterKey = PGPPublicKeyInfo(raw, userName, email, hexId, createdAt)
        }

      }
    }

    if (masterKey == null)
      throw new IllegalStateException("No master key found")
    masterKey
  }

}
