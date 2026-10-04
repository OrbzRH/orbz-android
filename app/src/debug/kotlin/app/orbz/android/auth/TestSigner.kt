package app.orbz.android.auth

import org.bouncycastle.crypto.digests.KeccakDigest
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger

/**
 * DEBUG BUILDS ONLY. Signs like a wallet would (EIP-191 personal_sign) with a throwaway key, so the whole
 * sign-in can run on an emulator with no wallet app. The class does not exist in release builds: the connect
 * screen looks for it by name and finds nothing.
 */
class TestSigner(privateKeyHex: String) : Signer {
    private val params = CustomNamedCurves.getByName("secp256k1")
    private val domain = ECDomainParameters(params.curve, params.g, params.n, params.h)
    private val d = BigInteger(privateKeyHex.removePrefix("0x"), 16)
    private val pub: ECPoint = domain.g.multiply(d).normalize()
    override val address: String = "0x" + keccak(pub.getEncoded(false).copyOfRange(1, 65)).copyOfRange(12, 32).hex()

    override suspend fun personalSign(message: String): String {
        val body = message.toByteArray(Charsets.UTF_8)
        val hash = keccak("\u0019Ethereum Signed Message:\n${body.size}".toByteArray(Charsets.UTF_8) + body)
        val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        signer.init(true, ECPrivateKeyParameters(d, domain))
        val (r, s0) = signer.generateSignature(hash).let { it[0] to it[1] }
        val s = if (s0 > domain.n.shiftRight(1)) domain.n.subtract(s0) else s0
        val v = (0..1).first { recover(hash, r, s, it)?.equals(pub) == true }
        return "0x" + r.pad32() + s.pad32() + "%02x".format(27 + v)
    }

    /** Q = r^-1 (s·R − e·G), with R the curve point whose x is r and whose parity is v. */
    private fun recover(hash: ByteArray, r: BigInteger, s: BigInteger, v: Int): ECPoint? = runCatching {
        val x = r.toByteArray().let { if (it.size > 32) it.copyOfRange(it.size - 32, it.size) else ByteArray(32 - it.size) + it }
        val rPoint = domain.curve.decodePoint(byteArrayOf((2 + v).toByte()) + x)
        val e = BigInteger(1, hash)
        val rInv = r.modInverse(domain.n)
        rPoint.multiply(s).subtract(domain.g.multiply(e)).multiply(rInv).normalize()
    }.getOrNull()

    private fun keccak(b: ByteArray): ByteArray = KeccakDigest(256).let { it.update(b, 0, b.size); ByteArray(32).also { out -> it.doFinal(out, 0) } }
    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
    private fun BigInteger.pad32() = toString(16).padStart(64, '0')
}
