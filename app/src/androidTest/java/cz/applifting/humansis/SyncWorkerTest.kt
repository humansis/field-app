package cz.applifting.humansis

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestWorkerBuilder
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.model.Target
import cz.applifting.humansis.model.db.BeneficiaryLocal
import cz.applifting.humansis.model.db.DistributionLocal
import cz.applifting.humansis.model.db.ProjectLocal
import cz.applifting.humansis.model.db.SyncError
import cz.applifting.humansis.repositories.BeneficiariesRepository
import cz.applifting.humansis.repositories.DistributionsRepository
import cz.applifting.humansis.repositories.ErrorsRepository
import cz.applifting.humansis.repositories.ProjectsRepository
import cz.applifting.humansis.synchronization.SP_SYNC_UPLOAD_INCOMPLETE
import cz.applifting.humansis.synchronization.SyncWorker
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.HttpException
import retrofit2.Response
import java.net.HttpURLConnection

@RunWith(AndroidJUnit4::class)
class SyncWorkerTest {

    private lateinit var context: Context
    private lateinit var worker: SyncWorker
    @MockK
    private lateinit var projectsRepository: ProjectsRepository
    @MockK
    private lateinit var distributionsRepository: DistributionsRepository
    @MockK
    private lateinit var beneficiariesRepository: BeneficiariesRepository
    @MockK
    private lateinit var errorsRepository: ErrorsRepository
    private val errors: MutableList<SyncError> = mutableListOf()
    @RelaxedMockK
    private lateinit var sharedPreferences: SharedPreferences
    private var uploadIncomplete: Boolean = false
    @MockK
    private lateinit var loginManager: LoginManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()
        worker = TestWorkerBuilder.from(context, SyncWorker::class.java).build().also {
            it.projectsRepository = projectsRepository.apply {
                coEvery { getNameByAssistanceId(any()) } returns ""
            }
            it.distributionsRepository = distributionsRepository.apply {
                coEvery { getNameById(any()) } returns ""
            }
            it.beneficiariesRepository = beneficiariesRepository.apply {
                // no upload by default
                coEvery { getAssignedBeneficiariesOfflineSuspend() } returns emptyList()
                coEvery { getAllReferralChangesOffline() } returns emptyList()
            }
            it.errorsRepository = errorsRepository.apply {
                coEvery { clearAll() } answers { errors.clear() }
                coEvery { insertAll(any()) } answers { errors.addAll(firstArg()) }
            }
            it.sp = sharedPreferences.apply {
                coEvery { edit().putBoolean(SP_SYNC_UPLOAD_INCOMPLETE, any()) } answers {
                    uploadIncomplete = secondArg()
                    self as SharedPreferences.Editor
                }
            }
            it.loginManager = loginManager.apply {
                coEvery { tryInitDB() } returns true
            }
        }
    }

    @Test
    fun allEmpty() {
        coEvery { projectsRepository.getProjectsOnline() } returns emptyList()

        val result = runBlocking { worker.doWork() }

        assertSuccess(result)
        coVerify(exactly = 1) { projectsRepository.getProjectsOnline() }
    }

    @Test
    fun download() {
        val projectsCount = 2
        coEvery { projectsRepository.getProjectsOnline() } returns List(projectsCount) { anyProject() }
        val distributionCount = 3
        coEvery { distributionsRepository.getDistributionsOnline(any()) } returns List(distributionCount) { anyDistribution() }
        coEvery { beneficiariesRepository.getBeneficiariesOnline(any()) } returns List(5) { anyBeneficiary() }

        val result = runBlocking { worker.doWork() }

        assertSuccess(result)
        coVerify(exactly = 1) { projectsRepository.getProjectsOnline() }
        coVerify(exactly = projectsCount) { distributionsRepository.getDistributionsOnline(any()) }
        coVerify(exactly = projectsCount * distributionCount) { beneficiariesRepository.getBeneficiariesOnline(any()) }
    }

    @Test
    fun upload() {
        coEvery { projectsRepository.getProjectsOnline() } returns emptyList()
        val assignedBeneficiaryCount = 2
        val changedReferralCount = 3
        beneficiariesRepository.apply {
            coEvery { getAssignedBeneficiariesOfflineSuspend() } returns List(assignedBeneficiaryCount) { anyBeneficiary() }
            coEvery { getAllReferralChangesOffline() } returns List(changedReferralCount) { anyBeneficiary() }
            coEvery { distribute(any()) } returns Unit
            coEvery { updateBeneficiaryReferralOnline(any()) } returns Unit
        }

        val result = runBlocking { worker.doWork() }

        assertSuccess(result)
        coVerify(exactly = 1) { projectsRepository.getProjectsOnline() }
        beneficiariesRepository.apply {
            coVerify(exactly = 1) { getAssignedBeneficiariesOfflineSuspend() }
            coVerify(exactly = 1) { getAllReferralChangesOffline() }
            coVerify(exactly = assignedBeneficiaryCount) { distribute(any()) }
            coVerify(exactly = changedReferralCount) { updateBeneficiaryReferralOnline(any()) }
        }
    }

    @Test
    fun errorOnDistribute() {
        beneficiariesRepository.apply {
            coEvery { getAssignedBeneficiariesOfflineSuspend() } returns listOf(anyBeneficiary())
            coEvery { distribute(any()) } throws anyHttpException()
        }

        val result = runBlocking { worker.doWork() }

        assertFailure(result)
        Assert.assertTrue(uploadIncomplete)
    }

    @Test
    fun errorOnReferralUpdate() {
        beneficiariesRepository.apply {
            coEvery { getAllReferralChangesOffline() } returns listOf(anyBeneficiary())
            coEvery { updateBeneficiaryReferralOnline(any()) } throws anyHttpException()
        }

        val result = runBlocking { worker.doWork() }

        assertFailure(result)
        Assert.assertTrue(uploadIncomplete)
    }

    @Test
    fun errorOnProjects() {
        coEvery { projectsRepository.getProjectsOnline() } throws anyHttpException()

        val result = runBlocking { worker.doWork() }

        assertFailure(result)
        coVerify(exactly = 1) { projectsRepository.getProjectsOnline() }
    }

    @Test
    fun errorOnDistributions() {
        coEvery { projectsRepository.getProjectsOnline() } returns listOf(anyProject())
        coEvery { distributionsRepository.getDistributionsOnline(any()) } throws anyHttpException()

        val result = runBlocking { worker.doWork() }

        assertFailure(result)
        coVerify(exactly = 1) { projectsRepository.getProjectsOnline() }
        coVerify(exactly = 1) { distributionsRepository.getDistributionsOnline(any()) }
    }

    @Test
    fun errorOnBeneficiaries() {
        coEvery { projectsRepository.getProjectsOnline() } returns listOf(anyProject())
        coEvery { distributionsRepository.getDistributionsOnline(any()) } returns listOf(anyDistribution())
        coEvery { beneficiariesRepository.getBeneficiariesOnline(any()) } throws anyHttpException()

        val result = runBlocking { worker.doWork() }

        assertFailure(result)
        coVerify(exactly = 1) { projectsRepository.getProjectsOnline() }
        coVerify(exactly = 1) { distributionsRepository.getDistributionsOnline(any()) }
        coVerify(exactly = 1) { beneficiariesRepository.getBeneficiariesOnline(any()) }
    }

    private fun assertSuccess(result: ListenableWorker.Result) {
        println(errors)
        Assert.assertThat(result, instanceOf(ListenableWorker.Result.Success::class.java))
        coVerify(exactly = 0) { errorsRepository.insertAll(any()) }
        Assert.assertFalse(uploadIncomplete)
    }

    private fun assertFailure(result: ListenableWorker.Result) {
        Assert.assertThat(result, instanceOf(ListenableWorker.Result.Failure::class.java))
        coVerify(atLeast = 1) { errorsRepository.insertAll(any()) }
        Assert.assertTrue(errors.isNotEmpty())
    }

    private fun anyProject() = ProjectLocal(
        id = 0,
        name = "",
        numberOfHouseholds = 0
    )

    private fun anyDistribution() = DistributionLocal(
        id = 0,
        name = "",
        numberOfBeneficiaries = 0,
        commodities = emptyList(),
        dateOfDistribution = "",
        dateOfExpiration = null,
        projectId = 0,
        target = Target.FAMILY,
        completed = false,
        remote = true,
        foodLimit = null,
        nonfoodLimit = null,
        cashbackLimit = null
    )

    private fun anyBeneficiary() = BeneficiaryLocal(
        id = 0,
        beneficiaryId = 0,
        givenName = null,
        familyName = null,
        assistanceId = 0,
        distributed = false,
        distributedAt = null,
        reliefIDs = emptyList(),
        qrBooklets = emptyList(),
        edited = false,
        commodities = emptyList(),
        nationalId = null,
        originalReferralType = null,
        originalReferralNote = null,
        referralType = null,
        referralNote = null,
        smartcard = null,
        newSmartcard = null,
        remote = true,
        dateExpiration = null,
        foodLimit = null,
        nonfoodLimit = null,
        cashbackLimit = null,
        balance = null,
        originalBalance = null,
    )

    private fun anyHttpException() =
        HttpException(Response.error<Any>(HttpURLConnection.HTTP_INTERNAL_ERROR, ResponseBody.create(null, "")))
}