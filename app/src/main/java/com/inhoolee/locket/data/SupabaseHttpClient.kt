package com.inhoolee.locket.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.lang.reflect.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseHttpClient(
    private val config: SupabaseConfig,
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = GsonBuilder().serializeNulls().create()
) {
    private val jsonMediaType = "application/json".toMediaType()

    suspend fun <T> authRequest(
        path: String,
        query: List<Pair<String, String>> = emptyList(),
        method: String,
        accessToken: String? = null,
        body: Any? = null,
        type: Type
    ): T = request(
        pathSegments = listOf("auth", "v1") + path.trim('/').split('/').filter(String::isNotBlank),
        query = query,
        method = method,
        accessToken = accessToken,
        body = body,
        prefer = null,
        type = type
    )

    suspend fun <T> postgrestRequest(
        table: String,
        query: List<Pair<String, String>> = emptyList(),
        method: String = "GET",
        accessToken: String,
        body: Any? = null,
        prefer: String? = if (method == "GET") null else "return=representation",
        type: Type
    ): T {
        val nextQuery = if (method != "GET" && body != null && query.none { it.first == "select" }) {
            query + ("select" to "*")
        } else {
            query
        }
        return request(
            pathSegments = listOf("rest", "v1", table),
            query = nextQuery,
            method = method,
            accessToken = accessToken,
            body = body,
            prefer = prefer,
            type = type
        )
    }

    suspend fun postgrestVoid(
        table: String,
        query: List<Pair<String, String>> = emptyList(),
        method: String,
        accessToken: String,
        body: Any? = null,
        prefer: String? = "return=minimal"
    ) {
        request<Unit>(
            pathSegments = listOf("rest", "v1", table),
            query = query,
            method = method,
            accessToken = accessToken,
            body = body,
            prefer = prefer,
            type = Unit::class.java
        )
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> request(
        pathSegments: List<String>,
        query: List<Pair<String, String>>,
        method: String,
        accessToken: String?,
        body: Any?,
        prefer: String?,
        type: Type
    ): T = withContext(Dispatchers.IO) {
        val url = buildUrl(pathSegments, query)
        val requestBody = when {
            body != null -> gson.toJson(body).toRequestBody(jsonMediaType)
            method in setOf("POST", "PUT", "PATCH") -> ByteArray(0).toRequestBody(jsonMediaType)
            else -> null
        }
        val requestBuilder = Request.Builder()
            .url(url)
            .method(method, requestBody)
            .addHeader("apikey", config.anonKey)
            .addHeader("Accept", "application/json")

        if (accessToken != null) {
            requestBuilder.addHeader("Authorization", "Bearer $accessToken")
        }
        if (prefer != null) {
            requestBuilder.addHeader("Prefer", prefer)
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val responseText = response.body.string()
            if (!response.isSuccessful) {
                throw SupabaseHttpException(response.code, extractErrorMessage(responseText))
            }
            if (type == Unit::class.java) {
                return@withContext Unit as T
            }
            val payload = responseText.ifBlank { "{}" }
            gson.fromJson<T>(payload, type)
        }
    }

    private fun buildUrl(pathSegments: List<String>, query: List<Pair<String, String>>): HttpUrl {
        val builder = config.url.toHttpUrl().newBuilder()
        pathSegments.forEach { builder.addPathSegment(it) }
        query.forEach { (name, value) -> builder.addQueryParameter(name, value) }
        return builder.build()
    }

    private fun extractErrorMessage(responseText: String): String {
        if (responseText.isBlank()) return "No response body"
        return runCatching {
            val map = gson.fromJson(responseText, Map::class.java)
            (map["message"] ?: map["error_description"] ?: map["error"])?.toString()
        }.getOrNull() ?: responseText
    }
}

class SupabaseHttpException(
    val statusCode: Int,
    message: String
) : IOException("Supabase request failed ($statusCode): $message")
