package app.orbz.android.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orbz.android.ui.AppVm
import app.orbz.android.ui.Ink
import app.orbz.android.ui.Mono
import app.orbz.android.ui.shortAddr

private data class Tab(val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("Home", Icons.Outlined.Home),
    Tab("Chat", Icons.Outlined.Email),
    Tab("Check", Icons.Outlined.Search),
    Tab("Receipts", Icons.Outlined.DateRange),
)

/** The signed-in app: four tabs and a settings page, the address always in view. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Shell(vm: AppVm, address: String) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var settings by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        containerColor = Ink.bg,
        topBar = {
            TopAppBar(
                title = { Text("Orbz", fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Ink.marbleLight) },
                actions = {
                    Text(shortAddr(address), style = Mono.copy(color = Ink.vein), modifier = Modifier.padding(end = 4.dp).semantics { testTag = "address" })
                    IconButton(onClick = { settings = !settings }) { Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = if (settings) Ink.ember else Ink.vein) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink.bg),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Ink.night) {
                TABS.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = !settings && tab == i,
                        onClick = { tab = i; settings = false },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Ink.bg, selectedTextColor = Ink.marbleLight, indicatorColor = Ink.ember, unselectedIconColor = Ink.dust, unselectedTextColor = Ink.dust),
                        modifier = Modifier.semantics { testTag = "tab-${t.label}" },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when {
                settings -> SettingsScreen(vm, address)
                tab == 0 -> HomeScreen(vm)
                tab == 1 -> ChatScreen()
                tab == 2 -> CheckScreen()
                else -> ReceiptsScreen()
            }
        }
    }
}
