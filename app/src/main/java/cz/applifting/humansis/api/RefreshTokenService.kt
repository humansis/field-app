package cz.applifting.humansis.api

import cz.applifting.humansis.model.api.LoginResponse
import cz.applifting.humansis.model.api.RefreshTokenRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface RefreshTokenService {

    @POST("v2/login/token/refresh")
    suspend fun refreshToken(@Body refreshTokenRequest: RefreshTokenRequest): LoginResponse
}