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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import github.madmarty.madsonic.R;
import github.madmarty.madsonic.domain.Indexes;
import github.madmarty.madsonic.domain.Version;
import github.madmarty.madsonic.service.DownloadService;
import github.madmarty.madsonic.service.DownloadServiceImpl;
import github.madmarty.madsonic.service.MusicService;
import github.madmarty.madsonic.service.MusicServiceFactory;
import github.madmarty.madsonic.service.ServerTooOldException;
import github.madmarty.madsonic.util.BackgroundTask;
import github.madmarty.madsonic.util.ChangeLog;
import github.madmarty.madsonic.util.Constants;
import github.madmarty.madsonic.util.Logger;
import github.madmarty.madsonic.util.MergeAdapter;
import github.madmarty.madsonic.util.ModalBackgroundTask;
import github.madmarty.madsonic.util.PopupMenuHelper;
import github.madmarty.madsonic.util.SilentBackgroundTask;
import github.madmarty.madsonic.util.TabActivityBackgroundTask;
import github.madmarty.madsonic.util.Util;
import github.madmarty.madsonic.util.FileUtil;
import github.madmarty.madsonic.view.PodcastChannelView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends SubsonicTabActivity {
	
    private static final Logger LOG = new Logger(MainActivity.class);
    
    private static final int MENU_GROUP_SERVER = 10;
    private static final int MENU_ITEM_OFFLINE = 111;
    private static final int MENU_ITEM_SERVER_1 = 101;
    private static final int MENU_ITEM_SERVER_2 = 102;
    private static final int MENU_ITEM_SERVER_3 = 103;
    private static final int MENU_ITEM_SERVER_4 = 104;
    private static final int MENU_ITEM_SERVER_5 = 105;
    private static final int MENU_ITEM_SERVER_6 = 106;
    private static final int MENU_ITEM_SERVER_7 = 107;
    private static final int MENU_ITEM_SERVER_8 = 108;
    private static final int MENU_ITEM_SERVER_9 = 109;
    private static final int MENU_ITEM_SERVER_10 = 110;

    private String theme;

    private static boolean infoDialogDisplayed;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_EXIT)) {
            exit();
            return;
        } 
    	
	    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	    StrictMode.setThreadPolicy(policy); 
        
        Util.changeLanguage(getBaseContext());
        
        setContentView(R.layout.main);
        
        loadSettings();

        View buttons = LayoutInflater.from(this).inflate(R.layout.main_buttons, null);

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

    
        final View serverButton = buttons.findViewById(R.id.main_select_server);
        final TextView serverTextView = (TextView) serverButton.findViewById(R.id.main_select_server_2);

        final View artistTitle = buttons.findViewById(R.id.main_artists);
        final View artistsButton = buttons.findViewById(R.id.main_artists_button);
        final View artistsGenreButton = buttons.findViewById(R.id.main_artists_genre);
        final View artistsStarredButton = buttons.findViewById(R.id.main_artists_starred );
        
        final View genresButton = buttons.findViewById(R.id.main_genres_button);
        final View songsTitle = buttons.findViewById(R.id.main_songs);
        final View randomSongsButton = buttons.findViewById(R.id.main_songs_button);
        final View songsStarredButton = buttons.findViewById(R.id.main_songs_starred );
        final View songsLastplayedButton = buttons.findViewById(R.id.main_songs_lastplayed_button );
        final View songsNewaddedButton = buttons.findViewById(R.id.main_songs_newadded_button );
        
        final View albumsTitle = buttons.findViewById(R.id.main_albums);
        final View albumsNewestButton = buttons.findViewById(R.id.main_albums_newest);
        final View albumsStarredButton = buttons.findViewById(R.id.main_albums_starred);
        final View albumsHighestButton = buttons.findViewById(R.id.main_albums_highest);
        final View albumsHotButton = buttons.findViewById(R.id.main_albums_hot);
        final View albumsFrequentButton = buttons.findViewById(R.id.main_albums_frequent);
        final View albumsRecentButton = buttons.findViewById(R.id.main_albums_recent);
        final View albumsRandomButton = buttons.findViewById(R.id.main_albums_random);
        final View albumsTipButton = buttons.findViewById(R.id.main_albums_tip); 
        final View albumsAlphaByNameButton = buttons.findViewById(R.id.main_albums_alphaByName);
        final View albumsAlphaByArtistButton = buttons.findViewById(R.id.main_albums_alphaByArtist);

        final View videosTitle = buttons.findViewById(R.id.main_videos);
        final View videosNewestButton = buttons.findViewById(R.id.main_videos_newest);
        
        final View dummyView = findViewById(R.id.main_dummy);

        boolean shouldShowDialog = false;
        
        if (!getActiveServerEnabled()) {
        	shouldShowDialog = true;
        	Util.setActiveServer(this, 0);
        }
        int instance = Util.getActiveServer(this);
        String name = Util.getServerName(this, instance);
        
        if (name == null) {
        	shouldShowDialog = true;
        	Util.setActiveServer(this, 0);
        	instance = Util.getActiveServer(this);
            name = Util.getServerName(this, instance);
        }
        serverTextView.setText(name);

        ListView list = (ListView) findViewById(R.id.main_list);

        MergeAdapter adapter = new MergeAdapter();
        adapter.addViews(Arrays.asList(serverButton), true);
        if (!Util.isOffline(this)) {
         	
//        	Version currentVersion = null ;
//	        Version requiredVersion = null ;
//	        
//	        PingPong();
//	        requiredVersion = new Version("1.10.5");
//	        Indexes index = getIndex(); 
//	        currentVersion = getAPIVersion();
//	        
//            if ( currentVersion == null ) {
//    	        PingPong();
//    	        index = getIndex(); 
//    	        currentVersion = getAPIVersion();
//            }
//	        
//	        boolean ok = false;
//	        
//            if ( currentVersion == null ) {
//            	ok = true;
//            } else {
//    			ok = requiredVersion.compareTo(currentVersion) <= 0;
//            }
//
//			Util.toast(getBaseContext(), "REST-API: " + currentVersion + " requiredVersion: "+ ok);
//
//			if (!ok) {
//			}
            
	            adapter.addView(artistTitle, false);
	        	adapter.addViews(Arrays.asList(artistsStarredButton, artistsGenreButton, artistsButton ), true);
	        	
	        	adapter.addView(songsTitle, false);
	        	adapter.addViews(Arrays.asList(songsStarredButton, genresButton, randomSongsButton, songsNewaddedButton, songsLastplayedButton), true);
	        	
	            adapter.addView(albumsTitle, false);
	            adapter.addViews(Arrays.asList(albumsStarredButton, albumsRandomButton, albumsNewestButton, albumsHighestButton, albumsHotButton, albumsRecentButton, albumsFrequentButton, albumsTipButton, albumsAlphaByNameButton, albumsAlphaByArtistButton), true);

	            adapter.addView(videosTitle, false);
	            adapter.addViews(Arrays.asList(videosNewestButton), true);
	            
	            
//		        } else {        	        	
//	        	adapter.addView(songsTitle, false);
//	        	adapter.addViews(Arrays.asList(songsStarredButton, genresButton, randomSongsButton ), true);
//	        	
//	            adapter.addView(albumsTitle, false);
//	            adapter.addViews(Arrays.asList(albumsStarredButton, albumsRandomButton, albumsNewestButton, albumsHighestButton, albumsRecentButton, albumsFrequentButton, albumsAlphaByNameButton, albumsAlphaByArtistButton), true);
//		        }
        }
        
        list.setAdapter(adapter);
        registerForContextMenu(dummyView);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (view == serverButton) {
                    dummyView.showContextMenu();
                } else if (view == albumsNewestButton) {
                    showAlbumList("newest");
                } else if (view == albumsRandomButton) {
                    showAlbumList("random");
                } else if (view == albumsHighestButton) {
                    showAlbumList("highest");
                } else if (view == albumsHotButton) {
                    showAlbumList("hot");
                } else if (view == albumsStarredButton) {
                    showAlbumList("starred");
                } else if (view == albumsRecentButton) {
                    showAlbumList("recent");
                } else if (view == albumsFrequentButton) {
                    showAlbumList("frequent");
                } else if (view == albumsTipButton) {
                    showAlbumList("tip");
                } else if (view == albumsAlphaByNameButton) {
                	showAlbumList("alphabeticalByName");
                } else if (view == albumsAlphaByArtistButton) {
                	showAlbumList("alphabeticalByArtist");
                } else if (view == artistsStarredButton) {
                	showArtistAlbumList("starredArtist");
                } else if (view == artistsButton) {
                	showArtistAlbumList("allArtist");
                } else if (view == songsStarredButton) {
                	showStarredSongs();
                } else if (view == randomSongsButton) {
                	showRandomSongs();
                } else if (view == songsNewaddedButton) {
                	showNewaddedSongs();
                } else if (view == songsLastplayedButton) {
                	showLastplayedSongs();
                } else if (view == genresButton) {
                	showGenres();
                } else if (view == artistsGenreButton) {
                	showArtistGenres();
                } else if (view == videosNewestButton) {
					showVideos();
                }
            }
        });

        // Title: Madsonic
        setTitle(R.string.common_appname);

        // Button 1: shuffle
        ImageButton actionShuffleButton = (ImageButton)findViewById(R.id.action_button_0);
        actionShuffleButton.setImageResource(R.drawable.action_box);
        actionShuffleButton.setVisibility(View.VISIBLE);        

        actionShuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, DownloadActivity.class);
                intent.putExtra(Constants.INTENT_EXTRA_NAME_SHUFFLE, true);
                Util.startActivityWithoutTransition(MainActivity.this, intent);
            }
        });
        
        // Button 2: search
        ImageButton searchButton = (ImageButton)findViewById(R.id.action_button_1);
        searchButton.setImageResource(R.drawable.action_search);
        
		SharedPreferences prefs = Util.getPreferences(this);
		if(prefs.getBoolean(Constants.PREFERENCES_KEY_SEARCH_ENABLED, true)) {
			searchButton.setVisibility(View.GONE);
		} else {
			searchButton.setVisibility(View.VISIBLE);
		}
        
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	
                onSearchRequested();
            }
        });     
        
        // Button 2: browse
        ImageButton playnowButton = (ImageButton)findViewById(R.id.action_button_2);
        playnowButton.setImageResource(R.drawable.action_browse);
        playnowButton.setVisibility(View.VISIBLE);
        
        playnowButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(MainActivity.this, DownloadActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Util.startActivityWithoutTransition(MainActivity.this, intent);
            }
    }); 
        
		// Button 3: Settings
        final ImageButton actionSettingsButton = (ImageButton)findViewById(R.id.action_button_3);
        actionSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               	new PopupMenuHelper().showMenu(MainActivity.this, actionSettingsButton, R.menu.main);
                }
        });

        
        // Remember the current theme.
        theme = Util.getTheme(this);

        showInfoDialog(shouldShowDialog);
        
		ChangeLog changeLog = new ChangeLog(this, Util.getPreferences(this));
		if(changeLog.isFirstRun()) {
			changeLog.getLogDialog().show();
		}   
		
    }

    	
    private void loadSettings() {
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        SharedPreferences prefs = Util.getPreferences(this);
        if (!prefs.contains(Constants.PREFERENCES_KEY_CACHE_LOCATION)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Constants.PREFERENCES_KEY_CACHE_LOCATION, FileUtil.getDefaultMusicDirectory().getPath());
            editor.commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restart activity if theme has changed.
        if (theme != null && !theme.equals(Util.getTheme(this))) {
            restart();
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        int activeServer = Util.getActiveServer(this);
        boolean checked = false;
                
        for (int i = 0; i <= Util.getActiveServers(this); i++) {
        	String serverName = Util.getServerName(this, i);
        	
        	if (serverName == null) {
        		continue;
        	}
        	
        	if (Util.getServerEnabled(this, i)) {
        		int menuItemNum = getMenuItem(i);
        		
        		MenuItem menuItem = menu.add(MENU_GROUP_SERVER, menuItemNum, menuItemNum, serverName);
        		
        		if (activeServer == i) {
        			checked = true;
        			menuItem.setChecked(true);
        		}
        	}
        }

		if (!checked) {
			menu.findItem(getMenuItem(0)).setChecked(true);	
        }
        
        menu.setGroupCheckable(MENU_GROUP_SERVER, true, true);
        menu.setHeaderTitle(R.string.main_select_server);
    }
    
    private boolean getActiveServerEnabled() {

		int activeServer = Util.getActiveServer(this);
		boolean activeServerEnabled = false;

		for (int i = 0; i <= Util.getActiveServers(this); i++) {
			if (Util.getServerEnabled(this, i)) {
				if (activeServer == i) {
					activeServerEnabled = true;
				}
			}
		}

		return activeServerEnabled;
    }
    
    private int getMenuItem(int serverInstance) {
    	switch (serverInstance) {
    	    case 0:
    	    	return MENU_ITEM_OFFLINE;
    		case 1:
    			return MENU_ITEM_SERVER_1;
    		case 2:
    			return MENU_ITEM_SERVER_2;
    		case 3:
    			return MENU_ITEM_SERVER_3;
    		case 4:
    			return MENU_ITEM_SERVER_4;
    		case 5:
    			return MENU_ITEM_SERVER_5;
    		case 6:
    			return MENU_ITEM_SERVER_6;
    		case 7:
    			return MENU_ITEM_SERVER_7;
    		case 8:
    			return MENU_ITEM_SERVER_8;
	   		case 9:
    			return MENU_ITEM_SERVER_9;    			    			    			
       		case 10:
    			return MENU_ITEM_SERVER_10;    			    			    			
    			    			    			    			
    	}
    	
		return 0;
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case MENU_ITEM_OFFLINE:
                setActiveServer(0);
                break;
            case MENU_ITEM_SERVER_1:
                setActiveServer(1);
                break;
            case MENU_ITEM_SERVER_2:
                setActiveServer(2);
                break;
            case MENU_ITEM_SERVER_3:
                setActiveServer(3);
                break;
            case MENU_ITEM_SERVER_4:
                setActiveServer(4);
                break;
            case MENU_ITEM_SERVER_5:
                setActiveServer(5);
                break;
            case MENU_ITEM_SERVER_6:
                setActiveServer(6);
                break;
            case MENU_ITEM_SERVER_7:
                setActiveServer(7);
                break;
            case MENU_ITEM_SERVER_8:
                setActiveServer(8);
                break; 
            case MENU_ITEM_SERVER_9:
                setActiveServer(9);
                break;   
            case MENU_ITEM_SERVER_10:
                setActiveServer(10);
                break;                                                                                                  
            default:
                return super.onContextItemSelected(menuItem);
        }

        // Restart activity
        restart();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
        switch (item.getItemId()) {

		    case R.id.menu_rescan:
				try {rescan();} catch (Exception e) {} 
				Util.toast(getBaseContext(), "Server is scaning ...");
			    return true;        

			case R.id.menu_refresh:
				refresh();
				return true;
		    
		    case R.id.menu_logs:
				getLogs();
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

    		case R.id.menu_changelog:
				ChangeLog changeLog = new ChangeLog(this, Util.getPreferences(this));
				changeLog.getFullLogDialog().show();		
				return true;                  
                
    		case R.id.menu_aboutDialog:
				showAboutDialog();
				return true;                

            case R.id.menu_help:
                startActivity(new Intent(this, HelpActivity.class));
                return true;
        }

        return false;
    }    
    
    private void setActiveServer(int instance) {
        if (Util.getActiveServer(this) != instance) {
            DownloadService service = getDownloadService();
            if (service != null) {
                service.clearIncomplete();
            }
            Util.setActiveServer(this, instance);
        }
    }

    private void refresh() {
        finish();
        Intent intent = getIntent();
        intent.putExtra(Constants.INTENT_EXTRA_NAME_REFRESH, true);
        Util.startActivityWithoutTransition(this, intent);
    }    
    
    private void restart() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Util.startActivityWithoutTransition(this, intent);
    }

    private void exit() {
        stopService(new Intent(this, DownloadServiceImpl.class));
        Util.unregisterMediaButtonEventReceiver(this);
        finish();
		
		//TODO:cleanup
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
    }

    private void showInfoDialog(boolean show) {
        if (!infoDialogDisplayed) {
             infoDialogDisplayed = true;
            if (Util.getRestUrl(this, null).contains("demo.subsonic.org")) {
                Util.info(this, R.string.main_welcome_title, R.string.main_welcome_text);
            } 
            else if (show || Util.getRestUrl(this, null).contains("yourhost")) {
                Util.info(this, R.string.main_welcome_title, R.string.main_welcome_text_offline);
            }
        }
    }

	private void showAboutDialog() {
		try {
			File rootFolder = FileUtil.getMusicDirectory(MainActivity.this);
			StatFs stat = new StatFs(rootFolder.getPath());
			long bytesTotalFs = (long) stat.getBlockCount() * (long) stat.getBlockSize();
			long bytesAvailableFs = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
			
			String msg = getResources().getString(R.string.main_about_text,
				getPackageManager().getPackageInfo(getPackageName(), 0).versionName,
				getPackageManager().getPackageInfo(getPackageName(), 0).versionCode,
				Util.formatBytes(FileUtil.getUsedSize(MainActivity.this, rootFolder)),
				Util.formatBytes(Util.getCacheSizeMB(MainActivity.this) * 1024L * 1024L),
				Util.formatBytes(bytesAvailableFs),
				Util.formatBytes(bytesTotalFs));
			Util.info(this, R.string.main_about_title, msg);
		} catch(Exception e) {
			Util.toast(MainActivity.this, "Failed to open dialog");
		}
		// Util.toast(MainActivity.this, "Size: " + Util.formatBytes(FileUtil.getUsedSize(MainActivity.this, FileUtil.getMusicDirectory(MainActivity.this))));
	}    
    
    private void showArtistAlbumList(String type) {		
        Intent intent = new Intent(this, SelectAlbumActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, type);
        intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, Util.getMaxArtists(this));
        intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0);
		Util.startActivityWithoutTransition(this, intent);
	}    
    
    private void showAlbumList(String type) {		
        Intent intent = new Intent(this, SelectAlbumActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_TYPE, type);
        intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, Util.getMaxAlbums(this));
        intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET, 0);
		Util.startActivityWithoutTransition(this, intent);
	}
    
    private void showStarredSongs() {
    	Intent intent = new Intent(this, SelectAlbumActivity.class);
    	intent.putExtra(Constants.INTENT_EXTRA_NAME_STARRED, 1);
    	Util.startActivityWithoutTransition(this, intent);
    }

    private void showRandomSongs() {
    	Intent intent = new Intent(this, SelectAlbumActivity.class);
    	intent.putExtra(Constants.INTENT_EXTRA_NAME_RANDOM, 1);
    	intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, Util.getMaxSongs(this));
    	Util.startActivityWithoutTransition(this, intent);
    }

    private void showLastplayedSongs() {
    	Intent intent = new Intent(this, SelectAlbumActivity.class);
    	intent.putExtra(Constants.INTENT_EXTRA_NAME_LASTPLAYED, 1);
    	intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 100);
    	Util.startActivityWithoutTransition(this, intent);
    }

    private void showNewaddedSongs() {
    	Intent intent = new Intent(this, SelectAlbumActivity.class);
    	intent.putExtra(Constants.INTENT_EXTRA_NAME_NEWADDED, 1);
    	intent.putExtra(Constants.INTENT_EXTRA_NAME_ALBUM_LIST_SIZE, 100);
    	Util.startActivityWithoutTransition(this, intent);
    }
    
    private void showGenres() {
    	Intent intent = new Intent(this, SelectGenreActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	Util.startActivityWithoutTransition(this, intent);
    }
 
    private void showArtistGenres() {
    	Intent intent = new Intent(this, SelectArtistGenreActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	Util.startActivityWithoutTransition(this, intent);
    }    
    
	private void showVideos() {
		final Intent intent = new Intent(this, SelectAlbumActivity.class);
		intent.putExtra(Constants.INTENT_EXTRA_NAME_VIDEOS, 1);
		Util.startActivityWithoutTransition(this, intent);
	}    
    
    private void rescan() {
        BackgroundTask<MainActivity> task = new TabActivityBackgroundTask<MainActivity>(this, true) {
            @Override
            protected MainActivity doInBackground() throws Throwable {
            	
                MusicService musicService = MusicServiceFactory.getMusicService(MainActivity.this);
                musicService.startRescan(MainActivity.this, this);
				return null;
            }

            @Override
            protected void done(MainActivity result) {}
        };
        task.execute();
    }
  
	private void PingPong() {		
		try {
			new SilentBackgroundTask<File>(this) {
				@Override
				protected File doInBackground() throws Throwable {
//					updateProgress("Contacting Server ... ");
	                MusicService musicService = MusicServiceFactory.getMusicService(MainActivity.this);
					musicService.ping(MainActivity.this, this);
					return null;
				}
				@Override
				protected void done(File done) {
				}
			}.execute();
		} catch(Exception e) {}
	}

	private Indexes getIndex() {
		
		final Indexes indexes = null;
		try {
			new SilentBackgroundTask<Indexes>(this) {
				@Override
				protected Indexes doInBackground() throws Throwable {
	                MusicService musicService = MusicServiceFactory.getMusicService(MainActivity.this);

	                if (!Util.isOffline(MainActivity.this)) {
	                    Object musicFolders = musicService.getMusicFolders(true, MainActivity.this, this);
	                }
	                String musicFolderId = Util.getSelectedMusicFolderId(MainActivity.this);
	                return musicService.getIndexes(musicFolderId, true, MainActivity.this, this);
	                
	                
				}
				@Override
				protected void done(Indexes result) {
				}
			    @Override
			    protected void onPostExecute(Indexes result) {
			    }
			}.execute();
			
		} catch(Exception e) {}
		return indexes;
		
	}	
	
    
	private Version getAPIVersion() {
		
		final Version version = null;
		try {
			new SilentBackgroundTask<Version>(this) {
				@Override
				protected Version doInBackground() throws Throwable {
	                MusicService musicService = MusicServiceFactory.getMusicService(MainActivity.this);
	                return musicService.getAPIVersion(MainActivity.this, this);
				}
				@Override
				protected void done(Version result) {
	                if (result != null) {
	    		        MainActivity.this.checkResponseForIntent(result);
	                }
				}
			    @Override
			    protected void onPostExecute(Version result) {
			        super.onPostExecute(result);
			        MainActivity.this.checkResponseForIntent(result);
			    }
			}.execute();
			
		} catch(Exception e) {}
		
		return MainActivity.this.RESTVersion;
	}
	
	protected void checkResponseForIntent(Version result) {
	    if (result != null) {
	    	MainActivity.this.RESTVersion = result;
	        return;
	    }
	}

    
	private void getLogs() {
		try {
			final String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			new ModalBackgroundTask<File>(this, false) {
				@Override
				protected File doInBackground() throws Throwable {
					updateProgress("Collecting Logs ...");
					File logcat = new File(FileUtil.getMadsonicDirectory(), "logcat.txt");
					Process logcatProc = null;

					try {
						List<String> progs = new ArrayList<String>();
						progs.add("logcat");
						progs.add("-v");
						progs.add("time");
						progs.add("-d");
						progs.add("-f");
						progs.add(logcat.getPath());
						progs.add("*:I");

						logcatProc = Runtime.getRuntime().exec(progs.toArray(new String[0]));
						logcatProc.waitFor();
					} catch(Exception e) {
						Util.toast(MainActivity.this, "Failed to gather logs");
					} finally {
						if(logcatProc != null) {
							logcatProc.destroy();
						}
					}

					return logcat;
				}

				@Override
				protected void done(File logcat) {
					Intent email = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
					email.setType("text/plain");

					email.putExtra(Intent.EXTRA_EMAIL, new String[] {"bugreport@madsonic.org"});
					email.putExtra(Intent.EXTRA_SUBJECT, "Madsonic " + version + " Bugreport");
					email.putExtra(Intent.EXTRA_TEXT, "Describe the problem here");
					
					ArrayList<Uri> uris = new ArrayList<Uri>();

					File stracktrace = new File(Environment.getExternalStorageDirectory(), "madsonic-stacktrace.txt");
					
					Uri attachmentLogcat = Uri.fromFile(logcat);
					Uri attachmentTrace = Uri.fromFile(stracktrace);
					if (attachmentLogcat != null) { uris.add(attachmentLogcat); }
					if (attachmentTrace != null) { uris.add(attachmentTrace); }
					
					email.putExtra(Intent.EXTRA_STREAM, uris);
					
					startActivity(email);
				}
			}.execute();
		} catch(Exception e) {}
	}


	
}