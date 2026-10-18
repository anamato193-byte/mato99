package com.smartlocker.guard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class AppLockAccessibilityService : AccessibilityService() {

    private lateinit var prefs: LockPreferences

    // حزم النظام الحساسة (إعدادات الحذف ومثبت الحزم) لحماية التطبيق من الحذف إلا بالبصمة
    private val installerPackages = setOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.miui.packageinstaller",
        "com.samsung.android.packageinstaller"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = LockPreferences(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkgName = event.packageName?.toString() ?: return

        // تجاهل التطبيق نفسه أو واجهة القفل الخاصة به
        if (pkgName == packageName || pkgName.contains("systemui")) return

        // 1. حماية ضد الحذف: إذا حاول أي شخص فتح شاشة إلغاء التثبيت أو إعدادات مسؤول الجهاز
        if (prefs.isUninstallProtectionEnabled) {
            val eventText = event.text?.joinToString(" ") ?: ""
            val isUninstallAttempt = installerPackages.contains(pkgName) ||
                (pkgName == "com.android.settings" && (
                    eventText.contains("حارس التطبيقات") ||
                    eventText.contains("Smart App Locker") ||
                    eventText.contains("إلغاء التثبيت") ||
                    eventText.contains("Uninstall") ||
                    eventText.contains("Device admin") ||
                    eventText.contains("مسؤول الجهاز")
                ))

            if (isUninstallAttempt && prefs.lastUnlockedPackage != "UNINSTALL_VERIFIED") {
                launchLockScreen(pkgName, "حماية عدم الحذف: يجب التحقق من البصمة أولاً!")
                return
            }
        }

        // 2. قفل كافة التطبيقات غير الموجودة في القائمة البيضاء (المسموح بها)
        if (!prefs.isMasterLockEnabled) return

        val allowed = prefs.allowedPackages
        if (!allowed.contains(pkgName) && prefs.lastUnlockedPackage != pkgName) {
            launchLockScreen(pkgName, "هذا التطبيق مقفل - يتطلب البصمة أو الرمز للفتح")
        }
    }

    private fun launchLockScreen(targetPackage: String, reason: String) {
        val lockIntent = Intent(this, LockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("TARGET_PACKAGE", targetPackage)
            putExtra("REASON", reason)
        }
        startActivity(lockIntent)
    }

    override fun onInterrupt() {}
}
