package com.lagradost.cloudstream3

import android.content.Context

/**
 * JVM stub for CS3's `com.lagradost.cloudstream3.CloudStreamApp`.
 *
 * In the Android app, CloudStreamApp is the Application class that holds
 * app-wide state. Some plugins (e.g., phisher98's MultiMoviesProvider)
 * reference it via DonationManager.checkAndShow() which calls
 * CloudStreamApp.getInstance(). On JVM desktop we provide this stub so
 * the plugin bytecode links without crashing.
 */
open class CloudStreamApp : Context() {

    companion object {
        @JvmStatic
        fun getInstance(): CloudStreamApp {
            return InstanceHolder.instance
        }
    }

    private object InstanceHolder {
        val instance = CloudStreamApp()
    }
}
