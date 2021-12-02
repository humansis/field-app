package cz.applifting.humansis.managers

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.commonsware.cwac.saferoom.SafeHelperFactory
import cz.applifting.humansis.db.DbProvider
import cz.applifting.humansis.db.HumansisDB
import cz.applifting.humansis.model.api.LoginReqRes
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.emptyOrNullString
import org.hamcrest.Matchers.not
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginManagerTest {

    private lateinit var context: Context
    private lateinit var sp: SharedPreferences
    private lateinit var spCrypto: SharedPreferences
    private lateinit var dbProvider: DbProvider
    @MockK
    private lateinit var db: HumansisDB
    private lateinit var loginManager: LoginManager

    private val dbPassword = slot<ByteArray>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()
        sp = context.getSharedPreferences("HumansisTesting", Context.MODE_PRIVATE)
        spCrypto = context.getSharedPreferences("HumansisCryptoTesting", Context.MODE_PRIVATE)
        dbProvider = spyk(DbProvider(context))
        loginManager = LoginManager(dbProvider, sp, spCrypto, context)
        every { dbProvider.init(capture(dbPassword), any()) } answers {
            dbProvider.db = db
        }
        every { db.openHelper.readableDatabase } returns mockk()
        mockkStatic(SafeHelperFactory::class)
        every { SafeHelperFactory.rekey(db.openHelper.readableDatabase, any<CharArray>()) } returns Unit
    }

    @Test
    fun login() {
        val loginReqRes = LoginReqRes(
            changePassword = false,
            email = "email",
            id = 42,
            //language = null,
            password = "hashedPassword",
            //roles = emptyList(),
            username = "username"
            //vendor = null,
            //projects = emptyList()
        )
        coEvery { db.userDao().getUser() } returns null
        coEvery { db.userDao().insert(any()) } returns Unit

        runBlocking { loginManager.login(loginReqRes, "password".toByteArray()) }

        coVerify(exactly = 1) { db.userDao().insert(any()) }
        Assert.assertFalse(String(dbPassword.captured).contains("password", ignoreCase = true))
        Assert.assertThat(sp.getString(SP_DB_PASS_KEY, null), not(emptyOrNullString()))
    }

    @Test
    fun logout() {
        dbProvider.init(ByteArray(0), null)
        every { db.clearAllTables() } returns Unit
        Assert.assertTrue(sp.edit().putBoolean("nukes-ready-to-launch", true).commit())

        runBlocking { loginManager.logout() }

        verify(atLeast = 1) { db.clearAllTables() }
        println(sp.all)
        Assert.assertTrue(sp.all.isEmpty())
        verify(atLeast = 1) { SafeHelperFactory.rekey(db.openHelper.readableDatabase, any<CharArray>()) }
    }
}