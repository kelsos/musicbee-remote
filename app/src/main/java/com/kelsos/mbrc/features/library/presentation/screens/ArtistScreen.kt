package com.kelsos.mbrc.features.library.presentation.screens

import androidx.lifecycle.LifecycleOwner
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.Meta.ARTIST
import com.kelsos.mbrc.common.utilities.nonNullObserver
import com.kelsos.mbrc.features.library.MenuItemSelectedListener
import com.kelsos.mbrc.features.library.data.Artist
import com.kelsos.mbrc.features.library.presentation.LibraryViewHolder
import com.kelsos.mbrc.features.library.presentation.adapters.ArtistAdapter
import com.kelsos.mbrc.features.library.presentation.viewmodels.ArtistViewModel
import com.kelsos.mbrc.features.queue.Queue
import com.kelsos.mbrc.features.queue.Queue.DEFAULT
import com.kelsos.mbrc.features.work.WorkHandler

class ArtistScreen(
  private val adapter: ArtistAdapter,
  private val workHandler: WorkHandler,
  private val viewModel: ArtistViewModel
) : LibraryScreen,
  MenuItemSelectedListener<Artist> {


  private lateinit var viewHolder: LibraryViewHolder

  override fun bind(viewHolder: LibraryViewHolder) {
    this.viewHolder = viewHolder
    viewHolder.setup(R.string.artists_list_empty, adapter)
    adapter.setMenuItemSelectedListener(this)
  }

  override fun observe(viewLifecycleOwner: LifecycleOwner) {
    viewModel.artists.nonNullObserver(viewLifecycleOwner) {
      adapter.submitList(it)
      viewHolder.refreshingComplete(it.isEmpty())
    }
  }

  override fun onMenuItemSelected(@Queue.Action action: String, item: Artist) {
    if (action == DEFAULT) {
      onItemClicked(item)
      return
    }
    workHandler.queue(item.id, ARTIST, action)
  }

  override fun onItemClicked(item: Artist) {
  }
}