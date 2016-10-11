package security.pgp

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.file.{Files, Path}

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
    * @param verifyIn InputStream to verify
    * @param keyIn    Public key InputStream
    * @return         True if verified
    */
  def verify(verifyIn: InputStream, keyIn: InputStream): Boolean = {
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
        case sigList: PGPOnePassSignatureList =>
          sig = sigList.get(0)
        case literalData: PGPLiteralData =>
          val dataIn = literalData.getInputStream
          data = IOUtils.toByteArray(dataIn)
          dataIn.close()
        case signatureList: PGPSignatureList =>
          sigList = signatureList
      }
      Logger.info(currentObject.toString)
      currentObject = factory.nextObject()
    }

    if (sig == null || data == null || sigList == null)
      return false

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
    result
  }

  def verify(path: Path, key: String): Boolean
  = verify(Files.newInputStream(path), PGPUtil.getDecoderStream(new ByteArrayInputStream(key.getBytes())))

}
