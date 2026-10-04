package app.orbz.android.auth

import app.orbz.android.net.Challenge

/**
 * The exact EIP-4361 message the console issues and checks. The server parses it strictly: one extra space and
 * the signature is refused, so this mirrors `lib/siwe.ts` line for line.
 */
object Siwe {
    const val LOGIN_STATEMENT = "Sign in to Orbz. This signs a message only: no transaction, no network fee."

    fun build(c: Challenge, address: String): String = listOf(
        "${c.domain} wants you to sign in with your Ethereum account:",
        address,
        "",
        LOGIN_STATEMENT,
        "",
        "URI: ${c.uri}",
        "Version: 1",
        "Chain ID: ${c.chainId}",
        "Nonce: ${c.nonce}",
        "Issued At: ${c.issuedAt}",
        "Expiration Time: ${c.expirationTime}",
    ).joinToString("\n")

    /** personal_sign takes the message as hex bytes: 0x + utf-8. */
    fun toHex(message: String): String = message.toByteArray(Charsets.UTF_8).joinToString("", prefix = "0x") { "%02x".format(it) }
}
