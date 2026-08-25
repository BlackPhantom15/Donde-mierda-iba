package com.blackphantom.dondemierdaiba.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.adapter.ContentAdapter
import com.blackphantom.dondemierdaiba.data.AppDatabase
import com.blackphantom.dondemierdaiba.data.ContentType
import com.blackphantom.dondemierdaiba.databinding.FragmentContentListBinding
import com.blackphantom.dondemierdaiba.dialogs.EditProgressDialog
import com.blackphantom.dondemierdaiba.util.Prefs

class ContentListFragment : Fragment() {

    private var _binding: FragmentContentListBinding? = null
    private val binding get() = _binding!!

    private lateinit var type: ContentType
    private lateinit var adapter: ContentAdapter
    private val queryLive = MutableLiveData("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = ContentType.valueOf(requireArguments().getString(ARG_TYPE)!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isGrid = Prefs.isGridView(requireContext())
        adapter = ContentAdapter(isGrid) { item ->
            EditProgressDialog.newInstance(item.id).show(childFragmentManager, "edit_progress")
        }

        binding.recyclerView.adapter = adapter
        applyLayoutManager(isGrid)

        binding.txtEmpty.text = when (type) {
            ContentType.SERIE -> getString(R.string.empty_series)
            ContentType.PELICULA -> getString(R.string.empty_peliculas)
            ContentType.VIDEO -> getString(R.string.empty_videos)
        }

        val dao = AppDatabase.getInstance(requireContext()).contentDao()
        val itemsLive = Transformations.switchMap(queryLive) { q ->
            dao.observeByType(type.name, q ?: "")
        }
        itemsLive.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.txtEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    fun setQuery(query: String) {
        if (queryLive.value != query) queryLive.value = query
    }

    fun setGridMode(grid: Boolean) {
        adapter.setGridMode(grid)
        applyLayoutManager(grid)
    }

    private fun applyLayoutManager(grid: Boolean) {
        binding.recyclerView.layoutManager = if (grid) {
            GridLayoutManager(requireContext(), 2)
        } else {
            LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TYPE = "arg_type"
        fun newInstance(type: ContentType): ContentListFragment {
            val f = ContentListFragment()
            f.arguments = Bundle().apply { putString(ARG_TYPE, type.name) }
            return f
        }
    }
}
