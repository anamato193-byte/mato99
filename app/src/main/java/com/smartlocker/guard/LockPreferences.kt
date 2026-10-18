package com.smartlocker.guard

import android.content.Context
import android.content.SharedPreferences

class LockPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_locker_prefs", Context.MODE_PRIVATE)

    var isMasterLockEnabled: Boolean
        get() = prefs.getBoolean("master_lock_enabled", true)
        set(value) = prefs.edit().putBoolean("master_lock_enabled", value).apply()

    var isUninstallProtectionEnabled: Boolean
        get() = prefs.getBoolean("uninstall_protection", true)
        set(value) = prefs.edit().putBoolean("uninstall_protection", value).apply()

    var pinCode: String
        get() = prefs.getString("pin_code", "1234") ?: "1234"
        set(value) = prefs.edit().putString("pin_code", value).apply()

    // القائمة البيضاء: التطبيقات المسموح بفتحها بدون بصمة أو رمز
    var allowedPackages: Set<String>
        get() = prefs.getStringSet("allowed_packages", setOf("com.android.dialer", "com.whatsapp")) ?: emptySet()
        set(value) = prefs.edit().putStringSet("allowed_packages", value).apply()

    var lastUnlockedPackage: String
        get() = prefs.getString("last_unlocked_pkg", "") ?: ""
        set(value) = prefs.edit().putString("last_unlocked_pkg", value).apply()

    fun toggleAllowedPackage(pkg: String): Boolean {
        val current = allowedPackages.toMutableSet()
        val isNowAllowed = if (current.contains(pkg)) {
            current.remove(pkg)
            false
        } else {
            current.add(pkg)
            true
        }
        allowedPackages = current
        return isNowAllowed
    }
}
