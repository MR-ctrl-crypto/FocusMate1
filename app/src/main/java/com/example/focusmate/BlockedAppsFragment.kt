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
import androidx.lifecycle.lifecycleScope // <-- IMPORT THIS
import androidx.navigation.fragment.findNavController // <-- IMPORT THIS
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private var allApps = mutableListOf<AppInfo>()
    private var currentlyDisplayedApps = mutableListOf<AppInfo>()
    private lateinit var blockedAppsPackageNames: MutableSet<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_blocked_apps, container, false)
        setHasOptionsMenu(true)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.title = "Block Apps"
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            // FIXED: Use NavController for safe navigation
            findNavController().navigateUp()
        }

        // FIXED: Changed preference file name for consistency with the service
        val sharedPreferences = requireActivity().getSharedPreferences("BlockedAppsPrefs", Context.MODE_PRIVATE)
        blockedAppsPackageNames = sharedPreferences.getStringSet("blocked_apps_set", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        recyclerView = view.findViewById(R.id.apps_recycler_view)
        appAdapter = AppAdapter(currentlyDisplayedApps)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = appAdapter

        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        // FIXED: Use lifecycleScope, which is safer than GlobalScope
        lifecycleScope.launch(Dispatchers.IO) {
            val pm: PackageManager = requireActivity().packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val appList = mutableListOf<AppInfo>()

            for (packageInfo in packages) {
                if ((packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && packageInfo.packageName != requireActivity().packageName) {
                    val appName = packageInfo.loadLabel(pm).toString()
                    val packageName = packageInfo.packageName
                    val appIcon = packageInfo.loadIcon(pm)
                    val isSelected = blockedAppsPackageNames.contains(packageName)
                    appList.add(AppInfo(appName, packageName, appIcon, isSelected))
                }
            }
            appList.sortBy { it.name.lowercase() }

            withContext(Dispatchers.Main) {
                allApps.clear()
                allApps.addAll(appList)
                currentlyDisplayedApps.clear()
                currentlyDisplayedApps.addAll(allApps)
                appAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val selectedApps = allApps.filter { it.isSelected }.map { it.packageName }.toSet()
        // FIXED: Use the correct preference file name and key
        val sharedPreferences = requireActivity().getSharedPreferences("BlockedAppsPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("blocked_apps_set", selectedApps).apply()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_menu, menu)
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        searchView.queryHint = "Search for an app..."
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterApps(newText.orEmpty())
                return true
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun filterApps(query: String) {
        val filteredList = if (query.isEmpty()) allApps else allApps.filter { it.name.contains(query, ignoreCase = true) }
        currentlyDisplayedApps.clear()
        currentlyDisplayedApps.addAll(filteredList)
        appAdapter.notifyDataSetChanged()
    }

    class AppAdapter(private val apps: List<AppInfo>) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_to_block, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            holder.appName.text = app.name
            holder.appIcon.setImageDrawable(app.icon)
            holder.appCheckbox.isChecked = app.isSelected

            holder.itemView.setOnClickListener {
                app.isSelected = !app.isSelected
                holder.appCheckbox.isChecked = app.isSelected
            }
            holder.appCheckbox.setOnCheckedChangeListener { _, isChecked ->
                app.isSelected = isChecked
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
