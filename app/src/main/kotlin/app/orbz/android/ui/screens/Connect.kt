package app.orbz.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import app.orbz.android.Config
import app.orbz.android.auth.Signer
import app.orbz.android.ui.AppVm
import app.orbz.android.ui.Display
import app.orbz.android.ui.ErrorLine
import app.orbz.android.ui.Field
import app.orbz.android.ui.Ghost
import app.orbz.android.ui.Ink
import app.orbz.android.ui.Label
import app.orbz.android.ui.Phase
import app.orbz.android.ui.Primary
import app.orbz.android.ui.Sheet
import app.orbz.android.ui.shortAddr

/** Debug builds carry a local signer for emulator runs. Release builds do not have the class, so this is null there. */
private fun testSigner(privateKeyHex: String): Signer? = runCatching {
    Class.forName("app.orbz.android.auth.TestSigner").getDeclaredConstructor(String::class.java).newInstance(privateKeyHex) as Signer
}.getOrNull()
private val hasTestSigner: Boolean by lazy { runCatching { Class.forName("app.orbz.android.auth.TestSigner"); true }.getOrDefault(false) }

@Composable
fun ConnectScreen(vm: AppVm, onConnect: () -> Unit) {
    val phase by vm.phase.collectAsState()
    val wallet by vm.wallet.collectAsState()
    val error by vm.error.collectAsState()
    val uri = LocalUriHandler.current
    var testKey by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Top) {
        Spacer(Modifier.height(36.dp))
        Text("ORBZ", style = Label)
        Spacer(Modifier.height(10.dp))
        Text("AI credits from trading fees.", style = Display)
        Spacer(Modifier.height(12.dp))
        Text("Hold \$ORBZ, use Orbz Opus. Your credit, your balance and the token checks, on your phone.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(28.dp))

        Sheet {
            Text("Signing in is one signature.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Your wallet signs a short message that names this app and expires in minutes. No transaction, no network fee, no token approval. Orbz never asks for one.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            when (val p = phase) {
                is Phase.Signing -> {
                    Row { CircularProgressIndicator(color = Ink.ember, modifier = Modifier.height(20.dp).padding(end = 10.dp)); Text(if (p.address == "key") "Checking the key…" else "Check your wallet and sign as ${shortAddr(p.address)}.", style = MaterialTheme.typography.bodyMedium) }
                    Spacer(Modifier.height(10.dp))
                    Ghost("Cancel", onClick = { vm.cancelSignIn() })
                }
                else -> {
                    if (!Config.walletConnectReady) {
                        Text("Connecting a wallet from the app arrives in the next release. For now, sign in with an API key below.", color = Ink.vein, style = MaterialTheme.typography.bodySmall)
                    } else if (wallet != null) {
                        Primary("Sign in as ${shortAddr(wallet!!)}", Modifier.fillMaxWidth(), onClick = { vm.signInWithWallet() })
                        TextButton(onClick = { vm.signOut() }) { Text("Use another wallet", color = Ink.vein) }
                    } else {
                        Primary("Connect wallet", Modifier.fillMaxWidth(), onClick = onConnect)
                    }
                }
            }
            ErrorLine(error)
        }

        Spacer(Modifier.height(16.dp))
        Sheet {
            Text("Sign in with an API key", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Sign in at use.orbz.app with your wallet, open Keys, create a key and paste it here. It is stored encrypted on this phone and can be revoked from the console at any time.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Field(apiKey, { apiKey = it }, "sk-orbz-…", Modifier.fillMaxWidth(), mono = true, tag = "apiKey")
            Spacer(Modifier.height(8.dp))
            Primary("Sign in with key", Modifier.fillMaxWidth(), enabled = phase !is Phase.Signing && apiKey.isNotBlank(), tag = "keySignIn", onClick = { vm.signInWithKey(apiKey) })
            TextButton(onClick = { uri.openUri("${Config.API_BASE.takeIf { it.startsWith("https") } ?: "https://use.orbz.app"}/keys") }) { Text("Open the Keys page", color = Ink.vein) }
        }

        if (hasTestSigner) {
            Spacer(Modifier.height(16.dp))
            Sheet {
                Text("TEST KEY · DEBUG BUILD ONLY", style = Label)
                Spacer(Modifier.height(8.dp))
                Field(testKey, { testKey = it }, "Private key, 0x…", Modifier.fillMaxWidth(), mono = true, tag = "testKey")
                Spacer(Modifier.height(8.dp))
                Ghost("Sign in with test key", tag = "testSignIn", enabled = phase !is Phase.Signing, onClick = { testSigner(testKey.trim())?.let { vm.signIn(it) } })
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = { uri.openUri(Config.SITE) }) { Text("What is Orbz", color = Ink.vein) }
            TextButton(onClick = { uri.openUri("${Config.SITE}/docs") }) { Text("Docs", color = Ink.vein) }
        }
        Text("Version ${Config.VERSION}", style = MaterialTheme.typography.bodySmall)
    }
}
