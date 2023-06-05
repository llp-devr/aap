package solutions.promova

import java.io.ByteArrayOutputStream
import java.io.FileInputStream

import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature

import java.util.Base64


class CryptoHandler {
    private val pkcs12File: String =
        System.getenv("PKCS12_FILE") ?: throw IllegalArgumentException("PKCS12_FILE is not set")
    private val pkcs12Password: String =
        System.getenv("PKCS12_PASSWORD") ?: throw IllegalArgumentException("PKCS12_PASSWORD is not set")

    private val publicKey: PublicKey
    private val privateKey: PrivateKey
    private val certificateList = mutableListOf<X509Certificate>()

    init {
        // Load the PKCS12 file
        val keystore = KeyStore.getInstance("PKCS12")
        val keystorePassword = pkcs12Password.toCharArray()
        val keystoreFile = FileInputStream(pkcs12File)
        keystore.load(keystoreFile, keystorePassword)

        // Get the PublicKey from the KeyStore
        val alias = keystore.aliases().nextElement()
        privateKey = keystore.getKey(alias, keystorePassword) as PrivateKey

        val certificateChain = keystore.getCertificateChain(alias)
        for (cert in certificateChain) {
            if (cert is X509Certificate) {
                certificateList.add(cert)
            }
        }

        publicKey = certificateList.first().publicKey
    }

    fun certChain(): String {
        val certFactory = CertificateFactory.getInstance("X.509")
        val certPath = certFactory.generateCertPath(certificateList)

        val encodedCertChain = certPath.encoded

        val baos = ByteArrayOutputStream()
        val encodedBytes = Base64.getEncoder().encode(encodedCertChain)
        baos.write(encodedBytes)
        return String(baos.toByteArray())
    }

    fun sign(message: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val messageDigest = digest.digest(message.toByteArray())

        val signature = Signature.getInstance("MD5withRSA")
        signature.initSign(privateKey)
        signature.update(messageDigest)
        val signatureBytes = signature.sign()

        return Base64.getEncoder().encodeToString(signatureBytes)
    }
}