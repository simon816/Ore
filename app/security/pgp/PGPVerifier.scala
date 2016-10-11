package security.pgp

import java.io.{ByteArrayInputStream, InputStream, OutputStream}
import java.nio.file.Files._
import java.nio.file.Path

import com.google.common.base.Preconditions._
import org.apache.commons.io.IOUtils
import org.bouncycastle.openpgp._
import org.bouncycastle.openpgp.jcajce.{JcaPGPObjectFactory, JcaPGPPublicKeyRingCollection}
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider

/**
  * Verifies data within the PGP ecosystem.
  */
class PGPVerifier {

  val Logger = PGPPublicKeyInfo.Logger

  /**
    * Verifies the specified [[InputStream]] against the specified public key
    * [[InputStream]]. Only the first signature will be checked.
    *
    * When using GPG one should sign uploaded files with the following command:
    *
    * {{{
    *   gpg --output plugin-signed.jar --sign plugin.jar
    * }}}
    *
    * Where "plugin.jar" is your original plugin file and "plugin-signed.jar" is your output file.
    *
    * @see https://www.gnupg.org/gph/en/manual/x135.html
    * @param verifyIn InputStream to verify
    * @param keyIn    Public key InputStream
    * @return         True if verified
    */
  def verifyAndDecrypt(verifyIn: InputStream, out: OutputStream, keyIn: InputStream): Boolean = {
    checkNotNull(verifyIn, "input is null", "")
    checkNotNull(out, "output is null", "")
    checkNotNull(keyIn, "key input is null", "")

    // Retrieve the necessary data
    val in = PGPUtil.getDecoderStream(verifyIn)
    var factory = new JcaPGPObjectFactory(in)
    var sig: PGPOnePassSignature = null
    var data: Array[Byte] = null
    var sigList: PGPSignatureList = null

    var currentObject: Object = factory.nextObject()
    while ((sig == null || data == null || sigList == null) && currentObject != null) {
      // Normally the packets will be read in this order
      currentObject match {
        case compressedData: PGPCompressedData =>
          // Decompress and continue
          factory = new JcaPGPObjectFactory(compressedData.getDataStream)
        case onePassSigList: PGPOnePassSignatureList =>
          if (onePassSigList.isEmpty) {
            Logger.info("Empty one pass signature list.")
            return false
          }
          sig = onePassSigList.get(0)
        case literalData: PGPLiteralData =>
          val dataIn = literalData.getInputStream
          data = IOUtils.toByteArray(dataIn)
          dataIn.close()
        case signatureList: PGPSignatureList =>
          if (signatureList.isEmpty) {
            Logger.info("Empty signature list.")
            return false
          }
          sigList = signatureList
        case _ =>
      }
      Logger.info(currentObject.toString)
      currentObject = factory.nextObject()
    }

    if (sig == null || data == null || sigList == null) {
      Logger.info("Incomplete packet data.")
      return false
    }

    Logger.info("Signature Key ID: " + java.lang.Long.toHexString(sig.getKeyID))

    // Verify against public key
    val keyRings = new JcaPGPPublicKeyRingCollection(keyIn)
    val pubKey = keyRings.getPublicKey(sig.getKeyID)
    if (pubKey == null)
      return false

    sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), pubKey)
    sig.update(data)
    val result = sig.verify(sigList.get(0))
    Logger.info("Verified: " + result)
    if (result) {
      out.write(data)
      out.close()
    }
    result
  }

  def verifyAndDecrypt(in: Path, out: Path, key: String): Boolean = {
    checkNotNull(in, "input is null", "")
    checkNotNull(out, "output is null", "")
    checkNotNull(key, "key is null", "")
    checkArgument(exists(in), "input does not exist", "")
    if (notExists(out)) {
      createDirectories(out.getParent)
      createFile(out)
    }
    val inStream = newInputStream(in)
    val outStream = newOutputStream(out)
    val keyStream = PGPUtil.getDecoderStream(new ByteArrayInputStream(key.getBytes))
    verifyAndDecrypt(inStream, outStream, keyStream)
  }

}
