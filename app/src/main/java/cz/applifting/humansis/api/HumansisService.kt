package cz.applifting.humansis.api

import cz.applifting.humansis.model.api.*
import retrofit2.http.*

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */
interface HumansisService {

    @GET("salt/{username}")
    suspend fun getSalt(@Path("username") username: String): GetSaltResponse

    @POST("login")
    suspend fun postLogin(@Body loginReqRes: LoginReqRes): LoginReqRes

    @GET("projects")
    suspend fun getProjects(): List<Project>

    @GET("projects/{projectId}/distributions")
    suspend fun getDistributions(@Path("projectId") projectId: Int): List<Distribution>

    @GET("distributions/{distributionId}/beneficiaries")
    suspend fun getDistributionBeneficiaries(@Path("distributionId") distributionId: Int): List<DistributionBeneficiary>

    @POST("distributions/generalrelief/distributed")
    suspend fun setDistributedRelief(@Body distributedReliefRequest: DistributedReliefRequest)

    @POST("booklets/assign/{distributionId}/{beneficiaryId}")
    suspend fun assignBooklet(@Path("beneficiaryId") beneficiaryId: Int, @Path("distributionId") distributionId: Int, @Body assingBookletRequest: AssingBookletRequest)

    @POST("beneficiaries/{beneficiaryId}")
    suspend fun updateBeneficiaryReferral(@Path("beneficiaryId") beneficiaryId: Int, @Body beneficiary: BeneficiaryForReferralUpdate)

    @POST("smartcards")
    suspend fun assignSmartcard(@Body assignSmartcardRequest: AssignSmartcardRequest)

    @PATCH("smartcards/{serialNumber}")
    suspend fun deactivateSmartcard(@Path("serialNumber") serialNumber: String, @Body deactivateSmartcardRequest: DeactivateSmartcardRequest)

    @PATCH("smartcards/{serialNumber}/deposit")
    suspend fun distributeSmartcard(@Path("serialNumber") serialNumber: String, @Body distributeSmartcardRequest: DistributeSmartcardRequest)
}
