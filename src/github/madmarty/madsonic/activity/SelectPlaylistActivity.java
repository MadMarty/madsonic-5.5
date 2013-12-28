/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */

package github.madmarty.madsonic.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import github.madmarty.madsonic.R;
import github.madmarty.madsonic.domain.Playlist;
import github.madmarty.madsonic.service.MusicServiceFactory;
import github.madmarty.madsonic.service.MusicService;
import github.madmarty.madsonic.util.BackgroundTask;
import github.madmarty.madsonic.util.Constants;
import github.madmarty.madsonic.util.PlaylistAdapter;
import github.madmarty.madsonic.util.PopupMenuHelper;
import github.madmarty.madsonic.util.TabActivityBackgroundTask;
import github.madmarty.madsonic.util.Util;

import java.util.List;

public class SelectPlaylistActivity extends SubsonicTabActivity implements AdapterView.OnItemClickListener {

    private static final int MENU_ITEM_PLAY_ALL = 1;
	private static final int MENU_ITEM_PLAY_SHUFFLED = 2;

    private ListView list;
    private View emptyTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_playlist);

		String theme = Util.getTheme(getBaseContext());
        if ("Madsonic Flawless".equals(theme) || "Madsonic Flawless Fullscreen".equals(theme)) {
            mainBar = findViewById(R.id.button_bar);
            mainBar.setBackgroundResource(R.drawable.menubar_button_normal_green);
	    } 
        if ("Madsonic Pink".equals(theme) || "Madsonic Pink Fullscreen".equals(theme)) {
            mainBar = findViewById(R.id.button_bar);
            mainBar.setBackgroundResource(R.drawable.menubar_button_normal_pink);
	    } 
        if ("Madsonic Light".equals(theme) || "Madsonic Light Fullscreen".equals(theme)) {
            mainBar = findViewById(R.id.button_bar);
            mainBar.setBackgroundResource(R.drawable.menubar_button_light);
	    } 
        
        list = (ListView) findViewById(R.id.select_playlist_list);
        emptyTextView = findViewById(R.id.select_playlist_empty);
        list.setOnItemClickListener(this);
        registerForContextMenu(list);

        // Title: Playlists
        setTitle(R.string.playlist_label);

        // Button 1: gone
        ImageButton searchButton = (ImageButton)findViewById(R.id.action_button_1);
        searchButton.setVisibility(View.GONE);

		// Button 2: refresh
        ImageButton refreshButton = (ImageButton) findViewById(R.id.action_button_2);
		refreshButton.setImageResource(R.drawable.action_refresh);
		refreshButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				refresh();
			}
		});
		
		// Button 3: Settings
        final ImageButton actionSettingsButton = (ImageButton)findViewById(R.id.action_button_3);
        actionSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               	new PopupMenuHelper().showMenu(SelectPlaylistActivity.this, actionSettingsButton, R.menu.common);
            }
        });

        load();
    }

	private void refresh() {
		finish();
		Intent intent = new Intent(this, SelectPlaylistActivity.class);
		intent.putExtra(Constants.INTENT_EXTRA_NAME_REFRESH, true);
		Util.startActivityWithoutTransition(this, intent);
	}

    private void load() {
        BackgroundTask<List<Playlist>> task = new TabActivityBackgroundTask<List<Playlist>>(this, true) {
            @Override
            protected List<Playlist> doInBackground() throws Throwable {
                MusicService musicService = MusicServiceFactory.getMusicService(SelectPlaylistActivity.this);
                boolean refresh = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_REFRESH, false);
                return musicService.getPlaylists(refresh, SelectPlaylistActivity.this, this);
            }

            @Override
            protected void done(List<Playlist> result) {
                list.setAdapter(new PlaylistAdapter(SelectPlaylistActivity.this, PlaylistAdapter.PlaylistComparator.sort(result)));
                emptyTextView.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
            }
        };
        task.execute();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        menu.add(Menu.NONE, MENU_ITEM_PLAY_ALL, MENU_ITEM_PLAY_ALL, R.string.common_play_now);
		menu.add(Menu.NONE, MENU_ITEM_PLAY_SHUFFLED, MENU_ITEM_PLAY_SHUFFLED, R.string.common_play_shuffled);
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
        Playlist playlist = (Playlist) list.getItemAtPosition(info.position);

		Intent intent;
        switch (menuItem.getItemId()) {
            case MENU_ITEM_PLAY_ALL:
                intent = new Intent(SelectPlaylistActivity.this, SelectAlbumActivity.class);
                intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, playlist.getId());
                intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, playlist.getName());
                intent.putExtra(Constants.INTENT_EXTRA_NAME_AUTOPLAY, true);
                Util.startActivityWithoutTransition(SelectPlaylistActivity.this, intent);
                break;
			case MENU_ITEM_PLAY_SHUFFLED:
				intent = new Intent(SelectPlaylistActivity.this, SelectAlbumActivity.class);
                intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, playlist.getId());
                intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, playlist.getName());
                intent.putExtra(Constants.INTENT_EXTRA_NAME_AUTOPLAY, true);
				intent.putExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, true);
                Util.startActivityWithoutTransition(SelectPlaylistActivity.this, intent);
                break;
            default:
                return super.onContextItemSelected(menuItem);
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Playlist playlist = (Playlist) parent.getItemAtPosition(position);

        Intent intent = new Intent(SelectPlaylistActivity.this, SelectAlbumActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, playlist.getId());
        intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME, playlist.getName());
        Util.startActivityWithoutTransition(SelectPlaylistActivity.this, intent);
    }

}