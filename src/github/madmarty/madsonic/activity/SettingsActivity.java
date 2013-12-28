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

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import android.text.InputType;
import github.madmarty.madsonic.R;
import github.madmarty.madsonic.domain.SrvSettings.ServerEntry;
import github.madmarty.madsonic.provider.MadsonicSearchSuggestionProvider;
import github.madmarty.madsonic.service.DownloadService;
import github.madmarty.madsonic.service.DownloadServiceImpl;
import github.madmarty.madsonic.service.MusicService;
import github.madmarty.madsonic.service.MusicServiceFactory;
import github.madmarty.madsonic.util.Constants;
import github.madmarty.madsonic.util.ErrorDialog;
import github.madmarty.madsonic.util.FileUtil;
import github.madmarty.madsonic.util.Logger;
import github.madmarty.madsonic.util.ModalBackgroundTask;
import github.madmarty.madsonic.util.TemplateReader;
import github.madmarty.madsonic.util.Util;

import java.io.File;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final Logger LOG = new Logger(SettingsActivity.class);
    
    private final Map<String, ServerSettings> serverSettings = new LinkedHashMap<String, ServerSettings>();
    
    private boolean testingConnection;
    
    private ListPreference language;
    
    private ListPreference theme;
    private ListPreference coverSize;
    
    private ListPreference maxBitrateWifi;
    private ListPreference maxBitrateMobile;
	private ListPreference maxVideoBitrateWifi;
    private ListPreference maxVideoBitrateMobile;
	
    private ListPreference defaultVideoplayer;
    private ListPreference videoPlayer;
		
	private ListPreference networkTimeout;
    private ListPreference cacheSize;
    private EditTextPreference cacheLocation;
    private ListPreference preloadCount;
    private ListPreference bufferLength;
	private EditTextPreference randomSize;
	
    private ListPreference maxAlbums;
    private ListPreference maxSongs;
    private ListPreference maxArtists;
    
//  private ListPreference defaultAlbums;
//  private ListPreference defaultSongs;
//  private ListPreference defaultArtists;
	private ListPreference audioFocus;
    private CheckBoxPreference gaplessPlaybackEnabled;   
    private ListPreference chatRefreshInterval;
    
    private int maxServerCount = 10;
    private int minServerCount = 0;
    private int activeServers = 0;
	
    SharedPreferences settings;
    PreferenceCategory serversCategory;
    Preference ServerPreference;
    Preference addServerPreference;

	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.settings);

        language = (ListPreference) findPreference(Constants.PREFERENCES_KEY_LANGUAGE);        
        theme = (ListPreference) findPreference(Constants.PREFERENCES_KEY_THEME);
//      coverSize = (ListPreference) findPreference(Constants.PREFERENCES_KEY_COVER_SIZE);
        maxBitrateWifi = (ListPreference) findPreference(Constants.PREFERENCES_KEY_MAX_BITRATE_WIFI);
        maxBitrateMobile = (ListPreference) findPreference(Constants.PREFERENCES_KEY_MAX_BITRATE_MOBILE);
		maxVideoBitrateWifi = (ListPreference) findPreference(Constants.PREFERENCES_KEY_MAX_VIDEO_BITRATE_WIFI);
        maxVideoBitrateMobile = (ListPreference) findPreference(Constants.PREFERENCES_KEY_MAX_VIDEO_BITRATE_MOBILE);
        videoPlayer = (ListPreference) findPreference(Constants.PREFERENCES_KEY_VIDEO_PLAYER);
        defaultVideoplayer = (ListPreference) findPreference(Constants.PREFERENCES_KEY_DEFAULT_VIDEOPLAYER);
		networkTimeout = (ListPreference) findPreference(Constants.PREFERENCES_KEY_NETWORK_TIMEOUT);
        cacheSize = (ListPreference) findPreference(Constants.PREFERENCES_KEY_CACHE_SIZE);
        cacheLocation = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_CACHE_LOCATION);
        preloadCount = (ListPreference) findPreference(Constants.PREFERENCES_KEY_PRELOAD_COUNT);
        bufferLength = (ListPreference) findPreference(Constants.PREFERENCES_KEY_BUFFER_LENGTH);
        maxAlbums = (ListPreference) findPreference(Constants.PREFERENCES_KEY_MAX_ALBUMS);
        maxSongs = (ListPreference) findPreference(Constants.PREFERENCES_KEY_MAX_SONGS);
        maxArtists = (ListPreference) findPreference(Constants.PREFERENCES_KEY_MAX_ARTISTS);
//      defaultArtists = (ListPreference) findPreference(Constants.PREFERENCES_KEY_DEFAULT_ARTISTS);
//      defaultSongs = (ListPreference) findPreference(Constants.PREFERENCES_KEY_DEFAULT_SONGS);
//      defaultAlbums = (ListPreference) findPreference(Constants.PREFERENCES_KEY_DEFAULT_ALBUMS);
		randomSize = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_RANDOM_SIZE);
		audioFocus = (ListPreference) findPreference(Constants.PREFERENCES_KEY_AUDIO_FOCUS);
        gaplessPlaybackEnabled = (CheckBoxPreference) findPreference(Constants.PREFERENCES_KEY_GAPLESS_PLAYBACK);
        chatRefreshInterval = (ListPreference) findPreference(Constants.PREFERENCES_KEY_CHAT_REFRESH_INTERVAL);


        if (Build.VERSION.SDK_INT < 14) {
        	gaplessPlaybackEnabled.setChecked(false);
        	gaplessPlaybackEnabled.setEnabled(false);
        }

//        findPreference(Constants.PREFERENCES_KEY_CLEAR_SEARCH_HISTORY).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(Preference preference) {
//                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(SettingsActivity.this, MadsonicSearchSuggestionProvider.AUTHORITY, MadsonicSearchSuggestionProvider.MODE);
//                suggestions.clearHistory();
//                Util.toast(SettingsActivity.this, R.string.settings_search_history_cleared);
//                return false;
//            }
//        });

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        activeServers = settings.getInt(Constants.PREFERENCES_KEY_ACTIVE_SERVERS, 0);

        serversCategory = (PreferenceCategory) findPreference(Constants.PREFERENCES_KEY_SERVERS_KEY);

        addServerPreference = new Preference(this);
        addServerPreference.setKey(Constants.PREFERENCES_KEY_ADD_SERVER);
        addServerPreference.setPersistent(false);
        addServerPreference.setTitle(getResources().getString(R.string.settings_server_add_server));
        addServerPreference.setEnabled(activeServers < maxServerCount);
        serversCategory.addPreference(addServerPreference);

        
        ///////////////////////////// ADD DEMO SERVER ////////////////////////
        findPreference(Constants.PREFERENCES_KEY_ADD_DEMOSERVER).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
			    activeServers++;
    
				SharedPreferences.Editor prefEditor = settings.edit();
				prefEditor.putInt(Constants.PREFERENCES_KEY_ACTIVE_SERVERS, activeServers);
				prefEditor.commit();

				Util.removeInstanceName(SettingsActivity.this, activeServers); 
				Preference deleteServerPreference = findPreference(Constants.PREFERENCES_KEY_SERVER + activeServers);
				if ( deleteServerPreference != null) { serversCategory.removePreference(deleteServerPreference); }  	      	        

				Preference addServerPreference = findPreference(Constants.PREFERENCES_KEY_ADD_SERVER);
				serversCategory.addPreference(addServer(activeServers, null, true));
				addServerPreference.setEnabled(activeServers < maxServerCount);
				
				String instance = String.valueOf(activeServers);
				serverSettings.put(instance, new ServerSettings(instance));

				prefEditor.commit();
				update();

	          	return true;
            }
        });          
        
        ///////////////////////////// IMPORT XML TEMPLATE ////////////////////////
        File template = new File(FileUtil.getMadsonicDirectory(), "madsonic.xml");
        if (template != null) {
        	findPreference(Constants.PREFERENCES_KEY_IMPORT_TEMPLATE).setSummary(template.toString());        		
        }
        
        findPreference(Constants.PREFERENCES_KEY_IMPORT_TEMPLATE).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
				activeServers = 0;
				List<ServerEntry> importTemplate = null;
				try { importTemplate = TemplateReader.getTemplates().getChildren();}
				catch (Exception e) {
					Util.toast(getBaseContext(), "Can't read from madsonic.xml file!");
				}
              
				if (importTemplate != null) {
                  SharedPreferences.Editor prefEditor = settings.edit();
	      	      for (int i = 1; i <= maxServerCount; i++) { 
	      	    	  Util.removeInstanceName(SettingsActivity.this, i); 
	      	    	  Preference deleteServerPreference = findPreference(Constants.PREFERENCES_KEY_SERVER + i);
	      	    	  if ( deleteServerPreference != null) { serversCategory.removePreference(deleteServerPreference); }
	      	      }
		      	  prefEditor.commit();
            	  
                  for (int i=0; i<importTemplate.size(); i++) {
                   	final int instanceValue = i+1;
                    activeServers++;
    	            prefEditor.putInt(Constants.PREFERENCES_KEY_ACTIVE_SERVERS, activeServers);
  	      	        prefEditor.commit();

  	      	        Preference addServerPreference = findPreference(Constants.PREFERENCES_KEY_ADD_SERVER);
                	serversCategory.addPreference(addServer(instanceValue, importTemplate.get(i), false));
                	
          	    	String instance = String.valueOf(instanceValue);
          	    	serverSettings.put(instance, new ServerSettings(instance));
                  } 
                  update();
              }
			  return true;
            }
        });        

        ///////////////////////////// REMOVE IMPORT TEMPLATE BUTTON ////////////////////////
        Preference addDemoServerButton = findPreference(Constants.PREFERENCES_KEY_ADD_DEMOSERVER);
		if (activeServers != 0) {
			serversCategory.removePreference(addDemoServerButton);
		}        
        
        ///////////////////////////// REMOVE IMPORT TEMPLATE BUTTON ////////////////////////
		Preference importTemplateButton = findPreference(Constants.PREFERENCES_KEY_IMPORT_TEMPLATE);
		if (TemplateReader.getTemplates() == null) {
				serversCategory.removePreference(importTemplateButton);
		}
        
        for (int i = 1; i <= activeServers; i++) {
        	final int instanceValue = i;
        	serversCategory.addPreference(addServer(i, null, false));
						
            findPreference(Constants.PREFERENCES_KEY_TEST_CONNECTION + i).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    testConnection(instanceValue);
                    return false;
                }
            });
        	
            String instance = String.valueOf(i);
            serverSettings.put(instance, new ServerSettings(instance));
          
        }
        
        addServerPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
            	if (activeServers == maxServerCount) {
            		return false;
            	}
            	activeServers++;
            	
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putInt(Constants.PREFERENCES_KEY_ACTIVE_SERVERS, activeServers);
                prefEditor.commit();

            	Preference addServerPreference = findPreference(Constants.PREFERENCES_KEY_ADD_SERVER);
            	serversCategory.removePreference(addServerPreference);
            	serversCategory.addPreference(addServer(activeServers, null, false));
            	serversCategory.addPreference(addServerPreference);
            	
            	String instance = String.valueOf(activeServers);
            	serverSettings.put(instance, new ServerSettings(instance));
            	
            	addServerPreference.setEnabled(activeServers < maxServerCount);

            	return true;
            }
        });
        
        SharedPreferences prefs = Util.getPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        
        update();
    }
	
    private PreferenceScreen addServer(final int instance, ServerEntry importServer, boolean demo) {
    	
    	final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);
    	
    	screen.setTitle(R.string.settings_server_unused);
    	screen.setKey(Constants.PREFERENCES_KEY_SERVER + instance);

      	if (importServer != null) {
	     	screen.setTitle(importServer.getServername().toString());
      	}
      	
    	// ----------------------------------------------      	
    	final EditTextPreference serverNamePreference = new EditTextPreference(this);
    	serverNamePreference.setKey(Constants.PREFERENCES_KEY_SERVER_NAME + instance);
    	serverNamePreference.setDefaultValue(getResources().getString(R.string.settings_server_unused));
    	serverNamePreference.setTitle(R.string.settings_server_name);
    	if (serverNamePreference.getText() == null) {
    		serverNamePreference.setText(getResources().getString(R.string.settings_server_unused));
    	}
    	serverNamePreference.setSummary(serverNamePreference.getText());
    	if (importServer != null) {
        	serverNamePreference.setText(importServer.getServername());
        	serverNamePreference.setSummary(importServer.getServername());
        	if (serverNamePreference.getText() == null) {
        		serverNamePreference.setText(importServer.getServername());
        	}
    	}
    	// ----------------------------------------------    	
    	final EditTextPreference serverUrlPreference = new EditTextPreference(this);
    	serverUrlPreference.setKey(Constants.PREFERENCES_KEY_SERVER_URL + instance);
    	serverUrlPreference.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_URI);
    	serverUrlPreference.setDefaultValue("http://yourhost");
    	serverUrlPreference.setTitle(R.string.settings_server_address);
    	if (serverUrlPreference.getText() == null) {
    		serverUrlPreference.setText("http://yourhost");
    	}
    	serverUrlPreference.setSummary(serverUrlPreference.getText());

    	if (importServer != null) {
        	serverUrlPreference.setDefaultValue(importServer.getServerURL());    		
  	    	serverUrlPreference.setText(importServer.getServerURL());
	    	serverUrlPreference.setSummary(importServer.getServerURL());
	    	if (serverUrlPreference.getText() == null) {
	    		serverUrlPreference.setText(importServer.getServerURL());
	    	}        	
      	}
    	
    	screen.setSummary(serverUrlPreference.getText());
      	if (importServer != null) {
	     	screen.setTitle(importServer.getServername());
	     	screen.setSummary(importServer.getServername());
      	}
    	
    	// ----------------------------------------------
      	final EditTextPreference serverUsernamePreference = new EditTextPreference(this);
    	serverUsernamePreference.setKey(Constants.PREFERENCES_KEY_USERNAME + instance);
    	serverUsernamePreference.setText("guest");
    	serverUsernamePreference.setTitle(R.string.settings_server_username);

    	if (importServer != null) {
	    	serverUsernamePreference.setText(importServer.getUsername());
	    	serverUsernamePreference.setSummary(importServer.getUsername());
    	}    	

    	// ----------------------------------------------
    	final EditTextPreference serverPasswordPreference = new EditTextPreference(this);
    	serverPasswordPreference.setKey(Constants.PREFERENCES_KEY_PASSWORD + instance);
    	serverPasswordPreference.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    	serverPasswordPreference.setSummary("*****");
    	serverPasswordPreference.setTitle(R.string.settings_server_password);

    	if (importServer != null) {
    		
	    	serverPasswordPreference.setText(Util.decrypt(importServer.getPassword()));
	    	serverPasswordPreference.setSummary("*****"); 
    	}    	
    	
    	final CheckBoxPreference serverEnabledPreference = new CheckBoxPreference(this);
    	serverEnabledPreference.setDefaultValue(true);
    	serverEnabledPreference.setKey(Constants.PREFERENCES_KEY_SERVER_ENABLED + instance);
    	serverEnabledPreference.setTitle(R.string.equalizer_enabled);
    	
    	Preference serverRemoveServerPreference = new Preference(this);
    	serverRemoveServerPreference.setKey(Constants.PREFERENCES_KEY_REMOVE_SERVER + instance);
    	serverRemoveServerPreference.setPersistent(false);
    	serverRemoveServerPreference.setTitle(R.string.settings_server_remove_server);

    	///// ADD DEMO SERVER /////
    	if (demo) {
    		
	     	screen.setTitle("subsonic.org Demo Server");
	     	screen.setSummary("http://demo.subsonic.org");
    		
	    	serverNamePreference.setText("subsonic.org Demo Server");
	    	serverNamePreference.setSummary("subsonic.org Demo Server");
			serverUrlPreference.setText("http://demo.subsonic.org");
			serverUrlPreference.setSummary("http://demo.subsonic.org");
	    	serverUrlPreference.setDefaultValue("http://demo.subsonic.org");
	    	serverUsernamePreference.setText("android-guest");
	    	serverUsernamePreference.setSummary("android-guest");
	    	serverPasswordPreference.setText("guest");
	    	serverPasswordPreference.setSummary("*****"); 
    	}
    	
    	serverRemoveServerPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

            	if (activeServers == minServerCount) {
            		return false;
            	}
            	
            	serverNamePreference.setText(null);
            	serverUrlPreference.setText(null);
            	serverUsernamePreference.setText(null);
            	serverPasswordPreference.setText(null);
            	serverEnabledPreference.setChecked(true);
            	
            	if (instance < activeServers) {
            		
            		int activeServer = Util.getActiveServer(SettingsActivity.this);
            		for (int i = instance; i <= activeServers; i++) {
            			Util.removeInstanceName(SettingsActivity.this, i, activeServer);
            		}
            	}

                activeServers--;
                serversCategory.removePreference(screen);
                
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putInt(Constants.PREFERENCES_KEY_ACTIVE_SERVERS, activeServers);
                prefEditor.commit();
                
                addServerPreference.setEnabled(activeServers < maxServerCount);
                screen.getDialog().dismiss();

                return true;
            }
        });
    	
    	Preference serverTestConnectionPreference = new Preference(this);
    	serverTestConnectionPreference.setKey(Constants.PREFERENCES_KEY_TEST_CONNECTION + instance);
    	serverTestConnectionPreference.setPersistent(false);
    	serverTestConnectionPreference.setTitle(R.string.settings_test_connection_title);
    	serverTestConnectionPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                testConnection(instance);
                return false;
            }
        });
    		
    	screen.addPreference(serverEnabledPreference);
    	screen.addPreference(serverNamePreference);
    	screen.addPreference(serverUrlPreference);
    	screen.addPreference(serverUsernamePreference);
    	screen.addPreference(serverPasswordPreference);
    	screen.addPreference(serverTestConnectionPreference);
    	screen.addPreference(serverRemoveServerPreference);
    	
    	return screen;
    }
    
    private void applyTheme() {
    
    	String theme = Util.getTheme(this);
        
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
   
    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences prefs = Util.getPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        LOG.debug( "Preference changed: " + key);
        update();

        if (Constants.PREFERENCES_KEY_HIDE_MEDIA.equals(key)) {
            setHideMedia(sharedPreferences.getBoolean(key, false));
        }
        else if (Constants.PREFERENCES_KEY_MEDIA_BUTTONS.equals(key)) {
            setMediaButtonsEnabled(sharedPreferences.getBoolean(key, true));
        }
        else if (Constants.PREFERENCES_KEY_CACHE_LOCATION.equals(key)) {
            setCacheLocation(sharedPreferences.getString(key, ""));
        }
    }

    private void update() {
        if (testingConnection) {
            return;
        }
        
        language.setSummary(language.getEntry());
        theme.setSummary(theme.getEntry());
//        coverSize.setSummary(coverSize.getEntry());
        maxBitrateWifi.setSummary(maxBitrateWifi.getEntry());
        maxBitrateMobile.setSummary(maxBitrateMobile.getEntry());
		maxVideoBitrateWifi.setSummary(maxVideoBitrateWifi.getEntry());
        maxVideoBitrateMobile.setSummary(maxVideoBitrateMobile.getEntry());
//        defaultVideoplayer.setSummary(defaultVideoplayer.getEntry());
        videoPlayer.setSummary(videoPlayer.getEntry());
		networkTimeout.setSummary(networkTimeout.getEntry());
        cacheSize.setSummary(cacheSize.getEntry());
        cacheLocation.setSummary(cacheLocation.getText());
        preloadCount.setSummary(preloadCount.getEntry());
        bufferLength.setSummary(bufferLength.getEntry());
		randomSize.setSummary(randomSize.getText());
        maxAlbums.setSummary(maxAlbums.getEntry());
        maxArtists.setSummary(maxArtists.getEntry());
        maxSongs.setSummary(maxSongs.getEntry());
//      defaultAlbums.setSummary(defaultAlbums.getEntry());
//      defaultArtists.setSummary(defaultArtists.getEntry());
//      defaultSongs.setSummary(defaultSongs.getEntry());
		audioFocus.setSummary(audioFocus.getEntry());
        chatRefreshInterval.setSummary(chatRefreshInterval.getEntry());
		
        for (ServerSettings ss : serverSettings.values()) {
            ss.update();
        }
    }

    private void setHideMedia(boolean hide) {
        File nomediaDir = new File(FileUtil.getMadsonicDirectory(), ".nomedia");
        if (hide && !nomediaDir.exists()) {
            if (!nomediaDir.mkdir()) {
                LOG.warn( "Failed to create " + nomediaDir);
            }
        } else if (nomediaDir.exists()) {
            if (!nomediaDir.delete()) {
                LOG.warn( "Failed to delete " + nomediaDir);
            }
        }
        Util.toast(this, R.string.settings_hide_media_toast, false);
    }

    private void setMediaButtonsEnabled(boolean enabled) {
        if (enabled) {
            Util.registerMediaButtonEventReceiver(this);
        } else {
            Util.unregisterMediaButtonEventReceiver(this);
        }
    }

    private void setCacheLocation(String path) {
        File dir = new File(path);
        if (!FileUtil.ensureDirectoryExistsAndIsReadWritable(dir)) {
            Util.toast(this, R.string.settings_cache_location_error, false);

            // Reset it to the default.
            String defaultPath = FileUtil.getDefaultMusicDirectory().getPath();
            if (!defaultPath.equals(path)) {
                SharedPreferences prefs = Util.getPreferences(this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(Constants.PREFERENCES_KEY_CACHE_LOCATION, defaultPath);
                editor.commit();
                cacheLocation.setSummary(defaultPath);
                cacheLocation.setText(defaultPath);
            }

            // Clear download queue.
            DownloadService downloadService = DownloadServiceImpl.getInstance();
            downloadService.clear();
        }
    }

    private void testConnection(final int instance) {
        ModalBackgroundTask<Boolean> task = new ModalBackgroundTask<Boolean>(this, false) {
            private int previousInstance;

            @Override
            protected Boolean doInBackground() throws Throwable {
                updateProgress(R.string.settings_testing_connection);

                previousInstance = Util.getActiveServer(SettingsActivity.this);
                testingConnection = true;
                Util.setActiveServer(SettingsActivity.this, instance);
                try {
                    MusicService musicService = MusicServiceFactory.getMusicService(SettingsActivity.this);
                    musicService.ping(SettingsActivity.this, this);
                    return musicService.isLicenseValid(SettingsActivity.this, null);
                } finally {
                    Util.setActiveServer(SettingsActivity.this, previousInstance);
                    testingConnection = false;
                }
            }

            @Override
            protected void done(Boolean licenseValid) {
                if (licenseValid) {
                    Util.toast(SettingsActivity.this, R.string.settings_testing_ok);
                } else {
                    Util.toast(SettingsActivity.this, R.string.settings_testing_unlicensed);
                }
            }

            @Override
            protected void cancel() {
                super.cancel();
                Util.setActiveServer(SettingsActivity.this, previousInstance);
            }

            @Override
            protected void error(Throwable error) {
                LOG.warn( error.toString(), error);
                new ErrorDialog(SettingsActivity.this, getResources().getString(R.string.settings_connection_failure) +
                        " " + getErrorMessage(error), false);
            }
        };
        task.execute();
    }

    private class ServerSettings {
    	
        private EditTextPreference serverName;
        private EditTextPreference serverUrl;
        private EditTextPreference username;
    	private EditTextPreference password; 
        private CheckBoxPreference enabled;
        private PreferenceScreen screen;

        
        @SuppressWarnings("deprecation")
		private ServerSettings(String instance) {

            screen = (PreferenceScreen) findPreference("server" + instance);
            serverName = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_SERVER_NAME + instance);
            serverUrl = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_SERVER_URL + instance);
            username = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_USERNAME + instance);
            password = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_PASSWORD + instance);
            enabled = (CheckBoxPreference) findPreference(Constants.PREFERENCES_KEY_SERVER_ENABLED + instance);

            serverUrl.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    try {
                        String url = (String) value;
                        new URL(url);
                        if (!url.equals(url.trim()) || url.contains("@") || url.contains("_")) {
                            throw new Exception();
                        }
                    } catch (Exception x) {
                        new ErrorDialog(SettingsActivity.this, R.string.settings_invalid_url, false);
                        return false;
                    }
                    return true;
                }
            });

            username.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    String username = (String) value;
                    if (username == null || !username.equals(username.trim())) {
                        new ErrorDialog(SettingsActivity.this, R.string.settings_invalid_username, false);
                        return false;
                    }
                    return true;
                }
            });
        }

        public void update() {
            serverName.setSummary(serverName.getText());
            serverUrl.setSummary(serverUrl.getText());
            username.setSummary(username.getText());
            screen.setSummary(serverUrl.getText());
            screen.setTitle(serverName.getText());
        }
    }
}