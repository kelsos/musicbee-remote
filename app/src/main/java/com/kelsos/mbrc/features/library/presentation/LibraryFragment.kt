package com.kelsos.mbrc.features.library.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kelsos.mbrc.R
import com.kelsos.mbrc.databinding.FragmentLibraryBinding
import com.kelsos.mbrc.features.library.presentation.screens.AlbumScreen
import com.kelsos.mbrc.features.library.presentation.screens.ArtistScreen
import com.kelsos.mbrc.features.library.presentation.screens.GenreScreen
import com.kelsos.mbrc.features.library.presentation.screens.TrackScreen
import com.kelsos.mbrc.features.library.sync.SyncCategory
import org.koin.android.ext.android.inject

class LibraryFragment : Fragment(), OnQueryTextListener, CategoryRetriever {

  private lateinit var pager: ViewPager2
  private lateinit var tabs: TabLayout
  private lateinit var dataBinding: FragmentLibraryBinding
  private lateinit var pagerAdapter: LibraryPagerAdapter
  private lateinit var searchView: SearchView
  private lateinit var searchMenuItem: MenuItem
  private lateinit var clearMenuItem: MenuItem

  private val viewModel: LibraryViewModel by inject()
  private val genreScreen: GenreScreen by inject()
  private val artistScreen: ArtistScreen by inject()
  private val albumScreen: AlbumScreen by inject()
  private val trackScreen: TrackScreen by inject()

  override fun onQueryTextSubmit(query: String): Boolean {
    val search = query.trim()
    if (search.isNotEmpty()) {
      closeSearch()
      viewModel.search(search)
      requireActivity().actionBar?.apply {
        title = search
      }
      searchMenuItem.isVisible = false
      clearMenuItem.isVisible = true
      return true
    }

    return false
  }

  private fun closeSearch(): Boolean {
    searchView.apply {
      if (isShown) {
        isIconified = true
        isFocusable = false
        clearFocus()
        searchMenuItem.collapseActionView()
        return@closeSearch true
      }
    }
    return false
  }

  override fun getCategory(category: Int): String = when (category) {
    SyncCategory.GENRES -> getString(R.string.library__category_genres)
    SyncCategory.ARTISTS -> getString(R.string.library__category_artists)
    SyncCategory.ALBUMS -> getString(R.string.library__category_albums)
    SyncCategory.TRACKS -> getString(R.string.library__category_tracks)
    SyncCategory.PLAYLISTS -> getString(R.string.library__category_playlists)
    else -> ""
  }

  override fun onQueryTextChange(newText: String): Boolean {
    return false
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    dataBinding.sync = viewModel.syncProgress
    dataBinding.category = this
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    setHasOptionsMenu(true)
    dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_library, container, false)
    dataBinding.lifecycleOwner = viewLifecycleOwner
    pager = dataBinding.libraryContainerPager
    tabs = dataBinding.libraryContainerTabs
    return dataBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val pagerAdapter = LibraryPagerAdapter(
      viewLifecycleOwner
    ).also {
      this.pagerAdapter = it
      pager.adapter = it
      pager.isUserInputEnabled = true
      it.submit(
        listOf(
          genreScreen,
          artistScreen,
          albumScreen,
          trackScreen
        )
      )
    }

    pager.adapter = pagerAdapter

    TabLayoutMediator(tabs, pager) { tab, position ->
      val resId = when (position) {
        0 -> R.string.label_genres
        1 -> R.string.label_artists
        2 -> R.string.label_albums
        3 -> R.string.label_tracks
        else -> error("invalid position")
      }

      tab.setText(resId)
    }.attach()
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.library_search, menu)
    clearMenuItem = menu.findItem(R.id.library__action_clear)
    searchMenuItem = menu.findItem(R.id.library__action_search).apply {
      searchView = actionView as SearchView
    }

    searchView.apply {
      queryHint = getString(R.string.library_search_hint)
      setIconifiedByDefault(true)
      setOnQueryTextListener(this@LibraryFragment)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.library__action_refresh -> {
        viewModel.refresh()
        return true
      }
      R.id.library__action_clear -> {
        viewModel.search()
        searchMenuItem.isVisible = true
        clearMenuItem.isVisible = false
        requireActivity().actionBar?.apply {
          setTitle(R.string.nav_library)
        }
        return true
      }
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onDestroy() {
    dataBinding.unbind()
    super.onDestroy()
  }

  companion object {
    private const val PAGER_POSITION = "com.kelsos.mbrc.ui.activities.nav.PAGER_POSITION"
  }
}