package com.example.focusmate

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationBlockerService : NotificationListenerService() {

    private var blockedApps: Set<String> = setOf()

    override fun onCreate() {
        super.onCreate()
        loadBlockedApps()
    }

    // This method is called by the system whenever a new notification is posted.
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        // Reload the list every time to get the latest user selections.
        loadBlockedApps()

        val packageName = sbn.packageName

        // The core logic: If the app that sent the notification is in our blocked list...
        if (blockedApps.contains(packageName)) {
            // ...cancel the notification immediately.
            cancelNotification(sbn.key)
        }
    }

    private fun loadBlockedApps() {
        // Read the set of blocked package names from the same SharedPreferences file.
        val sharedPreferences = getSharedPreferences("BlockedAppsPrefs", Context.MODE_PRIVATE)
        blockedApps = sharedPreferences.getStringSet("blocked_apps_set", setOf()) ?: setOf()
    }

    // We also need to handle when the listener service connects
    override fun onListenerConnected() {
        super.onListenerConnected()
        loadBlockedApps()
    }
}
