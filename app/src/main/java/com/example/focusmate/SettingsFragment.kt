import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.R
import androidx.preference.SwitchPreference
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SettingsFragment : PreferenceFragmentCompat() {
    private val blockedApps = mutableSetOf<String>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // User Account (Static Display)
        findPreference<Preference>("user_account")?.summary = "alex.johnson@example.com"

        // Manage Account (Navigate to a new screen or activity)
        findPreference<Preference>("manage_account")?.setOnPreferenceClickListener {
            // Launch ManageAccountActivity or handle account management
            true
        }

        // Cloud Sync (Placeholder Logic)
        findPreference<SwitchPreference>("cloud_sync")?.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue as Boolean
            if (isEnabled) {
                // Add your custom sync logic here
            } else {
                // Disable sync logic
            }
            true
        }

        // Notifications
        findPreference<SwitchPreference>("notifications")?.setOnPreferenceChangeListener { _, newValue ->
            // Handle notification enable/disable
            true
        }

        // Blocked Apps (Custom Logic for Adding Apps)
        findPreference<EditTextPreference>("add_blocked_app")?.setOnPreferenceChangeListener { _, newValue ->
            val appName = newValue as String
            if (appName.isNotEmpty()) {
                blockedApps.add(appName.lowercase())
                // Save to SharedPreferences
                saveBlockedApps()
                true
            } else {
                false
            }
        }

        // Custom layout for Blocked Apps
        val blockedAppsCategory = findPreference<PreferenceCategory>("blocked_apps_category")
        val view = layoutInflater.inflate(R.layout.blocked_apps_layout, null) as ViewGroup
        blockedAppsCategory?.removeAll()
        blockedAppsCategory?.addPreference(Preference(context).apply {
            widgetLayoutResource = R.layout.blocked_apps_layout
            isSelectable = false
        })

        // Initialize RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.blocked_apps_recycler)
        recyclerView.layoutManager = LinearLayoutManager(context)
        val adapter = BlockedAppsAdapter(blockedApps) { app -> blockedApps.remove(app); saveBlockedApps() }
        recyclerView.adapter = adapter

        // Theme
        findPreference<ListPreference>("theme_preference")?.setOnPreferenceChangeListener { _, newValue ->
            val theme = newValue as String
            when (theme) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "auto" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            true
        }
    }

    private fun saveBlockedApps() {
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("blocked_apps", blockedApps).apply()
    }

    override fun onResume() {
        super.onResume()
        // Load blocked apps on resume to reflect changes
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        blockedApps.addAll(sharedPreferences.getStringSet("blocked_apps", emptySet()) ?: emptySet())
    }

    // Nested Adapter Class
    class BlockedAppsAdapter(
        private val apps: MutableSet<String>,
        private val onRemove: (String) -> Unit
    ) : RecyclerView.Adapter<BlockedAppsAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_blocked_app, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps.elementAt(position)
            holder.appName.text = app
            holder.removeButton.setOnClickListener { onRemove(app) }
        }

        override fun getItemCount() = apps.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val appName: TextView = itemView.findViewById(android.R.id.text1)
            val removeButton: ImageView = itemView.findViewById(android.R.id.button1)
        }
    }
}