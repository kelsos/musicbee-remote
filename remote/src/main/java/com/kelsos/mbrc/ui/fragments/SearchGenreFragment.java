package com.kelsos.mbrc.ui.fragments;


import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.google.inject.Inject;
import com.kelsos.mbrc.R;
import com.kelsos.mbrc.adapters.GenreEntryAdapter;
import com.kelsos.mbrc.constants.Protocol;
import com.kelsos.mbrc.constants.ProtocolEventType;
import com.kelsos.mbrc.data.GenreEntry;
import com.kelsos.mbrc.data.Queue;
import com.kelsos.mbrc.data.UserAction;
import com.kelsos.mbrc.events.MessageEvent;
import com.kelsos.mbrc.events.general.SearchDefaultAction;
import com.kelsos.mbrc.events.ui.GenreSearchResults;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import roboguice.fragment.RoboListFragment;

public class SearchGenreFragment extends RoboListFragment {
    private static final int QUEUE_NEXT = 1;
    private static final int QUEUE_LAST = 2;
    private static final int PLAY_NOW = 3;
    private static final int GET_SUB = 4;
    private static final int GROUP_ID = 11;
    private String mDefault;
    private GenreEntryAdapter adapter;
    @Inject Bus bus;

    @Subscribe public void handleSearchDefaultAction(SearchDefaultAction action) {
        mDefault = action.getAction();
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerForContextMenu(getListView());
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ui_fragment_library_simpl, container, false);
    }

    @Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle(R.string.search_context_header);
        menu.add(GROUP_ID, QUEUE_NEXT, 0, R.string.search_context_queue_next);
        menu.add(GROUP_ID, QUEUE_LAST, 0, R.string.search_context_queue_last);
        menu.add(GROUP_ID, PLAY_NOW, 0, R.string.search_context_play_now);
        menu.add(GROUP_ID, GET_SUB, 0, R.string.search_context_get_artists);
    }

    @Override public boolean onContextItemSelected(android.view.MenuItem item) {
        if (item.getGroupId() == GROUP_ID) {
            AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            Object line = adapter.getItem(mi.position);
            final String qContext = Protocol.LibraryQueueGenre;
            final String gSub = Protocol.LibraryGenreArtists;
            String query = ((GenreEntry) line).getName();

            UserAction ua = null;
            switch (item.getItemId()) {
                case QUEUE_NEXT:
                    ua = new UserAction(qContext, new Queue("next", query));
                    break;
                case QUEUE_LAST:
                    ua = new UserAction(qContext, new Queue("last", query));
                    break;
                case PLAY_NOW:
                    ua = new UserAction(qContext, new Queue("now", query));
                    break;
                case GET_SUB:
                    ua = new UserAction(gSub, query);
                    break;
            }

            if (ua != null) bus.post(new MessageEvent(ProtocolEventType.UserAction, ua));
            return true;
        } else {
            return false;
        }

    }

    @Subscribe public void handleGenreSearchResults(GenreSearchResults results) {
        adapter = new GenreEntryAdapter(getActivity(), R.layout.ui_list_single, results.getList());
        setListAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override public void onStart() {
        super.onStart();
        bus.register(this);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String genre = ((GenreEntry) getListView().getAdapter().getItem(position)).getName();

                bus.post(new MessageEvent(ProtocolEventType.UserAction,
                        new UserAction(Protocol.LibraryQueueGenre,
                                new Queue(mDefault, genre))));
            }
        });
    }

    @Override public void onStop() {
        super.onStop();
        bus.unregister(this);
    }
}