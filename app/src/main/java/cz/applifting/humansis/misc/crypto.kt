package cz.applifting.humansis.misc

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.security.*
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.security.auth.x500.X500Principal
import kotlin.random.Random

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 19, August, 2019
 */

const val KEY_PROVIDER = "AndroidKeyStore"
const val SP_AES_IV_KEY = "db-aes"

// TODO measure these functions and check which - if not all - should be done on background thread

suspend fun hashAndSaltPassword(salt: String, password: String): String {
    return withContext(Dispatchers.Default) {
        val salted = "$password{$salt}".toByteArray()
        var digest = hashSHA512(salted)

        for (i in 1..4999) {
            digest = hashSHA512(digest.plus(salted))
        }

        Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}

@SuppressLint("SimpleDateFormat")
fun generateXWSSEHeader(username: String, saltedPassword: String, test: Boolean): String {
    val nonce = generateNonce()

    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    val createdAt = sdf.format(Date())

    val digest = generateDigest(saltedPassword, nonce, createdAt)
    val nonce64 = Base64.encodeToString(nonce.toByteArray(), Base64.NO_WRAP)

    if (test) {
        return "UsernameToken Username=\"$username\", PasswordDigest=\"$digest\", Nonce=\"badNone\", Created=\"$createdAt\""
    }

    return "UsernameToken Username=\"$username\", PasswordDigest=\"$digest\", Nonce=\"$nonce64\", Created=\"$createdAt\""
}

fun generateDigest(saltedPassword: String, nonce: String, created: String): String {
    val mix = nonce + created + saltedPassword
    return hashSHA1(mix)
}

fun hashSHA512(input: ByteArray, iterations: Int = 1): ByteArray {
    val digestor = MessageDigest.getInstance("SHA-512")

    var result = input
    for (i in 0 until iterations) {
        result = digestor.digest(result)
    }

    return result
}

fun encryptUsingKeyStoreKey(secret: ByteArray, keyAlias: String, context: Context, sp: SharedPreferences): ByteArray {
    val keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        doEncryptionUsingAES(secret, keyAlias, keyStore, sp)
    } else {
        doEncryptionUsingRSA(secret, keyAlias, keyStore, context)
    }
}

fun decryptUsingKeyStoreKey(secret: ByteArray, keyAlias: String, sp: SharedPreferences): ByteArray? {
    val keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)

    if (!keyStore.containsAlias(keyAlias)) {
        return null
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        doDecryptionUsingAES(secret, keyAlias, keyStore, sp)
    } else {
        doDecryptionUsingRSA(secret, keyAlias, keyStore)
    }
}

fun base64encode(value: ByteArray): String = Base64.encodeToString(value, Base64.NO_WRAP)

fun base64decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

fun generateNonce(): String {
    val nonceChars = "0123456789abcdef"
    val nonce = StringBuilder()

    for (i in 0..15) {
        nonce.append(nonceChars[Random.nextInt(nonceChars.length)])
    }

    return nonce.toString()
}

// ----------------------------------------------------------------------------------------------------------------------------- //

private fun doEncryptionUsingRSA(secret: ByteArray, keyAlias: String, keyStore: KeyStore, context: Context): ByteArray {
    if (!keyStore.containsAlias(keyAlias)) {
        generateKeyStoreRSAKey(keyAlias, context)
    }

    val keyEntry = keyStore.getEntry(keyAlias, null) as KeyStore.PrivateKeyEntry
    return encryptRSA(secret, keyEntry.certificate.publicKey)
}

private fun doDecryptionUsingRSA(secret: ByteArray, keyAlias: String, keyStore: KeyStore): ByteArray {
    val privateKey = keyStore.getKey(keyAlias, null) as PrivateKey
    return decryptRSA(secret, privateKey)
}

@RequiresApi(Build.VERSION_CODES.M)
private fun doEncryptionUsingAES(secret: ByteArray, keyAlias: String, keyStore: KeyStore, sp: SharedPreferences): ByteArray {
    if (!keyStore.containsAlias(keyAlias)) {
        generateAESKey(keyAlias)
    }

    val keyEntry = keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry
    return encryptAES(secret, keyEntry.secretKey, sp)
}

private fun doDecryptionUsingAES(secret: ByteArray, keyAlias: String, keyStore: KeyStore, sp: SharedPreferences): ByteArray {
    val key = keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry
    return decryptAES(secret, key.secretKey, sp)
}

private fun hashSHA1(s: String): String {
    return Base64.encodeToString(MessageDigest.getInstance("SHA-1").digest(s.toByteArray()), Base64.NO_WRAP)
}

private fun encryptRSA(secret: ByteArray, publicKey: PublicKey): ByteArray {
    val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    cipher.init(Cipher.ENCRYPT_MODE, publicKey)
    return cipher.doFinal(secret)
}

private fun decryptRSA(secret: ByteArray, privateKey: PrivateKey): ByteArray {
    val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    cipher.init(Cipher.DECRYPT_MODE, privateKey, cipher.parameters)
    return cipher.doFinal(secret)
}

private fun generateKeyStoreRSAKey(keyAlias: String, context: Context) {
    val kpg = KeyPairGenerator.getInstance(
        "RSA", KEY_PROVIDER
    )

    val startDate = Calendar.getInstance()
    val endDate = Calendar.getInstance()
    endDate.add(Calendar.YEAR, 30)

    // Using deprecated method to support API below 23
    val keyGeneratorSpec = KeyPairGeneratorSpec
        .Builder(context)
        .setAlias(keyAlias)
        .setSubject(X500Principal("CN=$keyAlias"))
        .setSerialNumber(BigInteger.TEN)
        .setStartDate(startDate.time)
        .setEndDate(endDate.time)
        .build()

    kpg.initialize(keyGeneratorSpec)

    val keys = kpg.genKeyPair()
}

private fun encryptAES(secret: ByteArray, key: SecretKey, sp: SharedPreferences): ByteArray {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val ivEncoded = base64encode(cipher.iv)

    // Save IV
    sp.edit().putString(SP_AES_IV_KEY, ivEncoded).apply()

    return cipher.doFinal(secret)
}

private fun decryptAES(secret: ByteArray, key: SecretKey, sp: SharedPreferences): ByteArray {
    val iv = base64decode(sp.getString(SP_AES_IV_KEY, null) ?: throw IllegalStateException("AES IV missing"))

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
    return cipher.doFinal(secret)
}

@RequiresApi(Build.VERSION_CODES.M)
private fun generateAESKey(keyAlias: String) {
    val keyGenerator = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES, KEY_PROVIDER
    )

    keyGenerator.init(
        KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
    )

    keyGenerator.generateKey()
}