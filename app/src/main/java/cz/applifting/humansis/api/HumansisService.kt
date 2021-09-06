package cz.applifting.humansis.api

import cz.applifting.humansis.model.api.*
import retrofit2.http.*

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */
interface HumansisService {

    @GET("v1/salt/{username}")
    suspend fun getSalt(@Path("username") username: String): GetSaltResponse

    @POST("v1/login")
    suspend fun postLogin(@Body loginReqRes: LoginReqRes): LoginReqRes

    @GET("v1/projects")
    suspend fun getProjects(): List<Project>

    @GET("v1/projects/{projectId}/distributions")
    suspend fun getDistributions(@Path("projectId") projectId: Int): List<Distribution>

    @GET("v1/distributions/{distributionId}/beneficiaries")
    suspend fun getDistributionBeneficiaries(@Path("distributionId") distributionId: Int): List<DistributionBeneficiary>

    @POST("v1/distributions/generalrelief/distributed")
    suspend fun setDistributedRelief(@Body distributedReliefRequest: DistributedReliefRequest)

    @POST("v1/booklets/assign/{distributionId}/{beneficiaryId}")
    suspend fun assignBooklet(@Path("beneficiaryId") beneficiaryId: Int, @Path("distributionId") distributionId: Int, @Body assingBookletRequest: AssingBookletRequest)

    @POST("v1/beneficiaries/{beneficiaryId}")
    suspend fun updateBeneficiaryReferral(@Path("beneficiaryId") beneficiaryId: Int, @Body beneficiary: BeneficiaryForReferralUpdate)

    @POST("v1/smartcards")
    suspend fun assignSmartcard(@Body assignSmartcardRequest: AssignSmartcardRequest)

    @PATCH("v1/smartcards/{serialNumber}")
    suspend fun deactivateSmartcard(@Path("serialNumber") serialNumber: String, @Body deactivateSmartcardRequest: DeactivateSmartcardRequest)

    @PATCH("v3/smartcards/{serialNumber}/deposit")
    suspend fun distributeSmartcard(@Path("serialNumber") serialNumber: String, @Body distributeSmartcardRequest: DistributeSmartcardRequest)
}
