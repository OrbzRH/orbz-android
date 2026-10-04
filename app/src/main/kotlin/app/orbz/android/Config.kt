package app.orbz.android

/** Build-time settings, read once. Nothing here is secret: the project id is a public site key. */
object Config {
    const val API_BASE: String = BuildConfig.API_BASE
    const val SITE: String = BuildConfig.SITE
    const val VERSION: String = BuildConfig.VERSION_NAME
    const val WC_PROJECT_ID: String = BuildConfig.WC_PROJECT_ID
    val walletConnectReady: Boolean get() = WC_PROJECT_ID.isNotBlank()

    const val CHAIN_ID = 4663
    const val CHAIN_NAME = "Robinhood Chain"
    const val RPC_URL = "https://rpc.mainnet.chain.robinhood.com"
    const val EXPLORER = "https://robin.etherscan.io"
    const val FLOOR_TOKENS = 100_000L
    const val EPOCH_SECONDS = 1800L
    const val RETURN_LINK = "orbz://request"
}
