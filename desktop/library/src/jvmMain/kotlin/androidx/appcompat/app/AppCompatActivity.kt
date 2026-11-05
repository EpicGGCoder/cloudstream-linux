package androidx.appcompat.app

import android.content.Context
import android.content.NoopSharedPreferences
import android.content.SharedPreferences

/**
 * JVM stub for Android's [androidx.appcompat.app.AppCompatActivity].
 *
 * CloudStream plugins sometimes hold a reference to AppCompatActivity
 * (the host activity) to show dialogs or access SharedPreferences.
 * On JVM desktop there's no Activity — this stub exists only so plugin
 * bytecode can link and load without crashing at class-resolution time.
 *
 * Methods like `getSharedPreferences` and `getSupportFragmentManager`
 * are stubbed to return sensible defaults (empty prefs, empty fragment
 * manager) so plugins that call them don't NPE during load().
 */
open class AppCompatActivity : Context() {

    /** Stub: returns an empty in-memory preferences map. */
    fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        return NoopSharedPreferences
    }

    /** Stub: returns an empty FragmentManager. */
    fun getSupportFragmentManager(): androidx.fragment.app.FragmentManager {
        return androidx.fragment.app.FragmentManager
    }
}
