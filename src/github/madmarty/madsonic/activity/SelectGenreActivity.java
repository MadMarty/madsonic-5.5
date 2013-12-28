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
import android.view.LayoutInflater;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import github.madmarty.madsonic.R;
import github.madmarty.madsonic.domain.Genre;
import github.madmarty.madsonic.domain.MusicFolder;
import github.madmarty.madsonic.service.MusicService;
import github.madmarty.madsonic.service.MusicServiceFactory;
import github.madmarty.madsonic.util.BackgroundTask;
import github.madmarty.madsonic.util.Constants;
import github.madmarty.madsonic.util.GenreAdapter;
import github.madmarty.madsonic.util.PopupMenuHelper;
import github.madmarty.madsonic.util.TabActivityBackgroundTask;
import github.madmarty.madsonic.util.Util;

import java.util.ArrayList;
import java.util.List;

public class SelectGenreActivity extends SubsonicTabActivity implements AdapterView.OnItemClickListener {

	private static final String TAG = SelectGenreActivity.class.getSimpleName();
	
	private ListView genreList;
    private View emptyView;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_genre);

        genreList = (ListView) findViewById(R.id.select_genre_list);
        genreList.setOnItemClickListener(this);
        //genreList.setOnTouchListener(gestureListener);
        
        emptyView = findViewById(R.id.select_genre_empty);

        registerForContextMenu(genreList);

        setTitle(R.string.main_genre_title);

		String theme = Util.getTheme(getBaseContext());
        if ("Madsonic Pink".equals(theme) || "Madsonic Pink Fullscreen".equals(theme)) {
            mainBar = findViewById(R.id.button_bar);
            mainBar.setBackgroundResource(R.drawable.menubar_button_normal_pink);
	    } 
        if ("Madsonic Light".equals(theme) || "Madsonic Light Fullscreen".equals(theme)) {
            mainBar = findViewById(R.id.button_bar);
            mainBar.setBackgroundResource(R.drawable.menubar_button_light);
	    } 
        
        // Button 1: gone
        ImageButton searchButton = (ImageButton) findViewById(R.id.action_button_1);
        searchButton.setVisibility(View.GONE);

		// Button 2: refresh
        ImageButton refreshButton = (ImageButton) findViewById(R.id.action_button_2);
        refreshButton.setVisibility(View.GONE);
		
		// Button 3: Settings
        final ImageButton actionSettingsButton = (ImageButton)findViewById(R.id.action_button_3);
        actionSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               	new PopupMenuHelper().showMenu(SelectGenreActivity.this, actionSettingsButton, R.menu.common);
            }
        });        
        
        
        load();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
 //   	inflater.inflate(R.menu.main, menu);
    	inflater.inflate(R.menu.common, menu);
    	super.onCreateOptionsMenu(menu);
    	
    	return true;
    }

    private void refresh() {
        finish();
        Intent intent = getIntent();
        intent.putExtra(Constants.INTENT_EXTRA_NAME_REFRESH, true);
        Util.startActivityWithoutTransition(this, intent);
    }

    private void load() {
        BackgroundTask<List<Genre>> task = new TabActivityBackgroundTask<List<Genre>>(this, true) {
            @Override
            protected List<Genre> doInBackground() throws Throwable {
                MusicService musicService = MusicServiceFactory.getMusicService(SelectGenreActivity.this);
                
                List<Genre> genres = new ArrayList<Genre>(); 
                
                try {
                	genres = musicService.getGenres(SelectGenreActivity.this, this);
                } catch (Exception x) {
                    Log.e(TAG, "Failed to load genres", x);
                }
                
				return genres;
            }

            @Override
            protected void done(List<Genre> result) {
        		emptyView.setVisibility(result == null || result.isEmpty() ? View.VISIBLE : View.GONE);
            	
            	if (result != null) {
            		genreList.setAdapter(new GenreAdapter(SelectGenreActivity.this, result));
            	}
            		
            }
        };
        task.execute();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    	Genre genre = (Genre) parent.getItemAtPosition(position);
    	Intent intent = new Intent(this, SelectAlbumActivity.class);
    	intent.putExtra(Constants.INTENT_EXTRA_NAME_GENRE_NAME, genre.getName());
        intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 20);
        intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0);
    	Util.startActivityWithoutTransition(this, intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
			case android.R.id.home:
	//			menuDrawer.toggleMenu();
				return true;
				
            case R.id.menu_refresh:
            	refresh();
                return true;
                
                
//            case R.id.main_shuffle:
//                Intent intent = new Intent(this, DownloadActivity.class);
//                intent.putExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, true);
//                Util.startActivityWithoutTransition(this, intent);
//                return true;
        }

        return false;
    }
}