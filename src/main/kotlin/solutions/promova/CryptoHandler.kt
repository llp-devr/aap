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


class CryptoHandler(
    private val pkcs12File: String = loadFromEnv("PKCS12_FILE"),
    private val pkcs12Password: String = loadFromEnv("PKCS12_PASSWORD")
) {
    companion object {
        private fun loadFromEnv(envVar: String): String =
            System.getenv(envVar) ?: throw IllegalArgumentException("$envVar is not set")
    }

    private val publicKey: PublicKey
    private val privateKey: PrivateKey
    private val certificateList = mutableListOf<X509Certificate>()

    init {
        // Load the PKCS12 file into a KeyStore instance
        val keystore = KeyStore.getInstance("PKCS12").apply {
            load(FileInputStream(pkcs12File), pkcs12Password.toCharArray())
        }

        // Get the next alias from the keystore
        val alias = keystore.aliases().nextElement()

        /* Get the private key from the keystore using the obtained alias
           Note that the keystore password is required to access the private key */
        privateKey = keystore.getKey(alias, pkcs12Password.toCharArray()) as PrivateKey

        /* Retrieve all certificates in the certificate chain for the alias from the keystore
           Filter the certificates to only keep those that are instances of X509Certificate
           and add them to the certificateList */
        certificateList.addAll(
            keystore.getCertificateChain(alias).filterIsInstance<X509Certificate>()
        )

        // Set the public key as the public key of the first certificate in the certificateList
        publicKey = certificateList.first().publicKey
    }

    fun certChain(): String =
        /* Get a CertificateFactory instance for X.509 certificates
           Generate a certificate path from the certificateList
           Encode the certificate path into a byte array
           Base64 encode the byte array */
        CertificateFactory.getInstance("X.509")
            .generateCertPath(certificateList).encoded.let(Base64.getEncoder()::encode)
            /* Write the Base64-encoded byte array to a ByteArrayOutputStream
               Note: the let function here is used to create a scope where baos
               refers to the Base64-encoded byte array */
            .let { baos -> ByteArrayOutputStream().apply { write(baos) } }
            // Convert the ByteArrayOutputStream to a byte array
            .toByteArray()
            // Convert the byte array to a string
            .let(::String)

    fun sign(message: String): String =
        /* Get a MessageDigest instance for the MD5 algorithm
           Compute the MD5 digest for the given message */
        MessageDigest.getInstance("MD5").digest(message.toByteArray())
            // With the message digest (it), do the following:
            .let { messageDigest ->
                /* Get a Signature instance for the MD5withRSA algorithm
                   Initialize the Signature instance for signing with the privateKey
                   Update the data to be signed with the messageDigest */
                Signature.getInstance("MD5withRSA").apply {
                    initSign(this@CryptoHandler.privateKey)
                    update(messageDigest)
                    // Sign all the updated data and return the signature bytes
                }.sign()
            }
            // Encode the signature bytes into a Base64 string and return this string
            .let(Base64.getEncoder()::encodeToString)
}