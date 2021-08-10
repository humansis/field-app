package cz.applifting.humansis.api

import cz.applifting.humansis.model.api.*
import retrofit2.http.*

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */
interface HumansisService {

    @GET("v1/salt/{username}")
    suspend fun getSalt(
        @Path("username") username: String
    ): GetSaltResponse

    @POST("v1/login")
    suspend fun postLogin(
        @Body loginReqRes: LoginReqRes
    ): LoginReqRes

    @GET("v2/projects")
    suspend fun getProjects(
        @Header("country") country: String
    ): List<Project>

    @GET("v1/modality-types")
    suspend fun getModalityTypes(): List<ModalityType>

    @GET("v2/commodities")
    suspend fun getCommodities(
        @Query("filter[notModalityTypes][]") notModalityTypes: List<String>?
    ): List<Commodity>

    /**
     *  Leaving filter as null returns assistances unfiltered
     *  Available assistanceTypes: distribution, activity
     */
    @GET("v2/projects/{projectId}/assistances")
    suspend fun getAssistances(
        @Path("projectId") projectId: Int,
        @Header("country") country: String,
        @Query("filter[type]") assistanceType: String?,
        @Query("filter[completed]") completed: Int?,
        @Query("filter[notModalityTypes][]") notModalityTypes: List<String>?
    ): List<Assistance>

    @GET("v1/distributions/{distributionId}/beneficiaries")
    suspend fun getDistributionBeneficiaries(
        @Path("distributionId") distributionId: Int
    ): List<DistributionBeneficiary>

    @POST("v1/distributions/generalrelief/distributed")
    suspend fun setDistributedRelief(
        @Body distributedReliefRequest: DistributedReliefRequest
    )

    @POST("v1/booklets/assign/{distributionId}/{beneficiaryId}")
    suspend fun assignBooklet(
        @Path("beneficiaryId") beneficiaryId: Int,
        @Path("distributionId") distributionId: Int,
        @Body assignBookletRequest: AssignBookletRequest
    )

    @POST("v1/beneficiaries/{beneficiaryId}")
    suspend fun updateBeneficiaryReferral(
        @Path("beneficiaryId") beneficiaryId: Int,
        @Body beneficiary: BeneficiaryForReferralUpdate
    )

    @POST("v1/smartcards")
    suspend fun assignSmartcard(
        @Body assignSmartcardRequest: AssignSmartcardRequest
    )

    @PATCH("v1/smartcards/{serialNumber}")
    suspend fun deactivateSmartcard(
        @Path("serialNumber") serialNumber: String,
        @Body deactivateSmartcardRequest: DeactivateSmartcardRequest
    )

    @PATCH("v1/smartcards/{serialNumber}/deposit")
    suspend fun distributeSmartcard(
        @Path("serialNumber") serialNumber: String,
        @Body distributeSmartcardRequest: DistributeSmartcardRequest
    )
}
