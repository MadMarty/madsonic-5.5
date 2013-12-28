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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import github.madmarty.madsonic.R;
import github.madmarty.madsonic.domain.MusicDirectory;
import github.madmarty.madsonic.domain.MusicDirectory.Entry;
import github.madmarty.madsonic.domain.PodcastEpisode;
import github.madmarty.madsonic.service.DownloadFile;
import github.madmarty.madsonic.service.MusicService;
import github.madmarty.madsonic.service.MusicServiceFactory;
import github.madmarty.madsonic.util.Constants;
import github.madmarty.madsonic.util.EntryAdapter;
import github.madmarty.madsonic.util.PopupMenuHelper;
import github.madmarty.madsonic.util.ShareUtil;
import github.madmarty.madsonic.util.StarUtil;
import github.madmarty.madsonic.util.TabActivityBackgroundTask;
import github.madmarty.madsonic.util.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectAlbumActivity extends SubsonicTabActivity {

    private ListView entryList;
    private View footer;
    private View emptyView;
    private Button selectButton;
    private Button playNowButton;
	private Button playShuffledButton;
    private Button playLastButton;
    private Button pinButton;
    private Button unpinButton;
    private Button deleteButton;
    private Button moreButton;
    private ImageView coverArtView;
    private ImageButton playAllButton;
    private ImageButton starButton;
    private boolean isPlaylist;
    private boolean isVideolist;
    private boolean isBlockedlist;    
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_album);

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
        
        Util.changeLanguage(getBaseContext());        
        
        final String id = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_ID);
        final String name = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_NAME);
        final String parentId = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_PARENT_ID);
        final String parentName = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_PARENT_NAME);
        String playlistId = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID);
        String playlistName = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME);
        String albumListType = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE);
        String genreName = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_GENRE_NAME);
        String artistGenreName = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_ARTIST_GENRE_NAME);
        
        String shareName = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_SHARE);
        int getSharedTracks = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_SHARED, 0);
        
        int getStarredTracks = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_STARRED, 0);
        int getRandomTracks = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_RANDOM, 0);
        int getLastplayedTracks = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_LASTPLAYED, 0);
        int getNewaddedTracks = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_NEWADDED, 0);
        int albumListSize = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 0);
        int albumListOffset = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0);
        
        String podcastId = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_PODCAST_ID);
        String podcastName = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_PODCAST_NAME);
        String podcastDescription = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_PODCAST_DESCRIPTION);
		
		int getVideos = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_VIDEOS, 0);
        
        entryList = (ListView) findViewById(R.id.select_album_entries);

        footer = LayoutInflater.from(this).inflate(R.layout.select_album_footer, entryList, false);
        entryList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        entryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0) {
                    MusicDirectory.Entry entry = (MusicDirectory.Entry) parent.getItemAtPosition(position);
                    if (entry == null) {
                        return;
                    }
                    if (entry.isDirectory()) {
                        Intent intent = new Intent(SelectAlbumActivity.this, SelectAlbumActivity.class);
                        intent.putExtra(Constants.INTENT_EXTRA_NAME_ID, entry.getId());
                        intent.putExtra(Constants.INTENT_EXTRA_NAME_NAME, entry.getTitle());
                        intent.putExtra(Constants.INTENT_EXTRA_NAME_PARENT_ID, id);
                        intent.putExtra(Constants.INTENT_EXTRA_NAME_PARENT_NAME, name);
                        Util.startActivityWithoutTransition(SelectAlbumActivity.this, intent);
                       
                    } else if (entry.isVideo()) {
                    	
                      playVideo(entry);
//                    	if (Util.getDefaultVideoplayer(SelectAlbumActivity.this) == 1) {
//                    		streamInternalPlayer(entry);
//                    	} else {
//	                        streamExternalPlayer(entry);
//                    	}
                    }
                     else {
                        enableButtons();
                    }
                }
            }
        });

        coverArtView = (ImageView) findViewById(R.id.actionbar_home_icon);
        selectButton = (Button) findViewById(R.id.select_album_select);
        playNowButton = (Button) findViewById(R.id.select_album_play_now);
	//	playShuffledButton = (Button) findViewById(R.id.select_album_play_shuffled);
        playLastButton = (Button) findViewById(R.id.select_album_play_last);
        pinButton = (Button) findViewById(R.id.select_album_pin);
        unpinButton = (Button) findViewById(R.id.select_album_unpin);
        deleteButton = (Button) findViewById(R.id.select_album_delete);
        moreButton = (Button) footer.findViewById(R.id.select_album_more);        
        
    //   TextView songCountView = (TextView) findViewById(R.id.artist_song_count);
        
        emptyView = findViewById(R.id.select_album_empty);

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectAllOrNone();
            }
        });
        playNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                download(false, false, true, false, false);
                selectAll(false, false);
            }
        });
//		playShuffledButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                download(false, false, true, false, true);
//                selectAll(false, false);
//            }
//        });
        playLastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                download(true, false, false, false, false);
                selectAll(false, false);
            }
        });
        pinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                download(true, true, false, false, false);
                selectAll(false, false);
            }
        });
        unpinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unpin();
                selectAll(false, false);
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delete();
                selectAll(false, false);
            }
        });

        registerForContextMenu(entryList);

        enableButtons();

        isPlaylist = playlistId != null;
        isVideolist = getVideos != 0;
        
        isBlockedlist = getStarredTracks != 0 || getRandomTracks !=0 || getLastplayedTracks !=0 || getNewaddedTracks !=0 || podcastId != null || getVideos !=0;  
        
        if (isPlaylist) {
            getPlaylist(playlistId, playlistName);
            
        } else if (albumListType != null) {
            getAlbumList(albumListType, albumListSize, albumListOffset);
            
        } else if (artistGenreName != null) {
        	getArtistsForGenre(artistGenreName, albumListSize, albumListOffset);
            
        } else if (genreName != null) {
        	getSongsForGenre(genreName, albumListSize, albumListOffset);

        } else if (podcastId != null) {
			getPodcast(podcastId, podcastName, true);
        	
        } else if (getStarredTracks != 0) {
        	getStarred();
        	
        } else if (getLastplayedTracks != 0) {
        	getLastplayed(albumListSize);
        	
        } else if (getNewaddedTracks != 0)  {
        	getNewadded(albumListSize);
        	
        } else if (getRandomTracks != 0) {
        	getRandom(albumListSize);
        	
        } else if (getSharedTracks != 0) {
        	getSharedFiles(shareName);   
        	
        } else if (getVideos != 0) {
   			getVideos();
   			
        } else {
            getMusicDirectory(id, name, parentId, parentName);
        }

        // Button 1: play all
        playAllButton = (ImageButton) findViewById(R.id.action_button_0);
        playAllButton.setImageResource(R.drawable.action_play_all);
        playAllButton.setVisibility(View.GONE);
        playAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playAll(false);
            }
        });

        // Button 2: search
        ImageButton actionSearchButton = (ImageButton) findViewById(R.id.action_button_1);
		SharedPreferences prefs = Util.getPreferences(this);
		if(prefs.getBoolean(Constants.PREFERENCES_KEY_SEARCH_ENABLED, true)) {
			actionSearchButton.setVisibility(View.GONE);
		} else {
			actionSearchButton.setVisibility(View.VISIBLE);
		}
        actionSearchButton.setImageResource(R.drawable.action_search);
        actionSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSearchRequested();
            }
        });     
        
        // Button: refresh
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
            	new PopupMenuHelper().showMenu(SelectAlbumActivity.this, actionSettingsButton, R.menu.select_album);
            }
        });
    }

	private void getPodcast(final String podcastId, final String podcastName, final boolean refresh) {
		setTitle(podcastName);

		new LoadTask() {
			@Override
			protected MusicDirectory load(MusicService service) throws Exception {
				return service.getPodcastEpisodes(refresh, podcastId, getBaseContext(), this);
			}
		}.execute();
	}

	private void playAll(final boolean shuffle) {
        boolean hasSubFolders = false;
        for (int i = 0; i < entryList.getCount(); i++) {
            MusicDirectory.Entry entry = (MusicDirectory.Entry) entryList.getItemAtPosition(i);
            if (entry != null && entry.isDirectory()) {
                hasSubFolders = true;
                break;
            }
        }

        String id = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_ID);
        if (hasSubFolders && id != null) {
            downloadRecursively(id, false, false, true, shuffle);
        } else {
            selectAll(true, false);
            download(false, false, true, false, shuffle);
            selectAll(false, false);
        }
    }

    private void refresh() {
        finish();
        Intent intent = getIntent();
        intent.putExtra(Constants.INTENT_EXTRA_NAME_REFRESH, true);
        Util.startActivityWithoutTransition(this, intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

	        case R.id.menu_refresh:
	        	refresh();
	            return true;
        
           case R.id.menu_shuffle:
            	Intent intent1 = new Intent(this, DownloadActivity.class);
            	intent1.putExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, true);
            	Util.startActivityWithoutTransition(this, intent1);
                return true;
                
            case R.id.menu_exit:
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(Constants.INTENT_EXTRA_NAME_EXIT, true);
                Util.startActivityWithoutTransition(this, intent);
                return true;

            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.menu_help:
                startActivity(new Intent(this, HelpActivity.class));
                return true;
        }

        return false;
    }        
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) menuInfo;

        MusicDirectory.Entry entry = (MusicDirectory.Entry) entryList.getItemAtPosition(info.position);
        if (entry == null) {
            return;
        }
        
        boolean offline = Util.isOffline(this);
        
        if (entry.isDirectory()) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.select_album_context, menu);
            menu.findItem(R.id.album_menu_star).setVisible(!offline && !entry.isStarred());
            menu.findItem(R.id.album_menu_unstar).setVisible(!offline && entry.isStarred());
            menu.findItem(R.id.album_menu_pin).setVisible(!offline);
            menu.findItem(R.id.album_menu_share).setVisible(!offline);
        } else {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.select_song_context, menu);
            DownloadFile downloadFile = getDownloadService().forSong(entry);
            
            menu.findItem(R.id.song_menu_pin).setVisible(!offline && !downloadFile.isSaved());
            menu.findItem(R.id.song_menu_unpin).setVisible(!offline && downloadFile.isSaved());
            menu.findItem(R.id.song_menu_star).setVisible(!offline && !entry.isStarred());
            menu.findItem(R.id.song_menu_unstar).setVisible(!offline && entry.isStarred());
            menu.findItem(R.id.song_menu_share).setVisible(!offline);
            
            if (entry instanceof PodcastEpisode) {
                menu.findItem(R.id.song_menu_pin).setVisible(false);
            	menu.findItem(R.id.song_menu_star).setVisible(false);
                menu.findItem(R.id.song_menu_unstar).setVisible(false);
                menu.findItem(R.id.song_menu_share).setVisible(false);
            }            
            
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
        MusicDirectory.Entry entry = (MusicDirectory.Entry) entryList.getItemAtPosition(info.position);
        if (entry == null) {
            return true;
        }
        List<MusicDirectory.Entry> songs = new ArrayList<MusicDirectory.Entry>(10);
        songs.add((MusicDirectory.Entry) entryList.getItemAtPosition(info.position));

		switch (menuItem.getItemId()) {
            case R.id.album_menu_play_now:
                downloadRecursively(entry.getId(), false, false, true, false);
                break;
			case R.id.album_menu_play_shuffled:
                downloadRecursively(entry.getId(), false, false, true, true);
                break;
            case R.id.album_menu_play_last:
                downloadRecursively(entry.getId(), false, true, false, false);
                break;
            case R.id.song_menu_star:     
                StarUtil.starInBackground(this, entry, true);
                entry.setStarred(true);
                refresh();
                entryList.invalidate();
                return true;
                
            case R.id.album_menu_star:  
                StarUtil.starInBackground(this, entry, true);
                entry.setStarred(true);
                refresh();
                entryList.invalidate();
                return true;

            case R.id.song_menu_unstar:
                StarUtil.starInBackground(this, entry, false);
                entry.setStarred(false);
                refresh();
                entryList.invalidate();                
                return true;
                
            case R.id.album_menu_unstar:
                StarUtil.starInBackground(this, entry, false);
                entry.setStarred(false);
                refresh();
                entryList.invalidate();                
                return true;                
                
            case R.id.album_menu_pin:
                downloadRecursively(entry.getId(), true, true, false, false);
                break;
            case R.id.song_menu_play_now:
                getDownloadService().download(songs, false, true, true, false);
                break;
            case R.id.song_menu_play_next:
                getDownloadService().download(songs, false, false, true, false);
                break;
            case R.id.song_menu_play_last:
                getDownloadService().download(songs, false, false, false, false);
                break;
            case R.id.album_menu_share:
                ShareUtil.shareInBackground(this, entry);
                return true;
            case R.id.song_menu_share:
                ShareUtil.shareInBackground(this, entry);
                return true;
            default:
                return super.onContextItemSelected(menuItem);
        }
        return true;
    }

    private void getMusicDirectory(final String id, String name, final String parentId, final String parentName) {

    	//TODO:RECHECK Title new function??
    	  setTitle(name);

        new LoadTask() {
            @Override
            protected MusicDirectory load(MusicService service) throws Exception {
                boolean refresh = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_REFRESH, false);
                return service.getMusicDirectory(id, refresh, SelectAlbumActivity.this, this);
            }

            @Override
            protected void done(final MusicDirectory result) {
                super.done(result);
                
                setTitle(result.getName());
                setBackAction(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent;
                        if (result.getParentId() != null) {
                            intent = new Intent(SelectAlbumActivity.this, SelectAlbumActivity.class);
                            intent.putExtra(Constants.INTENT_EXTRA_NAME_ID, result.getParentId());
                        } else if (parentId != null) {
                            intent = new Intent(SelectAlbumActivity.this, SelectAlbumActivity.class);
                            intent.putExtra(Constants.INTENT_EXTRA_NAME_ID, parentId);
                            intent.putExtra(Constants.INTENT_EXTRA_NAME_NAME, parentName);
                        } else {
                            intent = new Intent(SelectAlbumActivity.this, SelectArtistActivity.class);
                        }
                        Util.startActivityWithoutTransition(SelectAlbumActivity.this, intent);
                    }
                });
            }
        }.execute();
    }

    private void getSongsForGenre(final String genre, final int count, final int offset) {
    	setTitle(genre);

        new LoadTask() {
            @Override
            protected MusicDirectory load(MusicService service) throws Exception {
                return service.getSongsByGenre(genre, count, offset, SelectAlbumActivity.this, this);
            }
            
            @Override
            protected void done(MusicDirectory result) {
                    // Hide more button when results are less than album list size
                    if (result.getChildren().size() < getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 0)) {
                    	moreButton.setVisibility(View.GONE);
                    } else {
                    	moreButton.setVisibility(View.VISIBLE);
                    }

                    moreButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(SelectAlbumActivity.this, SelectAlbumActivity.class);
                            String genre = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_GENRE_NAME);
                            int size = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 0);
                            int offset = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0) + size;

                            intent.putExtra(Constants.INTENT_EXTRA_NAME_GENRE_NAME, genre);
                            intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, size);
                            intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, offset);
                            Util.startActivityWithoutTransition(SelectAlbumActivity.this, intent);
                        }
                    });
                	
                super.done(result);
            }
        }.execute();
    }    

    private void getArtistsForGenre(final String genre, final int count, final int offset) {
    //	setTitle(genre);

        new LoadTask() {
            @Override
            protected MusicDirectory load(MusicService service) throws Exception {
                return service.getArtistsByGenre(genre, count, offset, SelectAlbumActivity.this, this);
            }
            
            @Override
            protected void done(MusicDirectory result) {
                    // Hide more button when results are less than album list size
                    if (result.getChildren().size() < getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 0)) {
                    	moreButton.setVisibility(View.GONE);
                    } else {
                    	moreButton.setVisibility(View.VISIBLE);
                    }

                    moreButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(SelectAlbumActivity.this, SelectAlbumActivity.class);
                            String genre = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_ARTIST_GENRE_NAME);
                            int size = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 0);
                            int offset = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0) + size;

                            intent.putExtra(Constants.INTENT_EXTRA_NAME_ARTIST_GENRE_NAME, genre);
                            intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, size);
                            intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, offset);
                            Util.startActivityWithoutTransition(SelectAlbumActivity.this, intent);
                        }
                    });
                	
                super.done(result);
            }
        }.execute();
    }     

    private void getStarred() {
        setTitle(R.string.main_songs_starred);
        new LoadTask() {
            @Override
            protected MusicDirectory load(MusicService service) throws Exception {
                return Util.getSongsFromSearchResult(service.getStarred(SelectAlbumActivity.this, this));
            }
        }.execute();
    }    

    private void getRandom(final int size) {
    	setTitle(R.string.main_songs_random);

        new LoadTask() {
            @Override
            protected MusicDirectory load(MusicService service) throws Exception {
                return service.getRandomSongs(size, SelectAlbumActivity.this, this);
            }
        }.execute();
    }

    private void getLastplayed(final int size) {
    	setTitle(R.string.main_songs_lastplayed);

        new LoadTask() {
            @Override
            protected MusicDirectory load(MusicService service) throws Exception {
                return service.getLastplayedSongs(size, SelectAlbumActivity.this, this);
            }
        }.execute();
    }    
    
    private void getNewadded(final int size) {
    	setTitle(R.string.main_songs_newadded);

        new LoadTask() {
            @Override
            protected MusicDirectory load(MusicService service) throws Exception {
                return service.getNewaddedSongs(size, SelectAlbumActivity.this, this);
            }
        }.execute();
    }    
    
    private void getSharedFiles(final String sharename) {
    	setTitle(R.string.main_shared);

        new LoadTask() {
            @Override
            protected MusicDirectory load(MusicService service) throws Exception {
            	MusicDirectory shareFolder = new MusicDirectory(); 
            	try { shareFolder = service.getSharedSongs(sharename, SelectAlbumActivity.this, this);} 
            	catch(Exception e) {}
           		return shareFolder;            		
            	}
        }.execute();
    }    
    
	private void getVideos()
	{
		setTitle(R.string.main_videos_title);

		new LoadTask()
		{
			@Override
			protected MusicDirectory load(MusicService service) throws Exception
			{
				boolean refresh = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_REFRESH, false);
				return service.getVideos(refresh, SelectAlbumActivity.this, this);
			}
		}.execute();
	}
    
    private void getPlaylist(final String playlistId, String playlistName) {
//  private void getPlaylist(final String playlistId, final String playlistName) {
        setTitle(playlistName);
        setBackAction(new Runnable() {
            @Override
            public void run() {
                Util.startActivityWithoutTransition(SelectAlbumActivity.this, SelectPlaylistActivity.class);
            }
        });

        new LoadTask() {
            @Override
            protected MusicDirectory load(MusicService service) throws Exception {
                return service.getPlaylist(playlistId, SelectAlbumActivity.this, this);
//              return service.getPlaylist(playlistId, playlistName, SelectAlbumActivity.this, this);
            }
        }.execute();
    }

    private void getAlbumList(final String albumListType, final int size, final int offset) {

        if ("newest".equals(albumListType)) {
            setTitle(R.string.main_albums_newest);
        } else if ("random".equals(albumListType)) {
            setTitle(R.string.main_albums_random);
        } else if ("highest".equals(albumListType)) {
            setTitle(R.string.main_albums_highest);
        } else if ("recent".equals(albumListType)) {
            setTitle(R.string.main_albums_recent);
        } else if ("frequent".equals(albumListType)) {
            setTitle(R.string.main_albums_frequent);
        } else if ("starred".equals(albumListType)) {
            setTitle(R.string.main_albums_starred);
        } else if ("alphabeticalByName".equals(albumListType)) {
        	setTitle(R.string.main_albums_alphaByName);
        } else if ("alphabeticalByArtist".equals(albumListType)) {
        	setTitle(R.string.main_albums_alphaByArtist);
	    } else if ("allArtist".equals(albumListType)) {
	    	setTitle(R.string.main_artists);
		} else if ("starredArtist".equals(albumListType)) {
			setTitle(R.string.main_artists_starred);
		}

        setBackAction(new Runnable() {
            @Override
            public void run() {
                Util.startActivityWithoutTransition(SelectAlbumActivity.this, MainActivity.class);
            }
        });
        new LoadTask() {
            @Override
            protected MusicDirectory load(MusicService service) throws Exception {
                return service.getAlbumList(albumListType, size, offset, SelectAlbumActivity.this, this);
            }

            @Override
            protected void done(MusicDirectory result) {
                if (!result.getChildren().isEmpty()) {
                    pinButton.setVisibility(View.GONE);
                    unpinButton.setVisibility(View.GONE);
                    deleteButton.setVisibility(View.GONE);
                	
                    // Hide more button when results are less than album list size
                    if (result.getChildren().size() < getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 0)) {
                    	moreButton.setVisibility(View.GONE);
                    } else {
                    	entryList.addFooterView(footer);//
                    	moreButton.setVisibility(View.VISIBLE);
                    }

//                    int songCount = 0;	
//                    songCount = result.getChildren().size();
//                    TextView songCountView = (TextView) findViewById(R.id.artist_song_count);
//                    String s = getResources().getQuantityString(R.plurals.select_album_n_songs, songCount, songCount);
//                    songCountView.setText(s.toUpperCase());


                    moreButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(SelectAlbumActivity.this, SelectAlbumActivity.class);
                            String type = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE);
                            int size = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 0);
                            int offset = getIntent().getIntExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0) + size;

                            intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, type);
                            intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, size);
                            intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, offset);
                            Util.startActivityWithoutTransition(SelectAlbumActivity.this, intent);
                        }
                    });
                }
                super.done(result);
            }
        }.execute();
    }

    private void selectAllOrNone() {
        boolean someUnselected = false;
        int count = entryList.getCount();
        for (int i = 0; i < count; i++) {
            MusicDirectory.Entry entry = getEntryAtPosition(i);
            if (!entryList.isItemChecked(i) && entry != null) {
                someUnselected = true;
                break;
            }
        }
        selectAll(someUnselected, true);
    }

    private void selectAll(boolean selected, boolean toast) {
        int count = entryList.getCount();
        int selectedCount = 0;
        for (int i = 0; i < count; i++) {
            MusicDirectory.Entry entry = getEntryAtPosition(i);
            if (entry != null && !entry.isDirectory() && !entry.isVideo()) {
                entryList.setItemChecked(i, selected);
                selectedCount++;
            }
        }

        // Display toast: N tracks selected / N tracks unselected
        if (toast) {
            int toastResId = selected ? R.string.select_album_n_selected
                                      : R.string.select_album_n_unselected;
            Util.toast(this, getString(toastResId, selectedCount));
        }

        enableButtons();
    }

    private MusicDirectory.Entry getEntryAtPosition(int i) {
        Object item = entryList.getItemAtPosition(i);
        return item instanceof MusicDirectory.Entry ? (MusicDirectory.Entry) item : null;
    }

    private void enableButtons() {
        if (getDownloadService() == null) {
            return;
        }

        List<MusicDirectory.Entry> selection = getSelectedSongs();
        boolean enabled = !selection.isEmpty();
        boolean unpinEnabled = false;
        boolean deleteEnabled = false;

        for (MusicDirectory.Entry song : selection) {
            DownloadFile downloadFile = getDownloadService().forSong(song);
            if (downloadFile.isCompleteFileAvailable()) {
                deleteEnabled = true;
            }
            if (downloadFile.isSaved()) {
                unpinEnabled = true;
            }
        }

        playNowButton.setEnabled(enabled);
	//	playShuffledButton.setEnabled(enabled);
        playLastButton.setEnabled(enabled);
        pinButton.setEnabled(enabled && !Util.isOffline(this));
        unpinButton.setEnabled(unpinEnabled);
        deleteButton.setEnabled(deleteEnabled);
    }

    private List<MusicDirectory.Entry> getSelectedSongs() {
        List<MusicDirectory.Entry> songs = new ArrayList<MusicDirectory.Entry>(10);
        int count = entryList.getCount();
        for (int i = 0; i < count; i++) {
            if (entryList.isItemChecked(i)) {
                MusicDirectory.Entry entry = getEntryAtPosition(i);
                if (entry != null) {
                    songs.add(entry);
                }
            }
        }
        return songs;
    }

    private void download(final boolean append, final boolean save, final boolean autoplay, final boolean playNext, final boolean shuffle) {
        if (getDownloadService() == null) {
            return;
        }

        List<MusicDirectory.Entry> songs = getSelectedSongs();
                if (!append) {
                    getDownloadService().clear();
                }

                warnIfNetworkOrStorageUnavailable();
                getDownloadService().download(songs, save, autoplay, playNext, shuffle);
                String playlistName = getIntent().getStringExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_NAME);
                if (playlistName != null) {
                    getDownloadService().setSuggestedPlaylistName(playlistName,null);
                }
                if (autoplay) {
                    Util.startActivityWithoutTransition(SelectAlbumActivity.this, DownloadActivity.class);
                } else if (save) {
                    Util.toast(SelectAlbumActivity.this,
                               getResources().getQuantityString(R.plurals.select_album_n_songs_downloading, songs.size(), songs.size()));
                } else if (append) {
                    Util.toast(SelectAlbumActivity.this,
                               getResources().getQuantityString(R.plurals.select_album_n_songs_added, songs.size(), songs.size()));
                }
    }

    private void delete() {
        if (getDownloadService() != null) {
            getDownloadService().delete(getSelectedSongs());
        }
    }

    private void unpin() {
        if (getDownloadService() != null) {
            getDownloadService().unpin(getSelectedSongs());
        }
    }

    @Deprecated
	private boolean entryExists(MusicDirectory.Entry entry) {
		DownloadFile check = new DownloadFile(this, entry, false);
		return check.isCompleteFileAvailable();
	}
    
    @Deprecated
    private void streamInternalPlayer(MusicDirectory.Entry entry) {
		int maxBitrate = Util.getMaxVideoBitrate(this);
		
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(MusicServiceFactory.getMusicService(this).getVideoUrl(maxBitrate, this, entry.getId())));
		
        startActivity(intent);
    }

    @Deprecated
    private void streamExternalPlayer(MusicDirectory.Entry entry) {
	int maxBitrate = Util.getMaxVideoBitrate(this);
	Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setDataAndType(Uri.parse(MusicServiceFactory.getMusicService(this).getVideoStreamUrl(maxBitrate, this, entry.getId())), "video/*");
	
	List<ResolveInfo> intents = getPackageManager()
		.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
	if(intents != null && intents.size() > 0) {
		startActivity(intent);
	 } else {
		Util.toast(this, R.string.download_no_streaming_player);
	 }
    }
    
     
    private abstract class LoadTask extends TabActivityBackgroundTask<MusicDirectory> {

        public LoadTask() {
            super(SelectAlbumActivity.this, true);
        }

        protected abstract MusicDirectory load(MusicService service) throws Exception;

        @Override
        protected MusicDirectory doInBackground() throws Throwable {
            MusicService musicService = MusicServiceFactory.getMusicService(SelectAlbumActivity.this);
            return load(musicService);
        }

        @Override
        protected void done(MusicDirectory result) {
            List<MusicDirectory.Entry> entries = result.getChildren();

            int songCount = 0;
            for (MusicDirectory.Entry entry : entries) {
                if (!entry.isDirectory()) {
                    songCount++;
                }
//                entry.set         
                }

            
            if (songCount > 0) {
            	
                entryList.addHeaderView(createHeader(result));
			
//              getImageLoader().loadImage(coverArtView, entries.get(0), false, true);
         //       entryList.addFooterView(footer);
                selectButton.setVisibility(View.VISIBLE);
                playNowButton.setVisibility(View.VISIBLE);
//				playShuffledButton.setVisibility(View.VISIBLE);
                playLastButton.setVisibility(View.VISIBLE);
				pinButton.setVisibility(View.VISIBLE);
				unpinButton.setVisibility(View.VISIBLE);
				deleteButton.setVisibility(View.VISIBLE);
            }

            boolean isAlbumList = getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE);

            emptyView.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
            playAllButton.setVisibility(isAlbumList || entries.isEmpty() ? View.GONE : View.VISIBLE);
            
            entryList.setAdapter(new EntryAdapter(SelectAlbumActivity.this, getImageLoader(), entries, true));

            boolean playAll = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_AUTOPLAY, false);
            if (playAll && songCount > 0) {
                playAll(getIntent().getBooleanExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, false));
            }
        }
    }
    
//    @Deprecated
//    private View createHeader(final List<MusicDirectory.Entry> entries) {
//        View header = LayoutInflater.from(this).inflate(R.layout.select_album_header, entryList, false);
//
//        View coverArtView = header.findViewById(R.id.select_album_art);
//        getImageLoader().loadImage(coverArtView, entries.get(0), false, true);
//        
//        boolean offline = Util.isOffline(this);
//
//        final ImageView starView = (ImageView) header.findViewById(R.id.select_album_star);
//        starView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                StarUtil.starInBackground(SelectAlbumActivity.this, (Entry) entries, !((MusicDirectory) entries).isStarred());
//                starView.setImageResource(((MusicDirectory) entries).isStarred() ? R.drawable.starred : R.drawable.unstarred);
//            }
//        });
//        starView.setImageResource(((MusicDirectory) entries).isStarred() ? R.drawable.starred : R.drawable.unstarred);
//        starView.setVisibility(offline || isPlaylist ? View.GONE : View.VISIBLE);
//
//        final ImageView shareView = (ImageView) header.findViewById(R.id.select_album_share);
//        shareView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                ShareUtil.shareInBackground(SelectAlbumActivity.this, entries);
//            }
//        });
//        shareView.setVisibility(offline || isPlaylist ? View.GONE : View.VISIBLE);
//
//        TextView titleView = (TextView) header.findViewById(R.id.select_album_title);
//        titleView.setText(getTitle());
//
//        int songCount = 0;
//
//        Set<String> artists = new HashSet<String>();
//        for (MusicDirectory.Entry entry : entries) {
//            if (!entry.isDirectory()) {
//                songCount++;
//                if (entry.getArtist() != null) {
//                    artists.add(entry.getArtist());
//                }
//            }
//        }
//
//        TextView artistView = (TextView) header.findViewById(R.id.select_album_artist);
//        if (artists.size() == 1) {
//            artistView.setText(artists.iterator().next());
//            artistView.setVisibility(View.VISIBLE);
//        } else {
//            artistView.setVisibility(View.GONE);
//        }
//
//        TextView songCountView = (TextView) header.findViewById(R.id.select_album_song_count);
//        String s = getResources().getQuantityString(R.plurals.select_album_n_songs, songCount, songCount);
//        songCountView.setText(s.toUpperCase());
//
//        return header;
//    }
    
    private View createHeader(final MusicDirectory directory) {
    	
        List<MusicDirectory.Entry> entries = directory.getChildren();
        View header = LayoutInflater.from(this).inflate(R.layout.select_album_header, entryList, false);

        View coverArtView = header.findViewById(R.id.select_album_art);
        getImageLoader().loadImage(coverArtView, entries.get(0), false, true);
          
        boolean offline = Util.isOffline(this);

        final ImageView starView = (ImageView) header.findViewById(R.id.select_album_star);
        starView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StarUtil.starInBackground(SelectAlbumActivity.this, directory, !directory.isStarred());
                starView.setImageResource(directory.isStarred() ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
            }
        });
        
        
        starView.setImageResource(directory.isStarred() ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        starView.setVisibility(offline || isPlaylist || isBlockedlist ? View.GONE : View.VISIBLE);
        
        final ImageView shareView = (ImageView) header.findViewById(R.id.select_album_share);
        shareView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShareUtil.shareInBackground(SelectAlbumActivity.this, directory);
            }
        });
        shareView.setVisibility(offline || isPlaylist || isBlockedlist ? View.GONE : View.VISIBLE);

        
        final ImageView shareMadsonicView = (ImageView) header.findViewById(R.id.select_album_share_madsonic);
        shareMadsonicView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShareUtil.shareWithMadsonicInBackground(SelectAlbumActivity.this, directory);
            }
        });
//        shareMadsonicView.setVisibility(offline || isPlaylist || isBlockedlist ? View.GONE : View.VISIBLE);
        shareMadsonicView.setVisibility(View.GONE);
        
        TextView titleView = (TextView) header.findViewById(R.id.select_album_title);
        titleView.setText(getTitle());

        int songCount = 0;

        Set<String> artists = new HashSet<String>();
        for (MusicDirectory.Entry entry : entries) {
            if (!entry.isDirectory()) {
                songCount++;
                if (entry.getArtist() != null) {
                    artists.add(entry.getArtist());
                }
            }
        }

        TextView artistView = (TextView) header.findViewById(R.id.select_album_artist);
        if (artists.size() == 1) {
            artistView.setText(artists.iterator().next());
            artistView.setVisibility(View.VISIBLE);
        } else {
            artistView.setVisibility(View.GONE);
        }

        TextView songCountView = (TextView) header.findViewById(R.id.select_album_song_count);
        
        String s;
        if (isVideolist) {
        	s = getResources().getQuantityString(R.plurals.select_album_n_videos, songCount, songCount);
        } else {
        	s = getResources().getQuantityString(R.plurals.select_album_n_songs, songCount, songCount);	
        }
        songCountView.setText(s.toUpperCase());

        return header;
    }
    
}
