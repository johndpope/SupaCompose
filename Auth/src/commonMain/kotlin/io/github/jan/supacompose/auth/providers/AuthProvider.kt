package io.github.jan.supacompose.auth.providers

import io.github.jan.supacompose.SupabaseClient
import io.github.jan.supacompose.auth.auth
import io.github.jan.supacompose.auth.checkErrors
import io.github.jan.supacompose.auth.generateRedirectUrl
import io.github.jan.supacompose.auth.user.UserSession
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.json.JsonObject

interface AuthProvider<C, R> {

    suspend fun login(
        supabaseClient: SupabaseClient,
        onSuccess: suspend (UserSession) -> Unit,
        redirectUrl: String? = null,
        config: (C.() -> Unit)? = null
    )

    suspend fun signUp(
        supabaseClient: SupabaseClient,
        onSuccess: suspend (UserSession) -> Unit,
        redirectUrl: String? = null,
        config: (C.() -> Unit)? = null
    ): R

}

interface DefaultAuthProvider<C, R> : AuthProvider<C, R> {

    override suspend fun login(
        supabaseClient: SupabaseClient,
        onSuccess: suspend (UserSession) -> Unit,
        redirectUrl: String?,
        config: (C.() -> Unit)?
    ) {
        if(config == null) throw IllegalArgumentException("Credentials are required")
        val encodedCredentials = encodeCredentials(config)
        val response = supabaseClient.httpClient.post(supabaseClient.auth.resolveUrl("token?grant_type=password")) {
            setBody(encodedCredentials)
        }
        response.checkErrors()
        response.body<UserSession>().also {
            onSuccess(it)
        }
    }

    override suspend fun signUp(
        supabaseClient: SupabaseClient,
        onSuccess: suspend (UserSession) -> Unit,
        redirectUrl: String?,
        config: (C.() -> Unit)?
    ): R {
        if (config == null) throw IllegalArgumentException("Credentials are required")
        val finalRedirectUrl = supabaseClient.auth.generateRedirectUrl(redirectUrl)
        val redirect = finalRedirectUrl?.let {
            "?redirect_to=$finalRedirectUrl"
        } ?: ""
        val body = encodeCredentials(config)
        val response = supabaseClient.httpClient.post(supabaseClient.auth.resolveUrl("signup$redirect")) {
            setBody(body)
        }
        response.checkErrors()
        val json = response.body<JsonObject>()
        return decodeResult(json)
    }

    fun decodeResult(json: JsonObject): R

    fun encodeCredentials(credentials: C.() -> Unit): String

}