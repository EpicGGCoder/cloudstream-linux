package androidx.appcompat.app

import android.content.Context
import android.content.SharedPreferences

/**
 * JVM stub for Android's [androidx.appcompat.app.AppCompatActivity].
 *
 * CloudStream plugins sometimes hold a reference to AppCompatActivity
 * (the host activity) to show dialogs or access SharedPreferences.
 * On JVM desktop there's no Activity — this stub exists only so plugin
 * bytecode can link and load without crashing at class-resolution time.
 *
 * Extends our android.content.Context stub which already provides the
 * getSharedPreferences / getApplicationContext methods plugins need.
 */
open class AppCompatActivity : Context() {

    /** Stub: returns an empty FragmentManager. */
    fun getSupportFragmentManager(): androidx.fragment.app.FragmentManager {
        return androidx.fragment.app.FragmentManager
    }
}
