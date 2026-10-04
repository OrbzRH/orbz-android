package app.orbz.android.auth

/** Something that can sign a message for one address. The app never holds a key: a wallet does the signing. */
interface Signer {
    val address: String
    /** The user turned the request down in the wallet. */
    class Rejected(message: String = "The signature was declined.") : Exception(message)
    /** Returns the 65-byte signature as 0x-hex. Throws [Rejected] when the user declines. */
    suspend fun personalSign(message: String): String
}
