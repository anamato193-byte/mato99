package com.smartlocker.guard

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: LockPreferences
    private lateinit var centerShortcutBtn: Button
    private lateinit var statusLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = LockPreferences(this)
        setupUI()
    }

    private fun setupUI() {
        val root = ScrollView(this).apply {
            setBackgroundColor(0xFF0F172A.toInt())
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 64, 48, 64)
        }

        val header = TextView(this).apply {
            text = "حارس التطبيقات الذكي"
            textSize = 26f
            setTextColor(0xFF10B981.toInt())
            gravity = Gravity.CENTER
        }

        statusLabel = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFFCBD5E1.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 48)
        }

        // الزر الرئيسي الكبير والواضح في منتصف الشاشة
        centerShortcutBtn = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(540, 540).apply {
                topMargin = 32
                bottomMargin = 32
            }
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                onCenterShortcutClicked()
            }
            setOnLongClickListener {
                showQuickAllowedAppsDialog()
                true
            }
        }

        val quickAllowedBtn = Button(this).apply {
            text = "عرض وفتح التطبيقات المسموح بها فقط (وصول سريع)"
            setBackgroundColor(0xFF0284C7.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener { showQuickAllowedAppsDialog() }
        }

        val selectAppsBtn = Button(this).apply {
            text = "تحديد التطبيقات المسموح بها (مفتوحة بدون بصمة)"
            setBackgroundColor(0xFF334155.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                authenticateOwner("تعديل قائمة التطبيقات المسموحة") {
                    showAppWhitelistSelector()
                }
            }
        }

        val antiUninstallBtn = Button(this).apply {
            text = "تفعيل حماية منع الحذف إلا بالبصمة (Device Admin)"
            setBackgroundColor(0xFF7C3AED.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener { enableUninstallProtection() }
        }

        val accessibilityBtn = Button(this).apply {
            text = "تفعيل خدمة قفل البرامج في النظام (Accessibility)"
            setBackgroundColor(0xFF475569.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        container.addView(header)
        container.addView(statusLabel)
        container.addView(centerShortcutBtn)
        container.addView(quickAllowedBtn)
        container.addView(selectAppsBtn)
        container.addView(antiUninstallBtn)
        container.addView(accessibilityBtn)

        root.addView(container)
        setContentView(root)
        refreshCenterShortcutState()
    }

    private fun refreshCenterShortcutState() {
        val active = prefs.isMasterLockEnabled
        val shape = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (active) 0xFF059669.toInt() else 0xFFDC2626.toInt())
            setStroke(10, if (active) 0xFF34D399.toInt() else 0xFFF87171.toInt())
        }
        centerShortcutBtn.background = shape
        centerShortcutBtn.text = if (active) {
            "القفل الشامل مفعّل\n\nاضغط لفتح التطبيقات المسموحة\nأو إيقاف القفل بالبصمة"
        } else {
            "القفل الشامل متوقف\n\nاضغط هنا لتفعيل قفل كافة البرامج فوراً"
        }
        statusLabel.text = if (active) {
            "الحالة: كل البرامج مقفلة بالبصمة ما عدا (${prefs.allowedPackages.size}) تطبيقات مسموح بها"
        } else {
            "الحالة: الحماية متوقفة مؤقتاً - اضغط الزر الأوسط للتفعيل"
        }
    }

    private fun onCenterShortcutClicked() {
        if (!prefs.isMasterLockEnabled) {
            prefs.isMasterLockEnabled = true
            prefs.lastUnlockedPackage = ""
            refreshCenterShortcutState()
            Toast.makeText(this, "تم قفل كافة البرامج باستثناء المسموح بها!", Toast.LENGTH_SHORT).show()
        } else {
            val options = arrayOf(
                "فتح نافذة التطبيقات المسموح بها (بدون بصمة)",
                "إيقاف القفل الشامل مؤقتاً (يتطلب البصمة)"
            )
            AlertDialog.Builder(this)
                .setTitle("اختصار التحكم السريع")
                .setItems(options) { _, which ->
                    if (which == 0) {
                        showQuickAllowedAppsDialog()
                    } else {
                        authenticateOwner("إيقاف القفل الشامل") {
                            prefs.isMasterLockEnabled = false
                            refreshCenterShortcutState()
                        }
                    }
                }
                .show()
        }
    }

    private fun showQuickAllowedAppsDialog() {
        val pm = packageManager
        val allowedList = prefs.allowedPackages.toList()
        val labels = allowedList.map { pkg ->
            try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (e: Exception) {
                pkg
            }
        }.toTypedArray()

        if (labels.isEmpty()) {
            Toast.makeText(this, "لم تحدد أي تطبيقات مسموح بها بعد!", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("التطبيقات المسموح بها (تفتح فوراً بدون بصمة)")
            .setItems(labels) { _, idx ->
                val launchIntent = pm.getLaunchIntentForPackage(allowedList[idx])
                if (launchIntent != null) startActivity(launchIntent)
            }
            .setPositiveButton("إغلاق", null)
            .show()
    }

    private fun showAppWhitelistSelector() {
        val pm = packageManager
        val installedApps = pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null && it.packageName != packageName }
            .sortedBy { pm.getApplicationLabel(it).toString() }

        val appNames = installedApps.map { pm.getApplicationLabel(it).toString() }.toTypedArray()
        val currentAllowed = prefs.allowedPackages.toMutableSet()
        val checkedItems = BooleanArray(installedApps.size) { idx ->
            currentAllowed.contains(installedApps[idx].packageName)
        }

        AlertDialog.Builder(this)
            .setTitle("حدد التطبيقات المسموح بفتحها بدون بصمة")
            .setMultiChoiceItems(appNames, checkedItems) { _, which, isChecked ->
                val pkg = installedApps[which].packageName
                if (isChecked) currentAllowed.add(pkg) else currentAllowed.remove(pkg)
            }
            .setPositiveButton("حفظ القائمة") { _, _ ->
                prefs.allowedPackages = currentAllowed
                refreshCenterShortcutState()
                Toast.makeText(this, "تم تحديث قائمة التطبيقات المسموح بها!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun enableUninstallProtection() {
        val compName = ComponentName(this, UninstallProtectionAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "تفعيل هذه الميزة يمنع أي شخص من حذف البرنامج إلا بعد إدخال بصمتك.")
        }
        startActivity(intent)
    }

    private fun authenticateOwner(reason: String, onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("التحقق من بصمة المالك")
            .setSubtitle(reason)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
