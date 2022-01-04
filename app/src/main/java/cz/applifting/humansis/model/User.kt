package cz.applifting.humansis.model

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 */
data class User(
    val id: Long,
    val username: String,
    val token: JWToken?,
    val email: String,
    val invalidPassword: Boolean = false,
    val countries: List<String> = listOf()
)