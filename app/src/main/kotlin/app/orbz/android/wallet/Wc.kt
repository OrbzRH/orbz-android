package app.orbz.android.wallet

import android.app.Application
import android.util.Log
import app.orbz.android.Config
import app.orbz.android.auth.Signer
import app.orbz.android.auth.Siwe
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
import com.reown.appkit.client.models.Session
import com.reown.appkit.client.models.request.Request
import com.reown.appkit.client.models.request.SentRequestResult
import com.reown.appkit.presets.AppKitChainsPresets
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray

/**
 * WalletConnect, through Reown's AppKit. The app asks a wallet for exactly one thing, `personal_sign` of the
 * sign-in message; it never sends a transaction and never asks for a token approval, and the chain it declares
 * requires nothing else.
 */
object Wc : AppKit.ModalDelegate {
    private const val TAG = "orbz.wc"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The address of the live wallet session, or null. */
    val account = MutableStateFlow<String?>(null)
    /** Fires once each time the user approves a new session in a wallet. */
    val approved = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    private val responses = MutableSharedFlow<Modal.Model.SessionRequestResponse>(replay = 8, extraBufferCapacity = 8)
    private val expired = MutableSharedFlow<Long>(replay = 8, extraBufferCapacity = 8)

    @Volatile
    var ready = false
        private set

    val robinhood = Modal.Model.Chain(
        chainName = Config.CHAIN_NAME,
        chainNamespace = "eip155",
        chainReference = Config.CHAIN_ID.toString(),
        requiredMethods = listOf("personal_sign"),
        optionalMethods = listOf("eth_signTypedData_v4"),
        events = listOf("chainChanged", "accountsChanged"),
        token = AppKitChainsPresets.ethToken,
        rpcUrl = Config.RPC_URL,
        blockExplorerUrl = Config.EXPLORER,
    )

    fun init(app: Application) {
        if (!Config.walletConnectReady) return
        val meta = Core.Model.AppMetaData(
            name = "Orbz",
            description = "AI credits from \$ORBZ trading fees",
            url = Config.SITE,
            icons = listOf("${Config.API_BASE}/icon.png"),
            redirect = Config.RETURN_LINK,
        )
        CoreClient.initialize(application = app, projectId = Config.WC_PROJECT_ID, metaData = meta) { e -> Log.w(TAG, "core: ${e.throwable.message}") }
        AppKit.initialize(Modal.Params.Init(core = CoreClient), onSuccess = {
            // Ethereum mainnet too, so a wallet that has never heard of Robinhood Chain can still sign in
            AppKit.setChains(listOf(robinhood, AppKitChainsPresets.ethChains.getValue("1")))
            AppKit.setDelegate(this)
            ready = true
            refresh()
        }) { e -> Log.w(TAG, "appkit: ${e.throwable.message}") }
    }

    /** getAccount() blocks, so it runs off the main thread. */
    fun refresh() {
        if (!ready) return
        scope.launch { account.value = runCatching { AppKit.getAccount()?.address }.getOrNull() }
    }

    fun disconnect() {
        if (!ready) return
        runCatching { AppKit.disconnect(onSuccess = { account.value = null }, onError = { account.value = null }) }
        account.value = null
    }

    /** The wallet's own deep link, so the user lands on the signing prompt instead of hunting for it. */
    fun walletLink(): String? = runCatching { (AppKit.getSession() as? Session.WalletConnectSession)?.redirect }.getOrNull()?.takeIf { it.isNotBlank() }

    /** Signs through the connected wallet. [openWallet] is handed the wallet's deep link once the request is out. */
    class WalletSigner(override val address: String, private val openWallet: (String) -> Unit) : Signer {
        override suspend fun personalSign(message: String): String {
            val params = JSONArray().put(Siwe.toHex(message)).put(address).toString()
            val sent = CompletableDeferred<Long>()
            AppKit.request(
                Request(method = "personal_sign", params = params),
                onSuccess = { r: SentRequestResult -> sent.complete((r as? SentRequestResult.WalletConnect)?.requestId ?: -1L) },
                onError = { sent.completeExceptionally(it) },
            )
            val id = sent.await()
            walletLink()?.let(openWallet)
            val result = withTimeoutOrNull(5 * 60_000L) {
                merge(
                    responses.filter { id == -1L || it.result.id == id }.map { it.result },
                    expired.filter { it == id }.map { throw Signer.Rejected("The request expired before the wallet answered.") },
                ).first()
            } ?: throw Signer.Rejected("No answer from the wallet.")
            return when (result) {
                is Modal.Model.JsonRpcResponse.JsonRpcResult -> {
                    val sig = result.result?.toString()?.trim()?.trim('"') ?: ""
                    if (!Regex("^0x[0-9a-fA-F]{130}$").matches(sig)) throw Signer.Rejected("The wallet returned something that is not a signature.")
                    sig
                }
                is Modal.Model.JsonRpcResponse.JsonRpcError -> throw Signer.Rejected(if (result.code == 4001 || result.code == 5000) "The signature was declined." else result.message)
            }
        }
    }

    // ── delegate ──
    override fun onSessionApproved(approvedSession: Modal.Model.ApprovedSession) {
        refresh()
        scope.launch {
            val a = runCatching { AppKit.getAccount()?.address }.getOrNull() ?: return@launch
            account.value = a
            approved.emit(a)
        }
    }
    override fun onSessionRejected(rejectedSession: Modal.Model.RejectedSession) { errors.tryEmit("The wallet declined the connection.") }
    override fun onSessionUpdate(updatedSession: Modal.Model.UpdatedSession) = refresh()
    override fun onSessionEvent(sessionEvent: Modal.Model.SessionEvent) = refresh()
    override fun onSessionExtend(session: Modal.Model.Session) {}
    override fun onSessionDelete(deletedSession: Modal.Model.DeletedSession) { account.value = null }
    override fun onSessionRequestResponse(response: Modal.Model.SessionRequestResponse) { responses.tryEmit(response) }
    override fun onProposalExpired(proposal: Modal.Model.ExpiredProposal) { errors.tryEmit("The connection request expired. Try again.") }
    override fun onRequestExpired(request: Modal.Model.ExpiredRequest) { expired.tryEmit(request.id) }
    override fun onConnectionStateChange(state: Modal.Model.ConnectionState) {}
    override fun onError(error: Modal.Model.Error) { Log.w(TAG, "error: ${error.throwable.message}"); errors.tryEmit(error.throwable.message ?: "Wallet error.") }
}
