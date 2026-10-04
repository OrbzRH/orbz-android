package app.orbz.android.net

import app.orbz.android.Config
import app.orbz.android.store.SecureStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

/** An error the console answered with: its status, its code and its one-line message, shown as is. */
class ApiException(val status: Int, val code: String, message: String, val retryAfterS: Int = 0) : Exception(message)

/**
 * The console's API, spoken the way its own pages speak it: a session cookie, and an Origin header on every
 * write (the server refuses writes from anywhere else). The cookie lives in [SecureStore] and nowhere else.
 */
class Api(private val store: SecureStore, private val base: String = Config.API_BASE) {
    /** True when an API key, not a session cookie, is the way in: the key's routes live under /v1. */
    val keyMode: Boolean get() = store.apiKey() != null
    private fun v1(keyPath: String, sessionPath: String) = if (keyMode) keyPath else sessionPath

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false; coerceInputValues = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun request(path: String, body: String? = null): Request {
        val b = Request.Builder().url(base + path)
            .header("accept", "application/json")
            .header("origin", base)
            .header("user-agent", "Orbz-Android/${Config.VERSION}")
        store.apiKey()?.let { b.header("authorization", "Bearer $it") } ?: store.cookie()?.let { b.header("cookie", "orbz_session=$it") }
        if (body != null) b.post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
        return b.build()
    }

    /** A new session cookie replaces the old one; an emptied one (sign-out) forgets it. */
    private fun rememberCookie(res: Response) {
        for (c in res.headers("set-cookie")) {
            if (!c.startsWith("orbz_session=")) continue
            val v = c.substringAfter("orbz_session=").substringBefore(";")
            if (v.isEmpty()) store.clearCookie() else store.saveCookie(v)
        }
    }

    private fun failure(res: Response, text: String): ApiException {
        val e = runCatching { json.parseToJsonElement(text).jsonObject["error"]?.jsonObject }.getOrNull()
        val code = e?.get("code")?.jsonPrimitive?.content ?: "error"
        val msg = e?.get("message")?.jsonPrimitive?.content ?: "Request failed (${res.code})."
        return ApiException(res.code, code, msg, res.header("retry-after")?.toIntOrNull() ?: 0)
    }

    private suspend fun <T> call(r: Request, parse: (String) -> T): T = withContext(Dispatchers.IO) {
        http.newCall(r).execute().use { res ->
            rememberCookie(res)
            val text = res.body.string()
            if (!res.isSuccessful) throw failure(res, text)
            parse(text)
        }
    }

    private inline fun <reified T> get(path: String): suspend () -> T = { call(request(path)) { json.decodeFromString<T>(it) } }

    suspend fun nonce(): Challenge = get<Challenge>("/api/auth/nonce")()
    suspend fun me(): Me = if (keyMode) Me(address = keyInfo().wallet) else get<Me>("/api/auth/me")()
    suspend fun keyInfo(): KeyInfo = get<KeyInfo>("/v1/key")()
    suspend fun summary(): Summary = if (keyMode) keyInfo().asSummary() else get<Summary>("/api/me/summary")()

    /** The key's view, in the console's shape, so the screens do not care which way the user came in. */
    private fun KeyInfo.asSummary(): Summary {
        val micro = { d: String -> java.math.BigDecimal(d).movePointRight(6).toBigInteger().toString() }
        val next = runCatching { java.time.Instant.parse(epoch.nextAt).epochSecond }.getOrDefault(0L)
        return Summary(
            address = wallet, tier = tier, tierName = tierName,
            balance = Balance(open = micro(open), reserved = micro(reserved), available = micro(balance)),
            buckets = buckets.map { Bucket(it.epoch, micro(it.amount), micro(it.remaining), it.startsAt, it.expiresAt) },
            month = null,
            epoch = Epoch(n = epoch.current, startsAt = next - Config.EPOCH_SECONDS, nextAt = next),
        )
    }
    suspend fun receipts(limit: Int = 40): ReceiptsPage = get<ReceiptsPage>("/api/receipts?limit=$limit")()

    /** Trades the signed message for a session. The cookie is kept by [rememberCookie]. */
    suspend fun verify(message: String, signature: String): String {
        val body = buildJsonObject { put("message", message); put("signature", signature) }.toString()
        return call(request("/api/auth/verify", body)) { json.parseToJsonElement(it).jsonObject["address"]!!.jsonPrimitive.content }
    }

    suspend fun logout() {
        if (!keyMode) runCatching { call(request("/api/auth/logout", "{}")) { } }
        store.clearCookie()
        store.clearApiKey()
    }

    suspend fun tokenFacts(ca: String): JsonObject =
        call(request(v1("/v1/token/facts", "/api/token/facts"), buildJsonObject { put("ca", ca) }.toString())) { json.parseToJsonElement(it).jsonObject }

    suspend fun walletFacts(address: String): JsonObject =
        call(request(v1("/v1/wallet/facts", "/api/wallet/facts"), buildJsonObject { put("address", address) }.toString())) { json.parseToJsonElement(it).jsonObject }

    /** The console's chat: streamed, paid from the wallet's credit, under the console's daily limit. */
    fun chat(turns: List<Pair<String, String>>, maxTokens: Int = 400): Flow<Stream> {
        val body = buildJsonObject {
            put("model", "orbz-opus"); put("stream", true); put("max_tokens", maxTokens)
            put("messages", buildJsonArray { for ((role, content) in turns) add(buildJsonObject { put("role", role); put("content", content) }) })
        }
        return stream(v1("/v1/chat/completions", "/api/playground/chat"), body.toString())
    }

    fun tokenRead(ca: String, question: String = ""): Flow<Stream> =
        stream(v1("/v1/token/read", "/api/token/ask"), buildJsonObject { put("ca", ca); put("question", question) }.toString())

    fun walletRead(address: String, question: String = ""): Flow<Stream> =
        stream(v1("/v1/wallet/read", "/api/wallet/ask"), buildJsonObject { put("address", address); put("question", question) }.toString())

    /** One server-sent event stream, as the console's pages read it: `data: {chunk}` lines, `[DONE]` at the end. */
    private fun stream(path: String, body: String): Flow<Stream> = flow {
        http.newCall(request(path, body)).execute().use { res ->
            rememberCookie(res)
            if (!res.isSuccessful) throw failure(res, res.body.string())
            val src = res.body.source()
            var finish = "stop"
            var cost: String? = null
            var balance: String? = null
            var tokensOut: Int? = null
            var done = false
            while (!done) {
                val line = src.readUtf8Line() ?: break
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                val j = runCatching { json.parseToJsonElement(data).jsonObject }.getOrNull() ?: continue
                j["error"]?.jsonObject?.let { e -> throw ApiException(502, "model_unavailable", e["message"]?.jsonPrimitive?.content ?: "The answer stopped.") }
                val choice = j["choices"]?.let { (it as? kotlinx.serialization.json.JsonArray)?.firstOrNull()?.jsonObject }
                choice?.get("delta")?.jsonObject?.get("content")?.jsonPrimitive?.content?.let { if (it.isNotEmpty()) emit(Stream.Delta(it)) }
                choice?.get("finish_reason")?.jsonPrimitive?.content?.let { if (it != "null") { finish = it; done = true } }
                j["usage"]?.jsonObject?.get("completion_tokens")?.jsonPrimitive?.content?.toIntOrNull()?.let { tokensOut = it }
                j["orbz"]?.jsonObject?.let { o ->
                    cost = o["cost"]?.jsonPrimitive?.content
                    balance = o["balance_after"]?.jsonPrimitive?.content
                }
            }
            emit(Stream.Done(finish, cost, balance, tokensOut))
        }
    }.flowOn(Dispatchers.IO)
}
