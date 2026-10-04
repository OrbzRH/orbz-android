package app.orbz.android

import android.app.Application
import app.orbz.android.wallet.Wc

class OrbzApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Wc.init(this)
    }
}
