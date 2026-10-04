package app.orbz.android.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// What the console's API sends, as it sends it. Money is micro-USD in strings (bigints on the server), so
// nothing is rounded before the screen formats it.

@Serializable
data class Challenge(val nonce: String, val domain: String, val uri: String, val chainId: Int, val issuedAt: String, val expirationTime: String)

@Serializable
data class Me(val address: String? = null, val sandbox: Boolean = false, val tokenLaunched: Boolean = false)

@Serializable
data class Summary(
    val address: String,
    val sandbox: Boolean = false,
    val tier: String = "",
    val tierName: String = "",
    val token: TokenHolding? = null,
    val eth: String? = null,
    val balance: Balance,
    val buckets: List<Bucket> = emptyList(),
    val month: Month? = null,
    val epoch: Epoch,
)

/** GET /v1/key: what a key sees. Money here is decimal dollars, unlike the console's micro-USD. */
@Serializable
data class KeyInfo(
    val wallet: String,
    val tier: String = "",
    @SerialName("tier_name") val tierName: String = "",
    val balance: String,
    val open: String,
    val reserved: String,
    val buckets: List<KeyBucket> = emptyList(),
    val epoch: KeyEpoch,
)

@Serializable
data class KeyBucket(val epoch: Int, val amount: String, val remaining: String, @SerialName("starts_at") val startsAt: String, @SerialName("expires_at") val expiresAt: String)

@Serializable
data class KeyEpoch(val current: Int, @SerialName("next_at") val nextAt: String)

@Serializable
data class TokenHolding(val ca: String, val balance: String? = null, val whole: String? = null)

@Serializable
data class Balance(val open: String, val reserved: String, val available: String)

@Serializable
data class Bucket(val epoch: Int, val amount: String, val remaining: String, val startsAt: String, val expiresAt: String)

@Serializable
data class Month(val label: String, val used: String, val expired: String, val requests: Int)

@Serializable
data class Epoch(val n: Int, val startsAt: Long, val nextAt: Long)

@Serializable
data class ReceiptsPage(val current: Int, val data: List<Receipt>, @SerialName("next_from") val nextFrom: Int? = null)

@Serializable
data class Receipt(
    val epoch: Int,
    @SerialName("starts_at") val startsAt: String,
    val status: String,
    val wallets: Int,
    @SerialName("fees_in") val feesIn: String? = null,
    val granted: String,
    val used: String,
    val expired: String,
    val open: String,
    val burn: Burn? = null,
)

@Serializable
data class Burn(@SerialName("orbz_burned") val orbzBurned: String? = null, @SerialName("usdg_in") val usdgIn: String? = null, val tx: String? = null)

/** One streamed answer: the words as they arrive, then what it cost. */
sealed class Stream {
    data class Delta(val text: String) : Stream()
    data class Done(val finish: String, val cost: String?, val balanceAfter: String?, val tokensOut: Int?) : Stream()
}
