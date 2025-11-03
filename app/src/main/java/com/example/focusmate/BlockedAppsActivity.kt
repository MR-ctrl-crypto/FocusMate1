package com.example.focusmate

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Data class to hold the information we need for each app
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    var isSelected: Boolean = false
)

class BlockedAppsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var appAdapter: AppAdapter

    // CHANGED: Two lists are now used. One for all apps, one for what's displayed.
    private var allApps = mutableListOf<AppInfo>()
    private var currentlyDisplayedApps = mutableListOf<AppInfo>()

    private lateinit var blockedAppsPrefs: MutableSet<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_apps)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Load the set of currently blocked apps from SharedPreferences
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        blockedAppsPrefs = sharedPreferences.getStringSet("blocked_apps", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        // Set up the RecyclerView
        recyclerView = findViewById(R.id.apps_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // CHANGED: Initialize the adapter with the list that will be filtered.
        appAdapter = AppAdapter(currentlyDisplayedApps)
        recyclerView.adapter = appAdapter

        // Start loading the list of apps
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        // Loading apps can be slow, so we do it on a background thread
        GlobalScope.launch(Dispatchers.IO) {
            val pm: PackageManager = packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val appList = mutableListOf<AppInfo>()

            for (packageInfo in packages) {
                // Filter out system apps and this app itself
                if ((packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && packageInfo.packageName != packageName) {
                    val appName = packageInfo.loadLabel(pm).toString()
                    val packageName = packageInfo.packageName
                    val appIcon = packageInfo.loadIcon(pm)
                    val isSelected = blockedAppsPrefs.contains(packageName)
                    appList.add(AppInfo(appName, packageName, appIcon, isSelected))
                }
            }
            // Sort the list alphabetically
            appList.sortBy { it.name }

            // Switch back to the main thread to update the UI
            withContext(Dispatchers.Main) {
                // CHANGED: Populate both lists and notify the adapter.
                allApps.clear()
                allApps.addAll(appList)
                currentlyDisplayedApps.clear()
                currentlyDisplayedApps.addAll(allApps)
                appAdapter.notifyDataSetChanged() // Refresh the list in the UI
            }
        }
    }

    // Save the selections when the activity is paused or destroyed
    override fun onPause() {
        super.onPause()
        // IMPORTANT: We filter the 'allApps' list to ensure we don't lose selections that are hidden by a search filter.
        val selectedApps = allApps.filter { it.isSelected }.map { it.packageName }.toSet()
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("blocked_apps", selectedApps).apply()
    }

    // Handle the back arrow in the toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // NEW: Inflate the menu for the search bar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.queryHint = "Search for an app..."

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Not needed as we filter in real-time
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Filter the list as the user types
                filterApps(newText.orEmpty())
                return true
            }
        })
        return true
    }

    // NEW: Function to filter the app list based on a search query
    private fun filterApps(query: String) {
        val filteredList = if (query.isEmpty()) {
            allApps
        } else {
            // Filter the master list 'allApps'
            allApps.filter { it.name.contains(query, ignoreCase = true) }
        }

        // Update the displayed list and notify the adapter
        currentlyDisplayedApps.clear()
        currentlyDisplayedApps.addAll(filteredList)
        appAdapter.notifyDataSetChanged()
    }

    // --- RecyclerView Adapter ---
    // CHANGED: The adapter now takes a MutableList to allow for filtering
    class AppAdapter(private var apps: MutableList<AppInfo>) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_to_block, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            holder.appName.text = app.name
            holder.appIcon.setImageDrawable(app.icon)
            holder.appCheckbox.isChecked = app.isSelected

            // When a row is clicked, toggle the selection
            holder.itemView.setOnClickListener {
                app.isSelected = !app.isSelected
                holder.appCheckbox.isChecked = app.isSelected
            }
        }

        override fun getItemCount() = apps.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
            val appName: TextView = itemView.findViewById(R.id.app_name)
            val appCheckbox: CheckBox = itemView.findViewById(R.id.app_checkbox)
        }
    }
}
