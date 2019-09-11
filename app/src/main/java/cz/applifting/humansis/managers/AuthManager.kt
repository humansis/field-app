package cz.applifting.humansis.managers

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import cz.applifting.humansis.db.DbProvider
import cz.applifting.humansis.db.HumansisDB
import cz.applifting.humansis.extensions.suspendCommit
import cz.applifting.humansis.misc.*
import cz.applifting.humansis.model.api.LoginReqRes
import cz.applifting.humansis.model.db.User
import javax.inject.Inject

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 *
 * This class is something like a repository, but it is not, as it is required by API service and can't depend on it
 */
class AuthManager @Inject constructor(private val dbProvider: DbProvider, private val sp: SharedPreferences, private val context: Context) {

    val db: HumansisDB by lazy { dbProvider.get() }

    @SuppressLint("CommitPrefEdits")
    suspend fun login(userResponse: LoginReqRes) {
        // Initialize db and save the DB password in shared prefs

        val dbPass = hashSHA512(userResponse.password.toByteArray().plus(DB_SALT.toByteArray()))
        val encryptedPassword = encryptUsingKeyStoreKey(dbPass, DB_KEY_ALIAS, context)

        with(sp.edit()) {
            putString(DB_SP_KEY, String(encryptedPassword))
            suspendCommit()
        }

        dbProvider.init(dbPass)

        // Clear password immediately
        for (i in 0..dbPass.size) {
            dbPass[i] = 0
        }

        val db = dbProvider.get()
        val id = userResponse.id?.toInt() ?: throw HumansisError("User id in response missing")
        val user = User(id, userResponse.username, userResponse.email, userResponse.password)
        db.userDao().insert(user)
    }

    suspend fun logout() {
        // TODO clear all other data
        db.apply {
            userDao().deleteAll()
            distributionsDao().deleteAll()
            projectsDao().deleteAll()
            beneficiariesDao().deleteAll()
        }
    }

    // Initializes DB if the key is available. Otherwise returns false.
    fun tryInitDB(): Boolean {
        if (dbProvider.isInitialized()) return true
        val encryptedPassword = sp.getString(DB_SP_KEY, null) ?: return false
        val decryptedPassword = decryptUsingKeyStoreKey(encryptedPassword.toByteArray(), DB_KEY_ALIAS, context)
        dbProvider.init(decryptedPassword)

        return true
    }

    suspend fun retrieveUser(): User? {
        val db = dbProvider.get()
        return db.userDao().getUser()
    }

    suspend fun getAuthHeader(): String? {
        if (!dbProvider.isInitialized()) return null

        val user = retrieveUser()
        return user?.let {
            generateXWSSEHeader(user.username, user.saltedPassword)
        }
    }
}