package com.example.focusmate

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
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

class BlockedAppsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var appAdapter: AppAdapter
    private lateinit var toolbar: Toolbar

    // Two lists are used: one for all apps (the master list), and one for what's currently displayed (the filtered list).
    private var allApps = mutableListOf<AppInfo>()
    private var currentlyDisplayedApps = mutableListOf<AppInfo>()

    // This holds the package names of the apps the user has selected.
    private lateinit var blockedAppsPrefs: MutableSet<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment. This connects the Kotlin code to the XML file.
        val view = inflater.inflate(R.layout.fragment_blocked_apps, container, false)
        setHasOptionsMenu(true) // IMPORTANT: This tells the fragment that it has its own options menu (the search bar).
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Setup Toolbar ---
        toolbar = view.findViewById(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar) // Cast the fragment's activity to an AppCompatActivity.
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            // Handle the back arrow click
            parentFragmentManager.popBackStack()
        }

        // --- Load Preferences ---
        // Access SharedPreferences using the fragment's context.
        val sharedPreferences = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        blockedAppsPrefs = sharedPreferences.getStringSet("blocked_apps", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        // --- Setup RecyclerView ---
        recyclerView = view.findViewById(R.id.apps_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize the adapter with the list that will be filtered and displayed.
        appAdapter = AppAdapter(currentlyDisplayedApps)
        recyclerView.adapter = appAdapter

        // --- Start Loading Apps ---
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        // Loading apps can be slow, so it's done on a background thread using Coroutines.
        GlobalScope.launch(Dispatchers.IO) {
            val pm: PackageManager = requireActivity().packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val appList = mutableListOf<AppInfo>()

            for (packageInfo in packages) {
                // Filter out system apps and this app itself.
                if ((packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && packageInfo.packageName != requireActivity().packageName) {
                    val appName = packageInfo.loadLabel(pm).toString()
                    val packageName = packageInfo.packageName
                    val appIcon = packageInfo.loadIcon(pm)
                    val isSelected = blockedAppsPrefs.contains(packageName)
                    appList.add(AppInfo(appName, packageName, appIcon, isSelected))
                }
            }
            // Sort the list alphabetically.
            appList.sortBy { it.name }

            // Switch back to the main thread to update the UI.
            withContext(Dispatchers.Main) {
                allApps.clear()
                allApps.addAll(appList)
                currentlyDisplayedApps.clear()
                currentlyDisplayedApps.addAll(allApps)
                appAdapter.notifyDataSetChanged() // Refresh the list in the UI.
            }
        }
    }

    // --- Save Selections ---
    // Save the selections when the fragment is paused (e.g., when the user navigates away).
    override fun onPause() {
        super.onPause()
        // IMPORTANT: We filter the 'allApps' master list to ensure we don't lose selections that are hidden by a search filter.
        val selectedApps = allApps.filter { it.isSelected }.map { it.packageName }.toSet()
        val sharedPreferences = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("blocked_apps", selectedApps).apply()
    }

    // --- Search Menu ---
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.queryHint = "Search for an app..."

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false // Not needed as we filter in real-time.
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Filter the list as the user types.
                filterApps(newText.orEmpty())
                return true
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    // --- Filter Logic ---
    private fun filterApps(query: String) {
        val filteredList = if (query.isEmpty()) {
            allApps // If the search is empty, show all apps.
        } else {
            // Otherwise, filter the master list 'allApps'.
            allApps.filter { it.name.contains(query, ignoreCase = true) }
        }

        // Update the displayed list and notify the adapter.
        currentlyDisplayedApps.clear()
        currentlyDisplayedApps.addAll(filteredList)
        appAdapter.notifyDataSetChanged()
    }

    // --- RecyclerView Adapter ---
    // The adapter's code remains largely the same, but it's now an inner class of the fragment.
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

            // When a row is clicked, toggle the selection state of the app
            holder.itemView.setOnClickListener {
                app.isSelected = !app.isSelected
                holder.appCheckbox.isChecked = app.isSelected
            }

            // Also allow clicking the checkbox itself
            holder.appCheckbox.setOnClickListener {
                app.isSelected = holder.appCheckbox.isChecked
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
