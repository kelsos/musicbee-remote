package com.kelsos.mbrc.features.library.presentation.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.kelsos.mbrc.R
import com.kelsos.mbrc.features.queue.Queue
import com.kelsos.mbrc.ui.FastScrollableAdapter
import com.kelsos.mbrc.features.library.OnFastScrollListener
import com.kelsos.mbrc.features.library.popup
import com.kelsos.mbrc.features.library.data.Track
import com.kelsos.mbrc.features.library.presentation.viewholders.TrackViewHolder
import com.kelsos.mbrc.ui.widgets.RecyclerViewFastScroller.BubbleTextGetter

class TrackAdapter : FastScrollableAdapter<Track, TrackViewHolder>(
  DIFF_CALLBACK
),
  BubbleTextGetter, OnFastScrollListener {

  private val indicatorPressed: (View, Int) -> Unit = { view, position ->
    view.popup(R.menu.popup_track) {

      val action = when (it) {
        R.id.popup_track_queue_next -> Queue.NEXT
        R.id.popup_track_queue_last -> Queue.LAST
        R.id.popup_track_play -> Queue.NOW
        R.id.popup_track_play_queue_all -> Queue.ADD_ALL
        else -> throw IllegalArgumentException("invalid menuItem id $it")
      }

      val listener = requireListener()
      getItem(position)?.run {
        listener.onMenuItemSelected(action, this)
      }
    }
  }

  private val pressed: (View, Int) -> Unit = { _, position ->
    val listener = requireListener()
    getItem(position)?.run {
      listener.onItemClicked(this)
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
    return TrackViewHolder.create(
      parent,
      indicatorPressed,
      pressed
    )
  }

  override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
    if (fastScrolling) {
      holder.clear()
      return
    }

    val trackEntity = getItem(holder.adapterPosition)

    if (trackEntity != null) {
      holder.bindTo(trackEntity)
    } else {
      holder.clear()
    }
  }

  companion object {
    val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Track>() {
      override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
        return oldItem.id == newItem.id
      }

      override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
        return oldItem == newItem
      }
    }
  }
}