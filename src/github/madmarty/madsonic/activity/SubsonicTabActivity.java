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
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import github.madmarty.madsonic.R;

import github.madmarty.madsonic.domain.MusicDirectory;
import github.madmarty.madsonic.domain.Playlist;
import github.madmarty.madsonic.domain.Version;
import github.madmarty.madsonic.service.CachedMusicService;
import github.madmarty.madsonic.service.DownloadService;
import github.madmarty.madsonic.service.DownloadServiceImpl;
import github.madmarty.madsonic.service.MusicService;
import github.madmarty.madsonic.service.MusicServiceFactory;
import github.madmarty.madsonic.service.OfflineException;
import github.madmarty.madsonic.service.ServerTooOldException;
import github.madmarty.madsonic.util.Constants;
import github.madmarty.madsonic.util.ImageLoader;
import github.madmarty.madsonic.util.LoadingTask;
import github.madmarty.madsonic.util.ModalBackgroundTask;
import github.madmarty.madsonic.util.SilentBackgroundTask;
import github.madmarty.madsonic.util.Util;
import github.madmarty.madsonic.util.VideoPlayerType;

/**
 * @author Sindre Mehus
 */
public class SubsonicTabActivity extends Activity {

    private static final String TAG = SubsonicTabActivity.class.getSimpleName();
    private static ImageLoader IMAGE_LOADER;
    
	protected static String theme;

    private boolean destroyed;
    
    protected View mainBar;
    
    protected static Version RESTVersion;
    
    private View homeButton;
    private View musicButton;
    private View playlistButton;
    private View playlistButtonSep;
    private View chatButton;
    private View chatButtonSep;
    protected View searchButton;
    private View searchButtonSep;
    private View podcastButton;
    private View podcastButtonSep;

    private View nowPlayingButton;

    protected Drawable largeUnknownImage;
    
    protected CachedMusicService musicService;
   
    
    private void exit() {
        stopService(new Intent(this, DownloadServiceImpl.class));
        Util.unregisterMediaButtonEventReceiver(this);
        finish();
    }
    
    @Override
    protected void onCreate(Bundle bundle) {
        setUncaughtExceptionHandler();
        Util.changeLanguage(getBaseContext());
        applyTheme();
        super.onCreate(bundle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        startService(new Intent(this, DownloadServiceImpl.class));
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

	@Override
    protected void onPostCreate(Bundle bundle) {
		
        super.onPostCreate(bundle);

       
        homeButton = findViewById(R.id.button_bar_home);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SubsonicTabActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                Util.startActivityWithoutTransition(SubsonicTabActivity.this, intent);
            }
        });

        musicButton = findViewById(R.id.button_bar_music);
        musicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SubsonicTabActivity.this, SelectArtistActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                Util.startActivityWithoutTransition(SubsonicTabActivity.this, intent);
            }
        });

        searchButton = findViewById(R.id.button_bar_search);
        searchButtonSep =  findViewById(R.id.search_separator);
        
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSearch(null, false, null, false); 
            }
        });

        podcastButton = findViewById(R.id.button_bar_podcast);
        podcastButtonSep =  findViewById(R.id.podcast_separator);
        
        podcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
    	
                Intent intent = new Intent(SubsonicTabActivity.this, SelectPodcastsActivity.class);
                intent.putExtra(Constants.INTENT_EXTRA_NAME_PODCAST, true);
                Util.startActivityWithoutTransition(SubsonicTabActivity.this, intent);
            }
        });        
        
        chatButton = findViewById(R.id.button_bar_chat);
        chatButtonSep =  findViewById(R.id.chat_separator);
        
        chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	
    			Util.startActivityWithoutTransition(SubsonicTabActivity.this, ChatActivity.class);
            }
        });        
        
        playlistButton = findViewById(R.id.button_bar_playlists);
        playlistButtonSep =  findViewById(R.id.playlist_separator);

        playlistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SubsonicTabActivity.this, SelectPlaylistActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                Util.startActivityWithoutTransition(SubsonicTabActivity.this, intent);
            }
        });

        nowPlayingButton = findViewById(R.id.button_bar_now_playing);
        nowPlayingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Util.startActivityWithoutTransition(SubsonicTabActivity.this, DownloadActivity.class);
            }
        });

        if (this instanceof MainActivity) {
            homeButton.setEnabled(false);
        } else if (this instanceof SelectAlbumActivity || this instanceof SelectArtistActivity) {
            musicButton.setEnabled(false);
        } else if (this instanceof SelectPodcastsActivity) {
        	podcastButton.setEnabled(false);
        	podcastButtonSep.setVisibility(View.GONE);
        } else if (this instanceof SearchActivity) {
        	searchButton.setEnabled(false);
        	searchButtonSep.setVisibility(View.GONE);
        } else if (this instanceof SelectPlaylistActivity) {
        	playlistButton.setEnabled(false);
        	playlistButtonSep.setVisibility(View.GONE);
        } else if (this instanceof DownloadActivity || this instanceof LyricsActivity) {
            nowPlayingButton.setEnabled(false);
        }

        updateButtonVisibility();
    }

    @Override
    protected void onResume() {
        super.onResume();
		Util.registerMediaButtonEventReceiver(this);
		
        if (theme != null && !theme.equals(Util.getTheme(this))) {
            restart();
        }			
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
   // 	if (android.os.Build.VERSION.SDK_INT < 11) {
    		MenuInflater inflater = getMenuInflater();
    		inflater.inflate(R.menu.common, menu);
   // 	}
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

	    case R.id.menu_rescan:

		try {
			musicService.startRescan(SubsonicTabActivity.this, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
    protected void onDestroy() {
    	Util.unregisterMediaButtonEventReceiver(this);
        super.onDestroy();
        destroyed = true;
        getImageLoader().clear();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean isVolumeDown = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN;
        boolean isVolumeUp = keyCode == KeyEvent.KEYCODE_VOLUME_UP;
        boolean isVolumeAdjust = isVolumeDown || isVolumeUp;
        boolean isJukebox = getDownloadService() != null && getDownloadService().isJukeboxEnabled();

        if (isVolumeAdjust && isJukebox) {
            getDownloadService().adjustJukeboxVolume(isVolumeUp);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
	
	private void restart() {
        Intent intent = new Intent(this, this.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtras(getIntent());
        Util.startActivityWithoutTransition(this, intent);
    }

    public void finish() {
        super.finish();
        Util.disablePendingTransition(this);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);

        // Set the font of title in the action bar.
        TextView text = (TextView) findViewById(R.id.actionbar_title_text);
        
//      Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Storopia.ttf");
//      Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/NiseSonic.ttf");
//      Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Trendy.ttf");
//      Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf");
//      text.setTypeface(typeface);

        text.setText(title);
    }

    @Override
    public void setTitle(int titleId) {
        setTitle(getString(titleId));
    }

    private void applyTheme() {
    	
        theme = Util.getTheme(this);
    	
        if ("Madsonic Dark".equals(theme)) {
            setTheme(R.style.Madsonic_Dark);

        } else if ("Madsonic Light".equals(theme)) {
            setTheme(R.style.Madsonic_Light);

        } else if ("Madsonic Holo".equals(theme)) {
            setTheme(R.style.Madsonic_Holo);

        } else if ("Madsonic Red".equals(theme)) {
            setTheme(R.style.Madsonic_Red);
            
        } else if ("Madsonic Pink".equals(theme)) {
            setTheme(R.style.Madsonic_Pink);	
            
        } else if ("Madsonic Flawless".equals(theme)) {
            setTheme(R.style.Madsonic_Green);	

        } else if ("Madsonic Dark Fullscreen".equals(theme)) {
            setTheme(R.style.Madsonic_Dark_Fullscreen);
            
        } else if ("Madsonic Light Fullscreen".equals(theme)) {
            setTheme(R.style.Madsonic_Light_Fullscreen);

        } else if ("Madsonic Holo Fullscreen".equals(theme)) {
            setTheme(R.style.Madsonic_Holo_Fullscreen);

	    } else if ("Madsonic Red Fullscreen".equals(theme)) {
	        setTheme(R.style.Madsonic_Red_Fullscreen);
	        
	    } else if ("Madsonic Pink Fullscreen".equals(theme)) {
	        setTheme(R.style.Madsonic_Pink_Fullscreen);
	        
	    } else if ("Madsonic Flawless Fullscreen".equals(theme)) {
	        setTheme(R.style.Madsonic_Green_Fullscreen);
	    } 
        
    }    

	@SuppressLint("Override") public boolean isDestroyed() {
        return destroyed;
    }

    private void updateButtonVisibility() {
    	
        int visibility = Util.isOffline(this) ? View.GONE : View.VISIBLE;
        
		SharedPreferences prefs = Util.getPreferences(this);

		if(prefs.getBoolean(Constants.PREFERENCES_KEY_CHAT_ENABLED, true)) {
	        chatButton.setVisibility(visibility);
	        chatButtonSep.setVisibility(visibility);
		} else {
	        chatButton.setVisibility(View.GONE);
	        chatButtonSep.setVisibility(View.GONE);        
		}
		
		if(prefs.getBoolean(Constants.PREFERENCES_KEY_SEARCH_ENABLED, true)) {
			searchButton.setVisibility(visibility);
			searchButtonSep.setVisibility(visibility);
		} else {
			searchButton.setVisibility(View.GONE);
			searchButtonSep.setVisibility(View.GONE);        
		}
		
		if(prefs.getBoolean(Constants.PREFERENCES_KEY_PODCAST_ENABLED, true)) {
			podcastButton.setVisibility(visibility);
			podcastButtonSep.setVisibility(visibility);
		} else {
			podcastButton.setVisibility(View.GONE);
			podcastButtonSep.setVisibility(View.GONE);        
		}		
		
        playlistButton.setVisibility(visibility);
    	playlistButtonSep.setVisibility(visibility);
		
    }
    
    public void toggleStarredInBackground(final MusicDirectory.Entry entry, final ImageButton button) {
        
    	final boolean starred = !entry.isStarred();
    	
    	button.setImageResource(starred ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
    	entry.setStarred(starred);
    	
//        Util.toast(SubsonicTabActivity.this, getResources().getString(R.string.starring_content, entry.getTitle()));
        new SilentBackgroundTask<Void>(this) {
            @Override
            protected Void doInBackground() throws Throwable {
                MusicService musicService = MusicServiceFactory.getMusicService(SubsonicTabActivity.this);
				musicService.setStarred(entry.getId(), starred, SubsonicTabActivity.this, null);
                return null;
            }
            
            @Override
            protected void done(Void result) {
//              Util.toast(SubsonicTabActivity.this, getResources().getString(R.string.starring_content_done, entry.getTitle()));
            }
             
            @Override
            protected void error(Throwable error) {
            	button.setImageResource(!starred ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
            	entry.setStarred(!starred);
            	
            	String msg;
            	if (error instanceof OfflineException || error instanceof ServerTooOldException) {
            		msg = getErrorMessage(error);
            	} else {
            		msg = getResources().getString(R.string.starring_content_error, entry.getTitle()) + " " + getErrorMessage(error);
            	}
            	
        		Util.toast(SubsonicTabActivity.this, msg, false);
            }
        }.execute();
    }

    public void setProgressVisible(boolean visible) {
        View view = findViewById(R.id.tab_progress);
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public void updateProgress(String message) {
        TextView view = (TextView) findViewById(R.id.tab_progress_message);
        if (view != null) {
            view.setText(message);
        }
    }

    public DownloadService getDownloadService() {
        // If service is not available, request it to start and wait for it.
        for (int i = 0; i < 5; i++) {
            DownloadService downloadService = DownloadServiceImpl.getInstance();
            if (downloadService != null) {
                return downloadService;
            }
            Log.w(TAG, "DownloadService not running. Attempting to start it.");
            startService(new Intent(this, DownloadServiceImpl.class));
            Util.sleepQuietly(50L);
        }
        return DownloadServiceImpl.getInstance();
    }

    protected void warnIfNetworkOrStorageUnavailable() {
        if (!Util.isExternalStoragePresent()) {
            Util.toast(this, R.string.select_album_no_sdcard);
        } else if (!Util.isOffline(this) && !Util.isNetworkConnected(this)) {
            Util.toast(this, R.string.select_album_no_network);
        }
    }

    protected synchronized ImageLoader getImageLoader() {
        if (IMAGE_LOADER == null) {
            IMAGE_LOADER = new ImageLoader(this);
        }
        return IMAGE_LOADER;
    }

	public synchronized static ImageLoader getStaticImageLoader(Context context) {
		if (IMAGE_LOADER == null) {
            IMAGE_LOADER = new ImageLoader(context);
        }
		return IMAGE_LOADER;
	}

    protected void setBackAction(final Runnable runnable) {

        View backLayout = findViewById(R.id.actionbar_back_layout);
        backLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runnable.run();
            }
        });
    //    backLayout.setBackgroundResource(R.drawable.actionbar_button);

        findViewById(R.id.actionbar_back).setVisibility(View.VISIBLE);
    }
    protected void downloadRecursively(final String id, final boolean save, final boolean append, final boolean autoplay, final boolean shuffle) {
        ModalBackgroundTask<List<MusicDirectory.Entry>> task = new ModalBackgroundTask<List<MusicDirectory.Entry>>(this, false) {

            private static final int MAX_SONGS = 500;

            @Override
            protected List<MusicDirectory.Entry> doInBackground() throws Throwable {
                MusicService musicService = MusicServiceFactory.getMusicService(SubsonicTabActivity.this);
                MusicDirectory root = musicService.getMusicDirectory(id, false, SubsonicTabActivity.this, this);
                List<MusicDirectory.Entry> songs = new LinkedList<MusicDirectory.Entry>();
                getSongsRecursively(root, songs);
                return songs;
            }

            private void getSongsRecursively(MusicDirectory parent, List<MusicDirectory.Entry> songs) throws Exception {
                if (songs.size() > MAX_SONGS) {
                    return;
                }

                for (MusicDirectory.Entry song : parent.getChildren(false, true)) {
                    if (!song.isVideo()) {
                        songs.add(song);
                    }
                }
                for (MusicDirectory.Entry dir : parent.getChildren(true, false)) {
                    MusicService musicService = MusicServiceFactory.getMusicService(SubsonicTabActivity.this);
                    getSongsRecursively(musicService.getMusicDirectory(dir.getId(), false, SubsonicTabActivity.this, this), songs);
                }
            }

            @Override
            protected void done(List<MusicDirectory.Entry> songs) {
                DownloadService downloadService = getDownloadService();
                if (!songs.isEmpty() && downloadService != null) {
                    if (!append) {
                        downloadService.clear();
                    }
                    warnIfNetworkOrStorageUnavailable();
                    downloadService.download(songs, save, autoplay, false, shuffle);
                    Util.startActivityWithoutTransition(SubsonicTabActivity.this, DownloadActivity.class);
                }
            }
        };

        task.execute();
    }

   protected void playVideo(MusicDirectory.Entry entry)  {
        if (!Util.isNetworkConnected(this)) {
            Util.toast(this, R.string.select_album_no_network);
            return;
        }

        VideoPlayerType player = Util.getVideoPlayerType(this);
        try {
            player.playVideo(this, entry);
        } catch (Exception e) {
            Util.toast(this, e.getMessage(), false);
        }
   }
	
	protected void addToPlaylist(final List<MusicDirectory.Entry> songs) {
		if(songs.isEmpty()) {
			Util.toast(this, "No songs selected");
			return;
		}
		
		new LoadingTask<List<Playlist>>(this, true) {
            @Override
            protected List<Playlist> doInBackground() throws Throwable {
                MusicService musicService = MusicServiceFactory.getMusicService(SubsonicTabActivity.this);
				return musicService.getPlaylists(false, SubsonicTabActivity.this, this);
            }
            
            @Override
            protected void done(final List<Playlist> playlists) {
				List<String> names = new ArrayList<String>();
				names.add("Create New");
				for(Playlist playlist: playlists) {
					names.add(playlist.getName());
				}
				
				AlertDialog.Builder builder = new AlertDialog.Builder(SubsonicTabActivity.this);
				builder.setTitle("Add to Playlist")
					.setItems(names.toArray(new CharSequence[names.size()]), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						
						if(which > 0) {
							addToPlaylist(playlists.get(which - 1), songs);
						} else {
							createNewPlaylist(songs, false);
						}
					}
				});
				AlertDialog dialog = builder.create();
				dialog.show();
            }
            
            @Override
            protected void error(Throwable error) {            	
            	String msg;
            	if (error instanceof OfflineException || error instanceof ServerTooOldException) {
            		msg = getErrorMessage(error);
            	} else {
            		msg = getResources().getString(R.string.playlist_error) + " " + getErrorMessage(error);
            	}
            	
        		Util.toast(SubsonicTabActivity.this, msg, false);
            }
        }.execute();
	}
	
	private void addToPlaylist(final Playlist playlist, final List<MusicDirectory.Entry> songs) {		
		new SilentBackgroundTask<Void>(this) {
            @Override
            protected Void doInBackground() throws Throwable {
                MusicService musicService = MusicServiceFactory.getMusicService(SubsonicTabActivity.this);
				musicService.addToPlaylist(playlist.getId(), songs, SubsonicTabActivity.this, null);
                return null;
            }
            
            @Override
            protected void done(Void result) {
                Util.toast(SubsonicTabActivity.this, getResources().getString(R.string.updated_playlist, songs.size(), playlist.getName()));
            }
            
            @Override
            protected void error(Throwable error) {            	
            	String msg;
            	if (error instanceof OfflineException || error instanceof ServerTooOldException) {
            		msg = getErrorMessage(error);
            	} else {
            		msg = getResources().getString(R.string.updated_playlist_error, playlist.getName()) + " " + getErrorMessage(error);
            	}
            	
        		Util.toast(SubsonicTabActivity.this, msg, false);
            }
        }.execute();
	}    
    
	protected void createNewPlaylist(final List<MusicDirectory.Entry> songs, boolean getSuggestion) {
		
		View layout = this.getLayoutInflater().inflate(R.layout.save_playlist, null);
		final EditText playlistNameView = (EditText) layout.findViewById(R.id.save_playlist_name);
		final CheckBox overwriteCheckBox = (CheckBox) layout.findViewById(R.id.save_playlist_overwrite);
		if(getSuggestion) {
			String playlistName = (getDownloadService() != null) ? getDownloadService().getSuggestedPlaylistName() : null;
			if (playlistName != null) {
				playlistNameView.setText(playlistName);
				Version version = Util.getServerRestVersion(this);
				Version updatePlaylistVersion = new Version("1.8.0");
				try {
					if(version.compareTo(updatePlaylistVersion) >= 0 && Integer.parseInt(getDownloadService().getSuggestedPlaylistId()) != -1) {
						overwriteCheckBox.setChecked(true);
						overwriteCheckBox.setVisibility(View.VISIBLE);
					}
				} catch(Exception e) {
					Log.d(TAG, "Playlist id isn't a integer, probably MusicCabinet");
				}
			} else {
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				playlistNameView.setText(dateFormat.format(new Date()));
			}
		} else {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			playlistNameView.setText(dateFormat.format(new Date()));
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.download_playlist_title)
			.setMessage(R.string.download_playlist_name)
			.setView(layout)
			.setPositiveButton(R.string.common_save, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					if(overwriteCheckBox.isChecked()) {
						overwritePlaylist(songs, String.valueOf(playlistNameView.getText()), getDownloadService().getSuggestedPlaylistId());
					} else {
						createNewPlaylist(songs, String.valueOf(playlistNameView.getText()));
					}
				}
			})
			.setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			})
			.setCancelable(true);
		
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	private void createNewPlaylist(final List<MusicDirectory.Entry> songs, final String name) {
		new SilentBackgroundTask<Void>(this) {
			@Override
			protected Void doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(SubsonicTabActivity.this);
				musicService.createPlaylist(null, name, songs, SubsonicTabActivity.this, null);
				return null;
			}

			@Override
			protected void done(Void result) {
				Util.toast(SubsonicTabActivity.this, R.string.download_playlist_done);
			}

			@Override
			protected void error(Throwable error) {
				String msg = SubsonicTabActivity.this.getResources().getString(R.string.download_playlist_error) + " " + getErrorMessage(error);
				Util.toast(SubsonicTabActivity.this, msg);
			}
		}.execute();
	}
	private void overwritePlaylist(final List<MusicDirectory.Entry> songs, final String name, final String id) {
		new SilentBackgroundTask<Void>(this) {
			@Override
			protected Void doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(SubsonicTabActivity.this);
				MusicDirectory playlist = musicService.getPlaylist(id, name, SubsonicTabActivity.this, null);
				List<MusicDirectory.Entry> toDelete = playlist.getChildren();
				musicService.overwritePlaylist(id, name, toDelete.size(), songs, SubsonicTabActivity.this, null);
				return null;
			} 

			@Override
			protected void done(Void result) {
				Util.toast(SubsonicTabActivity.this, R.string.download_playlist_done);
			}

			@Override
			protected void error(Throwable error) {            	
				String msg;
				if (error instanceof OfflineException || error instanceof ServerTooOldException) {
					msg = getErrorMessage(error);
				} else {
					msg = SubsonicTabActivity.this.getResources().getString(R.string.download_playlist_error) + " " + getErrorMessage(error);
				}

				Util.toast(SubsonicTabActivity.this, msg, false);
			}
		}.execute();
	}
    private void setUncaughtExceptionHandler() {
        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        if (!(handler instanceof SubsonicUncaughtExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new SubsonicUncaughtExceptionHandler(this));
        }
    }

    /**
     * Logs the stack trace of uncaught exceptions to a file on the SD card.
     */
    private static class SubsonicUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        private final Thread.UncaughtExceptionHandler defaultHandler;
        private final Context context;

        private SubsonicUncaughtExceptionHandler(Context context) {
            this.context = context;
            defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        }

        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            File file = null;
            PrintWriter printWriter = null;
            try {

                PackageInfo packageInfo = context.getPackageManager().getPackageInfo("github.madmarty.madsonic", 0);
                file = new File(Environment.getExternalStorageDirectory(), "madsonic-stacktrace.txt");
                printWriter = new PrintWriter(file);
                printWriter.println("Android API level: " + Build.VERSION.SDK);
                printWriter.println("Madsonic version name: " + packageInfo.versionName);
                printWriter.println("Madsonic version code: " + packageInfo.versionCode);
                printWriter.println();
                throwable.printStackTrace(printWriter);
                Log.i(TAG, "Stack trace written to " + file);
            } catch (Throwable x) {
                Log.e(TAG, "Failed to write stack trace to " + file, x);
            } finally {
                Util.close(printWriter);
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable);
                }

            }
        }
    }
}

