package cz.applifting.humansis

import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.security.auth.x500.X500Principal


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class CryptoTest {

    @Test
    fun aesTest() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
        )

        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                "key2",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )

        val key = keyGenerator.generateKey()

        val cipher = Cipher.getInstance ("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val encrypted = cipher.doFinal("password".toByteArray())

        cipher.init(Cipher.DECRYPT_MODE, key, cipher.parameters)
        val decrypted = cipher.doFinal(encrypted)

        println(String(decrypted))
    }

    @Test
    fun rsaTest90() {
        val keyAlias = "humansis"
        val appContext = InstrumentationRegistry.getTargetContext()

        val kpg = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore"
        )

        val startDate = Calendar.getInstance()
        val endDate = Calendar.getInstance()
        endDate.add(Calendar.YEAR, 30)

        val keyGeneratorSpec = KeyPairGeneratorSpec
            .Builder(appContext)
            .setAlias(keyAlias)
            .setSubject(X500Principal("CN=$keyAlias"))
            .setSerialNumber(BigInteger.TEN)
            .setStartDate(startDate.time)
            .setEndDate(endDate.time)
            .build()

        kpg.initialize(keyGeneratorSpec)

        val keys = kpg.genKeyPair()

        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, keys.public)

        val input = "Password"

        val encrypted = cipher.doFinal(input.toByteArray())
        cipher.init(Cipher.DECRYPT_MODE, keys.private, cipher.parameters)
        val decrypted = cipher.doFinal(encrypted)

        val output = String(decrypted)

        Assert.assertEquals(input, output)
    }
}
