package com.smartlocker.guard

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class LockOverlayActivity : AppCompatActivity() {

    private lateinit var prefs: LockPreferences
    private var targetPackage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = LockPreferences(this)
        targetPackage = intent.getStringExtra("TARGET_PACKAGE") ?: ""
        val reason = intent.getStringExtra("REASON") ?: "يرجى التحقق من البصمة أو الرمز السري لفتح التطبيق"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF0F172A.toInt())
            setPadding(64, 64, 64, 64)
        }

        val titleView = TextView(this).apply {
            text = "حارس التطبيقات الذكي"
            textSize = 24f
            setTextColor(0xFF10B981.toInt())
            gravity = Gravity.CENTER
        }

        val reasonView = TextView(this).apply {
            text = reason
            textSize = 16f
            setTextColor(0xFFE2E8F0.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 48)
        }

        val bioBtn = Button(this).apply {
            text = "الفتح ببصمة الهاتف أو رمز قفل الهاتف الحالي"
            setBackgroundColor(0xFF059669.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener { showBiometricPrompt() }
        }

        val cancelBtn = Button(this).apply {
            text = "العودة للشاشة الرئيسية"
            setBackgroundColor(0xFF334155.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener { goHome() }
        }

        root.addView(titleView)
        root.addView(reasonView)
        root.addView(bioBtn)
        root.addView(cancelBtn)
        setContentView(root)

        showBiometricPrompt()
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onAuthSuccess()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("تأكيد البصمة")
            .setSubtitle("ضع بصمتك لفتح القفل أو السماح بالإجراء")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun onAuthSuccess() {
        if (targetPackage == "UNINSTALL_GUARD") {
            prefs.lastUnlockedPackage = "UNINSTALL_VERIFIED"
        } else {
            prefs.lastUnlockedPackage = targetPackage
        }
        finish()
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    override fun onBackPressed() {
        goHome()
    }
}
