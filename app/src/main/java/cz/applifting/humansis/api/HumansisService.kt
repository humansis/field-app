package cz.applifting.humansis.api

import cz.applifting.humansis.model.api.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */
interface HumansisService {

    @POST("v2/login")
    suspend fun postLogin(@Body loginRequest: LoginRequest): LoginResponse

    @POST("v2/login/token/refresh")
    suspend fun refreshToken(@Body refreshToken: String): LoginResponse

    @GET("v1/projects")
    suspend fun getProjects(): List<Project>

    @GET("v1/projects/{projectId}/distributions")
    suspend fun getDistributions(
        @Path("projectId") projectId: Int
    ): List<Distribution>

    @GET("v4/assistances/{assistanceId}/targets/beneficiaries")
    suspend fun getDistributionBeneficiaries(@Path("assistanceId") assistanceId: Int): PagedApiEntity<DistributionBeneficiary>

    @PATCH("v1/assistances/relief-packages/distribute")
    suspend fun setReliefPackagesDistributed(
        @Body distributedReliefPackages: List<DistributedReliefPackages>
    )

    @POST("v1/booklets/assign/{assistanceId}/{beneficiaryId}")
    suspend fun assignBooklet(
        @Path("beneficiaryId") beneficiaryId: Int,
        @Path("assistanceId") assistanceId: Int,
        @Body assingBookletRequest: AssignBookletRequest
    )

    @POST("v1/beneficiaries/{beneficiaryId}")
    suspend fun updateBeneficiaryReferral(
        @Path("beneficiaryId") beneficiaryId: Int,
        @Body beneficiary: BeneficiaryForReferralUpdate
    )

    @POST("v1/smartcards")
    suspend fun assignSmartcard(@Body assignSmartcardRequest: AssignSmartcardRequest)

    @PATCH("v1/smartcards/{serialNumber}")
    suspend fun deactivateSmartcard(
        @Path("serialNumber") serialNumber: String,
        @Body deactivateSmartcardRequest: DeactivateSmartcardRequest
    )

    // TODO must be removed for v3.9.0 release
    @POST("v4/smartcards/{serialNumber}/deposit")
    suspend fun legacyDistributeSmartcard(
        @Path("serialNumber") serialNumber: String,
        @Body distributeSmartcardRequest: LegacyDistributeSmartcardRequest
    )

    @POST("v5/smartcards/{serialNumber}/deposit")
    suspend fun distributeSmartcard(
        @Path("serialNumber") serialNumber: String,
        @Body distributeSmartcardRequest: DistributeSmartcardRequest
    )

    @Multipart
    @POST("v1/users/{id}/logs")
    suspend fun postLogs(
        @Path("id") userId: Long,
        @Part logfile: MultipartBody.Part
    ): Response<Unit>
}
