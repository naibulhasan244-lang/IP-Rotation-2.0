package com.yourname.iprotation2

import android.app.Application
import com.yourname.iprotation2.data.network.SocksAuth
import dagger.hilt.android.HiltAndroidApp
import java.net.Authenticator

@HiltAndroidApp
class IPRotationApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Needed for SOCKS5 username/password authentication.
        Authenticator.setDefault(SocksAuth)
    }
}
