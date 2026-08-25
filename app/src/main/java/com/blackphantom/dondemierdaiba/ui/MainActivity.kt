package com.blackphantom.dondemierdaiba.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.tabs.TabLayout
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.data.ContentType
import com.blackphantom.dondemierdaiba.databinding.ActivityMainBinding
import com.blackphantom.dondemierdaiba.dialogs.AddContentDialog
import com.blackphantom.dondemierdaiba.util.Prefs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val types = arrayOf(ContentType.SERIE, ContentType.PELICULA, ContentType.VIDEO)
    private val fragments = arrayOfNulls<ContentListFragment>(3)
    private var currentTab = 0
    private var isGrid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isGrid = Prefs.isGridView(this)
        updateToggleIcon()

        setupFragments()
        setupTabs()
        setupSearch()
        setupToggleView()
        setupFab()
        updateFabLabel()
    }

    private fun setupFragments() {
        val tx: FragmentTransaction = supportFragmentManager.beginTransaction()
        types.forEachIndexed { index, type ->
            val fragment = ContentListFragment.newInstance(type)
            fragments[index] = fragment
            tx.add(R.id.fragmentContainer, fragment, "tab_$index")
            if (index != 0) tx.hide(fragment)
        }
        tx.commit()
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                switchTab(tab.position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun switchTab(position: Int) {
        if (position == currentTab) return
        val tx = supportFragmentManager.beginTransaction()
        fragments[currentTab]?.let { tx.hide(it) }
        fragments[position]?.let { tx.show(it) }
        tx.commit()
        currentTab = position
        updateFabLabel()
        binding.editSearch.text?.clear()
    }

    private fun setupSearch() {
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                fragments[currentTab]?.setQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupToggleView() {
        binding.btnToggleView.setOnClickListener {
            isGrid = !isGrid
            Prefs.setGridView(this, isGrid)
            updateToggleIcon()
            fragments.forEach { it?.setGridMode(isGrid) }
        }
    }

    private fun updateToggleIcon() {
        binding.btnToggleView.setImageResource(if (isGrid) R.drawable.ic_list else R.drawable.ic_grid)
        binding.btnToggleView.contentDescription =
            getString(if (isGrid) R.string.view_list else R.string.view_grid)
    }

    private fun setupFab() {
        binding.btnAdd.setOnClickListener {
            AddContentDialog.newInstance(types[currentTab]).show(supportFragmentManager, "add_content")
        }
    }

    private fun updateFabLabel() {
        binding.btnAdd.text = when (types[currentTab]) {
            ContentType.SERIE -> getString(R.string.add_serie)
            ContentType.PELICULA -> getString(R.string.add_pelicula)
            ContentType.VIDEO -> getString(R.string.add_video)
        }
    }
}
