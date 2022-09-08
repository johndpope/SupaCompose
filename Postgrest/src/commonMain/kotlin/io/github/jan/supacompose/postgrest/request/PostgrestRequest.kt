package io.github.jan.supacompose.postgrest.request

import io.github.jan.supacompose.SupabaseClient
import io.github.jan.supacompose.auth.auth
import io.github.jan.supacompose.exceptions.RestException
import io.github.jan.supacompose.postgrest.Postgrest
import io.github.jan.supacompose.postgrest.query.Count
import io.github.jan.supacompose.postgrest.query.PostgrestBuilder
import io.github.jan.supacompose.postgrest.query.PostgrestResult
import io.github.jan.supacompose.postgrest.query.Returning
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.parametersOf
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface PostgrestRequest {

    val body: JsonElement? get() = null
    val method: HttpMethod
    val filter: Map<String, String>
    val prefer: List<String>

    suspend fun execute(table: String, postgrest: Postgrest): PostgrestResult {
        return postgrest.supabaseClient.httpClient.request(postgrest.resolveUrl(table)) {
            method = this@PostgrestRequest.method
            contentType(ContentType.Application.Json)
            headers[HttpHeaders.Authorization] = "Bearer ${postgrest.supabaseClient.auth.currentSession.value?.accessToken ?: throw IllegalStateException("Trying to access database without a user session")}"
            headers[PostgrestBuilder.HEADER_PREFER] = prefer.joinToString(",")
            setBody(this@PostgrestRequest.body)
            parametersOf(filter.mapValues { (_, value) -> listOf(value) })
        }.checkForErrorCodes()
    }

    private suspend fun HttpResponse.checkForErrorCodes(): PostgrestResult {
        if(status.value !in 200..299) {
            try {
                val error = body<JsonObject>()
                throw RestException(status.value, error["error"]?.jsonPrimitive?.content ?: "Unknown error", error.toString())
            } catch(_: Exception) {
                throw RestException(status.value, "Unknown error", "")
            }
        }
        return PostgrestResult(body(), status.value)
    }

    data class Select(
        private val head: Boolean = false,
        private val count: Count? = null,
        override val filter: Map<String, String>
    ): PostgrestRequest {

        override val method = if(head) HttpMethod.Head else HttpMethod.Get
        override val prefer = if (count != null) listOf("count=${count.identifier}") else listOf()

    }

    data class Insert(
        override val body: JsonArray,
        private val upsert: Boolean = false,
        private val onConflict: String? = null,
        private val returning: Returning = Returning.REPRESENTATION,
        private val count: Count? = null,
        override val filter: Map<String, String>,
    ): PostgrestRequest {

        override val method = HttpMethod.Post
        override val prefer = buildList {
            add("return=${returning.identifier}")
            if(upsert) add("resolution=merge-duplicates")
            if(count != null) add("count=${count.identifier}")
        }

    }

    data class Update(
        private val returning: Returning = Returning.REPRESENTATION,
        private val count: Count? = null,
        override val filter: Map<String, String>,
        override val body: JsonElement
    ) : PostgrestRequest {

        override val method = HttpMethod.Patch
        override val prefer = buildList {
            add("return=${returning.identifier}")
            if (count != null) add("count=${count.identifier}")
        }

    }

    data class Delete(
        private val returning: Returning = Returning.REPRESENTATION,
        private val count: Count? = null,
        override val filter: Map<String, String>
    ): PostgrestRequest {

        override val method = HttpMethod.Delete
        override val prefer = buildList {
            add("return=${returning.identifier}")
            if (count != null) add("count=${count.identifier}")
        }

    }

}