package cz.applifting.humansis.managers

import android.content.Context
import android.content.SharedPreferences
import com.commonsware.cwac.saferoom.BuildConfig
import com.commonsware.cwac.saferoom.SafeHelperFactory
import cz.applifting.humansis.db.DbProvider
import cz.applifting.humansis.db.HumansisDB
import cz.applifting.humansis.di.SPQualifier
import cz.applifting.humansis.extensions.suspendCommit
import cz.applifting.humansis.misc.*
import cz.applifting.humansis.model.JWToken
import cz.applifting.humansis.model.User
import cz.applifting.humansis.model.api.LoginResponse
import cz.applifting.humansis.model.db.UserDbEntity
import kotlinx.coroutines.supervisorScope
import net.sqlcipher.database.SQLiteException
import quanti.com.kotlinlog.Log
import javax.inject.Inject

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 *
 * This class is something like a repository, but it is not, as it is required by API service and can't depend on it
 */
const val KEYSTORE_KEY_ALIAS = "HumansisDBKey"

class LoginManager @Inject constructor(
    private val dbProvider: DbProvider,
    @param:SPQualifier(type = SPQualifier.Type.GENERIC) private val sp: SharedPreferences,
    @param:SPQualifier(type = SPQualifier.Type.CRYPTO) private val spCrypto: SharedPreferences,
    private val context: Context
) {

    val db: HumansisDB by lazy { dbProvider.get() }

    suspend fun login(userResponse: LoginResponse, originalPass: ByteArray): User {
        // Initialize db and save the DB password in shared prefs
        // The hashing of pass might be unnecessary, but why not. I am passing it to 3-rd part lib.
        val dbPass = hashSHA512(originalPass.plus(retrieveOrInitDbSalt().toByteArray()), 1000)
        val defaultCountry = userResponse.availableCountries.firstOrNull() ?: ""

        if (retrieveUser()?.shouldReauthenticate == true) {
            // This case handles token expiration on backend. DB is decrypted with the old pass, but is rekeyed using the new one.
            val oldEncryptedPassword = sp.getString(SP_DB_PASS_KEY, null)
                ?: throw IllegalStateException("DB password lost")
            val oldDecryptedPassword = decryptUsingKeyStoreKey(
                base64decode(oldEncryptedPassword),
                KEYSTORE_KEY_ALIAS,
                spCrypto
            )
                ?: throw IllegalStateException("DB password couldn't be decrypted")

            dbProvider.init(dbPass, oldDecryptedPassword)
        } else {
            sp.edit().putBoolean(SP_FIRST_COUNTRY_DOWNLOAD, true).suspendCommit()
            dbProvider.init(dbPass, "default".toByteArray())
        }

        with(sp.edit()) {
            // Note that encryptUsingKeyStoreKey generates and stores IV to shared prefs
            val encryptedDbPass =
                base64encode(encryptUsingKeyStoreKey(dbPass, KEYSTORE_KEY_ALIAS, context, spCrypto))
            putString(SP_DB_PASS_KEY, encryptedDbPass)
            putString(SP_COUNTRY, defaultCountry)
            putString(SP_USERNAME, userResponse.username)
            suspendCommit()
        }

        val db = dbProvider.get()

        val user = convert(userResponse)
        db.userDao().insert(user)

        return convert(user)
    }

    fun updateUser(loginResponse: LoginResponse) {
        db.userDao().update(convert(loginResponse))
    }

    suspend fun logout() {
        db.clearAllTables()
        sp.edit().clear().suspendCommit()
        spCrypto.edit().clear().suspendCommit()

        encryptDefault()
    }

    suspend fun forceReauthentication() {
        val user = retrieveUserDb()
        if (user != null) {
            db.userDao().update(user.copy(shouldReauthenticate = true))
        }
    }

    suspend fun invalidateTokens() {
        val user = retrieveUserDb()
        if (user != null) {
            db.userDao().update(
                user.copy(
                    token = null,
                    refreshToken = null,
                    refreshTokenExpiration = null
                )
            )
        }
    }

    // Initializes DB if the key is available. Otherwise returns false.
    fun tryInitDB(): Boolean {
        if (dbProvider.isInitialized()) {
            return true
        }
        val encryptedPassword = sp.getString(SP_DB_PASS_KEY, null) ?: return false
        val decryptedPassword =
            decryptUsingKeyStoreKey(base64decode(encryptedPassword), KEYSTORE_KEY_ALIAS, spCrypto)
                ?: return false

        dbProvider.init(decryptedPassword)

        return true
    }

    private suspend fun retrieveUserDb(): UserDbEntity? {
        return supervisorScope {
            if (dbProvider.isInitialized()) {
                try {
                    val db = dbProvider.get()
                    db.userDao().getUser()
                } catch (e: Exception) {
                    Log.e(TAG, e, "DB not initialized")
                    null
                }
            } else {
                null
            }
        }
    }

    suspend fun retrieveUser(): User? {
        return retrieveUserDb()?.let { convert(it) }
    }

    suspend fun getRefreshToken(): String? {
        return retrieveUserDb()?.refreshToken
    }

    suspend fun getAuthToken(): String? {
        return retrieveUserDb()?.token
    }

    suspend fun getCountries(): List<String> {
        return retrieveUserDb()?.countries ?: listOf()
    }

    private fun convert(userDb: UserDbEntity): User {
        return userDb.let {
            User(
                id = it.id,
                username = it.username,
                token = it.token?.let { token -> JWToken(getPayload(token)) },
                refreshToken = it.refreshToken,
                refreshTokenExpiration = it.refreshTokenExpiration,
                email = it.email,
                shouldReauthenticate = it.shouldReauthenticate,
                countries = it.countries
            )
        }
    }

    private fun convert(loginResponse: LoginResponse): UserDbEntity {
        return UserDbEntity(
            id = loginResponse.id,
            username = loginResponse.username,
            email = loginResponse.email,
            token = loginResponse.token,
            refreshToken = loginResponse.refreshToken,
            refreshTokenExpiration = loginResponse.refreshTokenExpiration,
            countries = loginResponse.availableCountries
        )
    }

    private fun encryptDefault() {
        if (dbProvider.isInitialized() && !BuildConfig.DEBUG) {
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