package app.orbz.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.navigation.BottomSheetNavigator
import androidx.compose.material.navigation.ModalBottomSheetLayout
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.orbz.android.Config
import app.orbz.android.ui.screens.ConnectScreen
import app.orbz.android.ui.screens.Shell
import com.reown.appkit.ui.AppKitTheme
import com.reown.appkit.ui.appKitGraph
import com.reown.appkit.ui.openAppKit

/**
 * One navigation host: the app itself, and the wallet sheet AppKit opens on top of it. The sheet is a Material
 * bottom sheet driven by navigation, which is how AppKit wants to be hosted.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Root(vm: AppVm = viewModel()) {
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val bottomSheetNavigator = remember(sheetState) { BottomSheetNavigator(sheetState) }
    val nav = rememberNavController(bottomSheetNavigator)
    // the wallet sheet in the app's own dark palette, with the ember as its accent
    AppKitTheme(
        mode = AppKitTheme.Mode.DARK,
        darkColors = AppKitTheme.provideDarkAppKitColor(
            accent100 = Ink.ember, accent90 = Ink.emberGlow, accent80 = Ink.ember.copy(alpha = 0.8f),
            background = AppKitTheme.provideBackgroundDarkColorPalette(color100 = Ink.bg, color125 = Ink.bg2, color150 = Ink.bg2, color175 = Ink.night),
        ),
    ) {
    ModalBottomSheetLayout(
        bottomSheetNavigator = bottomSheetNavigator,
        sheetBackgroundColor = Color.Transparent,
        sheetElevation = 0.dp,
        scrimColor = Color(0x99000000),
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        NavHost(nav, startDestination = "app") {
            composable("app") {
                val phase by vm.phase.collectAsState()
                Box(Modifier.fillMaxSize().background(Ink.bg)) {
                    when (phase) {
                        Phase.Booting -> CircularProgressIndicator(color = Ink.ember, modifier = Modifier.align(Alignment.Center))
                        is Phase.SignedIn -> Shell(vm, (phase as Phase.SignedIn).address)
                        else -> ConnectScreen(vm, onConnect = { nav.openAppKit { e -> vm.error.value = e.message ?: "The wallet sheet could not open." } })
                    }
                }
            }
            if (Config.walletConnectReady) appKitGraph(nav)
        }
    }
    }
}
