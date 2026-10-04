package app.orbz.android.ui

import android.app.Application
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.orbz.android.auth.Signer
import app.orbz.android.auth.Siwe
import app.orbz.android.net.Api
import app.orbz.android.net.ApiException
import app.orbz.android.net.Receipt
import app.orbz.android.net.Stream
import app.orbz.android.net.Summary
import app.orbz.android.store.SecureStore
import app.orbz.android.wallet.Wc
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/** One Api and one store for the whole process, so every screen sees the same session. */
object Services {
    @Volatile private var store: SecureStore? = null
    @Volatile private var api: Api? = null
    fun store(app: Application) = store ?: SecureStore(app).also { store = it }
    fun api(app: Application) = api ?: Api(store(app)).also { api = it }
}

sealed class Phase {
    data object Booting : Phase()
    data object SignedOut : Phase()
    data class Signing(val address: String) : Phase()
    data class SignedIn(val address: String) : Phase()
}

/** The session: who is signed in, their summary, and the sign-in and sign-out flows. */
class AppVm(app: Application) : AndroidViewModel(app) {
    private val store = Services.store(app)
    private val api = Services.api(app)

    val phase = MutableStateFlow<Phase>(Phase.Booting)
    val summary = MutableStateFlow<Summary?>(null)
    val error = MutableStateFlow<String?>(null)
    /** The connected wallet, which may not be signed in yet. */
    val wallet: StateFlow<String?> = Wc.account
    private var signing: Job? = null

    init {
        boot()
        viewModelScope.launch { Wc.approved.collect { a -> if (phase.value !is Phase.SignedIn) signIn(Wc.WalletSigner(a, ::openWallet)) } }
        viewModelScope.launch { Wc.errors.collect { error.value = it } }
    }

    private fun boot() = viewModelScope.launch {
        if (store.cookie() == null && store.apiKey() == null) { phase.value = Phase.SignedOut; return@launch }
        val me = runCatching { api.me() }.getOrNull()
        val a = me?.address
        if (a != null) { phase.value = Phase.SignedIn(a); refresh() } else { store.clearCookie(); store.clearApiKey(); phase.value = Phase.SignedOut }
    }

    /** The other way in: a key from the console's Keys page. Checked against /v1/key before it is kept. */
    fun signInWithKey(raw: String) = viewModelScope.launch {
        val key = raw.trim()
        error.value = null
        if (!Regex("^sk-orbz-\\d+-[A-Za-z0-9_-]{20,}$").matches(key)) { error.value = "That does not look like an Orbz API key (sk-orbz-…)."; return@launch }
        phase.value = Phase.Signing("key")
        store.saveApiKey(key)
        try {
            val info = api.keyInfo()
            store.saveAddress(info.wallet)
            phase.value = Phase.SignedIn(info.wallet)
            refresh()
        } catch (e: Exception) {
            store.clearApiKey()
            error.value = e.message ?: "The key was not accepted."
            phase.value = Phase.SignedOut
        }
    }

    val viaKey: Boolean get() = api.keyMode

    /** The whole sign-in: a one-time challenge, the signature from the wallet, the session cookie, the summary. */
    fun signIn(signer: Signer) {
        signing?.cancel()
        signing = viewModelScope.launch {
            error.value = null
            phase.value = Phase.Signing(signer.address)
            try {
                val challenge = api.nonce()
                val message = Siwe.build(challenge, signer.address)
                val signature = signer.personalSign(message)
                val address = api.verify(message, signature)
                store.saveAddress(address)
                phase.value = Phase.SignedIn(address)
                refresh()
            } catch (e: Exception) {
                error.value = e.message ?: "Sign-in failed."
                phase.value = Phase.SignedOut
            }
        }
    }

    fun signInWithWallet() { wallet.value?.let { signIn(Wc.WalletSigner(it, ::openWallet)) } }

    fun cancelSignIn() { signing?.cancel(); phase.value = Phase.SignedOut }

    fun refresh() = viewModelScope.launch {
        try { summary.value = api.summary() } catch (e: ApiException) {
            if (e.status == 401) signOut() else error.value = e.message
        } catch (e: Exception) { error.value = e.message }
    }

    fun signOut() = viewModelScope.launch {
        api.logout()
        store.clearAll()
        Wc.disconnect()
        summary.value = null
        phase.value = Phase.SignedOut
    }

    private fun openWallet(link: String) {
        runCatching { getApplication<Application>().startActivity(Intent(Intent.ACTION_VIEW, link.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

data class Turn(val id: Long, val role: String, val text: String, val cost: String? = null, val tokens: Int? = null, val streaming: Boolean = false)

/** The console's chat, kept for as long as the app lives. */
class ChatVm(app: Application) : AndroidViewModel(app) {
    private val api = Services.api(app)
    val turns = MutableStateFlow<List<Turn>>(emptyList())
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val balance = MutableStateFlow<String?>(null)

    fun send(text: String) {
        val q = text.trim()
        if (q.isEmpty() || busy.value) return
        error.value = null
        val history = turns.value.filter { !it.streaming }.takeLast(10).map { it.role to it.text }
        val aid = System.nanoTime()
        turns.value = turns.value + Turn(aid - 1, "user", q) + Turn(aid, "assistant", "", streaming = true)
        busy.value = true
        viewModelScope.launch {
            api.chat(history + ("user" to q))
                .onEach { ev ->
                    when (ev) {
                        is Stream.Delta -> patch(aid) { it.copy(text = it.text + ev.text) }
                        is Stream.Done -> { patch(aid) { it.copy(cost = ev.cost, tokens = ev.tokensOut, streaming = false) }; ev.balanceAfter?.let { balance.value = it } }
                    }
                }
                .catch { e -> error.value = e.message; patch(aid) { it.copy(streaming = false) } }
                .collect()
            turns.value = turns.value.filter { it.text.isNotEmpty() || it.role == "user" }
            busy.value = false
        }
    }

    private fun patch(id: Long, f: (Turn) -> Turn) { turns.value = turns.value.map { if (it.id == id) f(it) else it } }
    fun clear() { turns.value = emptyList(); error.value = null }
}

/** Token check and Wallet check: the facts, then the read, then follow-ups. */
class CheckVm(app: Application) : AndroidViewModel(app) {
    private val api = Services.api(app)
    val kind = MutableStateFlow<String?>(null) // "token" | "wallet"
    val facts = MutableStateFlow<JsonObject?>(null)
    val loading = MutableStateFlow(false)
    val read = MutableStateFlow("")
    val reading = MutableStateFlow(false)
    val cost = MutableStateFlow<String?>(null)
    val error = MutableStateFlow<String?>(null)
    private var job: Job? = null

    fun check(input: String) {
        val target = input.trim()
        if (target.isEmpty() || loading.value) return
        job?.cancel()
        job = viewModelScope.launch {
            loading.value = true; error.value = null; facts.value = null; kind.value = null; read.value = ""; cost.value = null
            try {
                val f = try { api.tokenFacts(target).also { kind.value = "token" } } catch (e: ApiException) {
                    if (e.code != "not_a_contract") throw e
                    api.walletFacts(target).also { kind.value = "wallet" }
                }
                facts.value = f
                loading.value = false
                stream(if (kind.value == "token") api.tokenRead(target) else api.walletRead(target))
            } catch (e: Exception) { error.value = e.message; loading.value = false }
        }
    }

    fun ask(question: String) {
        val q = question.trim()
        val f = facts.value ?: return
        if (q.isEmpty() || reading.value) return
        val target = f["address"].toString().trim('"')
        job?.cancel()
        job = viewModelScope.launch { stream(if (kind.value == "token") api.tokenRead(target, q) else api.walletRead(target, q)) }
    }

    private suspend fun stream(flow: kotlinx.coroutines.flow.Flow<Stream>) {
        reading.value = true; read.value = ""; cost.value = null; error.value = null
        flow.onEach { ev ->
            when (ev) {
                is Stream.Delta -> read.value += ev.text
                is Stream.Done -> cost.value = ev.cost
            }
        }.catch { e -> error.value = e.message }.collect()
        reading.value = false
    }
}

class ReceiptsVm(app: Application) : AndroidViewModel(app) {
    private val api = Services.api(app)
    val rows = MutableStateFlow<List<Receipt>?>(null)
    val error = MutableStateFlow<String?>(null)
    fun load() = viewModelScope.launch {
        try { rows.value = api.receipts().data } catch (e: Exception) { error.value = e.message }
    }
}
