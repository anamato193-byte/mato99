package com.smartlocker.guard

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class UninstallProtectionAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "تم تفعيل حماية التطبيق من الحذف بنجاح", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // عند محاولة إلغاء صلاحية المسؤول لحذف التطبيق، يتم إطلاق شاشة البصمة فوراً
        val lockIntent = Intent(context, LockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("TARGET_PACKAGE", "UNINSTALL_GUARD")
            putExtra("REASON", "محاولة إلغاء الحماية أو حذف التطبيق! يلزم إدخال البصمة")
        }
        context.startActivity(lockIntent)
        return "تحذير أمني: لا يمكن إلغاء التفعيل أو حذف البرنامج إلا بعد إدخال البصمة داخل التطبيق."
    }
}
