package cz.applifting.humansis.managers

import android.content.Context
import android.content.SharedPreferences
import com.commonsware.cwac.saferoom.SafeHelperFactory
import cz.applifting.humansis.db.DbProvider
import cz.applifting.humansis.db.HumansisDB
import cz.applifting.humansis.di.SPQualifier
import cz.applifting.humansis.extensions.suspendCommit
import cz.applifting.humansis.misc.*
import cz.applifting.humansis.model.api.LoginReqRes
import cz.applifting.humansis.model.db.User
import kotlinx.coroutines.supervisorScope
import net.sqlcipher.database.SQLiteException
import quanti.com.kotlinlog.Log
import javax.inject.Inject

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 *
 * This class is something like a repository, but it is not, as it is required by API service and can't depend on it
 */
const val SP_DB_PASS_KEY = "humansis-db"
const val SP_SALT_KEY = "humansis-db-pass-salt"
const val KEYSTORE_KEY_ALIAS = "HumansisDBKey"
const val SP_COUNTRY = "country"
const val SP_FIRST_COUNTRY_DOWNLOAD = "first_country_download"
const val SP_LOGS_UPLOAD_FAILED_ONLY = "logs_upload_failed_only"

class LoginManager @Inject constructor(
    private val dbProvider: DbProvider,
    @param:SPQualifier(type = SPQualifier.Type.GENERIC) private val sp: SharedPreferences,
    @param:SPQualifier(type = SPQualifier.Type.CRYPTO) private val spCrypto: SharedPreferences,
    private val context: Context
) {

    val db: HumansisDB by lazy { dbProvider.get() }

    suspend fun login(userResponse: LoginReqRes, originalPass: ByteArray): User {
        // Initialize db and save the DB password in shared prefs
        // The hashing of pass might be unnecessary, but why not. I am passing it to 3-rd part lib.
        val dbPass = hashSHA512(originalPass.plus(retrieveOrInitDbSalt().toByteArray()), 1000)
        val defaultCountry = userResponse.availableCountries?.firstOrNull() ?: ""

        if (retrieveUser()?.invalidPassword == true) {
            // This case handles token expiration on backend. DB is decrypted with the old pass, but is rekyed using the new one.
            val oldEncryptedPassword = sp.getString(SP_DB_PASS_KEY, null) ?: throw IllegalStateException("DB password lost")
            val oldDecryptedPassword = decryptUsingKeyStoreKey(base64decode(oldEncryptedPassword), KEYSTORE_KEY_ALIAS, spCrypto)
                ?: throw IllegalStateException("DB password couldn't be decrypted")

            dbProvider.init(dbPass, oldDecryptedPassword)
        } else {
            sp.edit().putBoolean(SP_FIRST_COUNTRY_DOWNLOAD, true).suspendCommit()
            dbProvider.init(dbPass, "default".toByteArray())
        }

        with(sp.edit()) {
            // Note that encryptUsingKeyStoreKey generates and stores IV to shared prefs
            val encryptedDbPass = base64encode(encryptUsingKeyStoreKey(dbPass, KEYSTORE_KEY_ALIAS, context, spCrypto))
            putString(SP_DB_PASS_KEY, encryptedDbPass)
            putString(SP_COUNTRY, defaultCountry)
            suspendCommit()
        }

        val db = dbProvider.get()

        val user = User(
            id = userResponse.id,
            username = userResponse.username,
            email = userResponse.email,
            saltedPassword = userResponse.password,
            countries = userResponse.availableCountries ?: listOf()
        )
        db.userDao().insert(user)

        return user
    }

    suspend fun logout() {
        db.clearAllTables()
        sp.edit().clear().suspendCommit()
        spCrypto.edit().clear().suspendCommit()

        encryptDefault()
    }

    suspend fun markInvalidPassword() {
        val user = db.userDao().getUser()
        if (user != null) {
            db.userDao().update(user.copy(invalidPassword = true))
        }
    }

    // Initializes DB if the key is available. Otherwise returns false.
    fun tryInitDB(): Boolean {
        if (dbProvider.isInitialized()) { return true }
        val encryptedPassword = sp.getString(SP_DB_PASS_KEY, null) ?: return false
        val decryptedPassword = decryptUsingKeyStoreKey(base64decode(encryptedPassword), KEYSTORE_KEY_ALIAS, spCrypto) ?: return false

        dbProvider.init(decryptedPassword)

        return true
    }

    suspend fun retrieveUser(): User? {
        return supervisorScope {
            try {
                val db = dbProvider.get()
                db.userDao().getUser()
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getAuthHeader(): String? {
        if (!dbProvider.isInitialized()) return null

        val user = retrieveUser()
        return user?.let {
            generateXWSSEHeader(user.username, user.saltedPassword, sp.getBoolean("test", false))
        }
    }

    suspend fun getCountries(): List<String> {
        return db.userDao().getUser()?.countries ?: listOf()
    }

    private fun encryptDefault() {
        if (dbProvider.isInitialized()) {
            try {
                SafeHelperFactory.rekey(db.openHelper.readableDatabase, "default".toCharArray())
            } catch (e: SQLiteException) {
                Log.d(TAG, e.toString())
            }
        }
    }

    private suspend fun retrieveOrInitDbSalt(): String {
        var salt = sp.getString(SP_SALT_KEY, null)

        if (salt == null) {
            salt = generateNonce()
            sp.edit()
                .putString(SP_SALT_KEY, salt)
                .suspendCommit()
        }

        return salt
    }

    companion object {
        private val TAG = LoginManager::class.java.simpleName
    }
}