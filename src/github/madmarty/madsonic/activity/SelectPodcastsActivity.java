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

 Copyright 2010 (C) Sindre Mehus
 */
package github.madmarty.madsonic.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import github.madmarty.madsonic.R;
import github.madmarty.madsonic.domain.PodcastChannel;
import github.madmarty.madsonic.service.MusicService;
import github.madmarty.madsonic.service.MusicServiceFactory;
import github.madmarty.madsonic.service.OfflineException;
import github.madmarty.madsonic.service.RESTMusicService;
import github.madmarty.madsonic.service.ServerTooOldException;
import github.madmarty.madsonic.util.Constants;
import github.madmarty.madsonic.util.LoadingTask;
import github.madmarty.madsonic.util.PopupMenuHelper;
import github.madmarty.madsonic.util.SilentBackgroundTask;
import github.madmarty.madsonic.util.TabActivityBackgroundTask;
import github.madmarty.madsonic.util.Util;
import github.madmarty.madsonic.view.PodcastChannelAdapter;
import github.madmarty.madsonic.util.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
 
/**
 *
 * @author Scott
 */
public class SelectPodcastsActivity extends SubsonicTabActivity implements AdapterView.OnItemClickListener {
	
    private static final Logger LOG = new Logger(SelectPodcastsActivity.class);
    
	private PodcastChannelAdapter podcastAdapter;
	private ListView podcastListView;
	private View emptyView;
	
	private List<PodcastChannel> channels;
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

        setContentView(R.layout.select_podcasts);
		
		if(bundle != null) {
			channels = (List<PodcastChannel>) bundle.getSerializable(Constants.CHANNEL_LIST);
		}
		
        try {
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
    		
            // Button 1: gone
            ImageButton searchButton = (ImageButton) findViewById(R.id.action_button_1);
            searchButton.setVisibility(View.GONE);

    		// Button 2: refresh
            ImageButton refreshButton = (ImageButton) findViewById(R.id.action_button_2);
    		refreshButton.setImageResource(R.drawable.action_refresh);
            refreshButton.setVisibility(View.VISIBLE);
    		refreshButton.setOnClickListener(new View.OnClickListener() {
    			@Override
    			public void onClick(View view) {
    				refresh(true);
    			}
    		});  
            
            
    		// Button 3: Settings
            final ImageButton actionSettingsButton = (ImageButton)findViewById(R.id.action_button_3);
            actionSettingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                   	new PopupMenuHelper().showMenu(SelectPodcastsActivity.this, actionSettingsButton, R.menu.select_podcasts);
                }
            });   
        } catch (Exception ex) {
   //      Log.e(TAG, ex.getCause().toString());
        }
	
        
		podcastListView = (ListView) findViewById(R.id.select_podcasts_list);
		podcastListView.setOnItemClickListener(this);
		registerForContextMenu(podcastListView);
		emptyView = findViewById(R.id.select_podcasts_empty);

		if(channels == null) {
			refresh(true);
		}
		else {
			podcastListView.setAdapter(podcastAdapter = new PodcastChannelAdapter(getBaseContext(), channels));
			refresh(false);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(Constants.CHANNEL_LIST, (Serializable) channels);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.select_podcasts, menu);
    	super.onCreateOptionsMenu(menu);
    	
    	return true;
    }
	
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.select_podcasts, menu);
	}
	 
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(super.onOptionsItemSelected(item)) {
			return true;
		}
		
		switch (item.getItemId()) {
			case R.id.menu_check:
				refreshPodcasts();
				break;
			case R.id.menu_add_podcast:
				addNewPodcast();
				break;
		}

		return false;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		if(!Util.isOffline(getBaseContext())) {
			
	        MenuInflater inflater = getMenuInflater();
	        inflater.inflate(R.menu.select_podcasts_context, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
		PodcastChannel channel = (PodcastChannel) podcastListView.getItemAtPosition(info.position);

		switch (menuItem.getItemId()) {
			case R.id.podcast_channel_info:
				displayPodcastInfo(channel);
				break;
			case R.id.podcast_channel_delete:
				deletePodcast(channel);
				break;
		}
		
		return true;
	}
	
	protected void refresh(final boolean refresh) {
		setTitle(R.string.button_bar_podcasts);
		podcastListView.setVisibility(View.INVISIBLE);
		emptyView.setVisibility(View.GONE);
		
		TabActivityBackgroundTask<List<PodcastChannel>> task = new TabActivityBackgroundTask<List<PodcastChannel>>(this, refresh) {
			@Override
			protected List<PodcastChannel> doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(getBaseContext());

				channels = new ArrayList<PodcastChannel>();

				try {
					channels = musicService.getPodcastChannels(refresh, getBaseContext(), this);
				} catch (Exception x) {
					LOG.error("Failed to load podcasts", x);
				}

				return channels;
			}

			@Override
			protected void done(List<PodcastChannel> result) {
				emptyView.setVisibility(result == null || result.isEmpty() ? View.VISIBLE : View.GONE);

				if (result != null) {
					podcastListView.setAdapter(podcastAdapter = new PodcastChannelAdapter(getBaseContext(), result));
					podcastListView.setVisibility(View.VISIBLE);
				}
			}
		};
		task.execute();
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		PodcastChannel channel = (PodcastChannel) parent.getItemAtPosition(position);
		
		if("error".equals(channel.getStatus())) {
			Util.toast(getBaseContext(), getBaseContext().getResources().getString(R.string.select_podcasts_invalid_podcast_channel, channel.getErrorMessage() == null ? "error" : channel.getErrorMessage()));
			
		} else if("downloading".equals(channel.getStatus())) {
			Util.toast(getBaseContext(), R.string.select_podcasts_initializing);
		} else {
			
	    	Intent intent = new Intent(this, SelectAlbumActivity.class);
	    	
	        intent.putExtra(Constants.INTENT_EXTRA_NAME_ID, channel.getId());
	        intent.putExtra(Constants.INTENT_EXTRA_NAME_NAME, channel.getName());
	        
	        intent.putExtra(Constants.INTENT_EXTRA_NAME_PODCAST_ID, channel.getId());
	        intent.putExtra(Constants.INTENT_EXTRA_NAME_PODCAST_NAME, channel.getName());
	        intent.putExtra(Constants.INTENT_EXTRA_NAME_PODCAST_DESCRIPTION, channel.getDescription());
       
	    	Util.startActivityWithoutTransition(this, intent);
		}
	}
	
	public void refreshPodcasts() {
		new SilentBackgroundTask<Void>(this) {
			@Override
			protected Void doInBackground() throws Throwable {				
				MusicService musicService = MusicServiceFactory.getMusicService(getBaseContext());
				musicService.refreshPodcasts(getBaseContext(), null);
				return null;
			}

			@Override
			protected void done(Void result) {
				Util.toast(getBaseContext(), R.string.select_podcasts_refreshing);
			}

			@Override
			protected void error(Throwable error) {
				Util.toast(getBaseContext(), getErrorMessage(error), false);
			}
		}.execute();
	}
	
	private void addNewPodcast() {
		View dialogView = getLayoutInflater().inflate(R.layout.create_podcast, null);
		final TextView urlBox = (TextView) dialogView.findViewById(R.id.create_podcast_url);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.menu_add_podcast)
			.setView(dialogView)
			.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					addNewPodcast(urlBox.getText().toString());
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
	
	
	private void addNewPodcast(final String url) {
		new LoadingTask<Void>(this, false) {
			@Override
			protected Void doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(getBaseContext());
				musicService.createPodcastChannel(url, getBaseContext(), null);
				return null;
			}

			@Override
			protected void done(Void result) {
				refresh(false);
			}

			@Override
			protected void error(Throwable error) {
				String msg;
				if (error instanceof OfflineException || error instanceof ServerTooOldException) {
					msg = getErrorMessage(error);
				} else {
					msg = getBaseContext().getResources().getString(R.string.select_podcasts_created_error) + " " + getErrorMessage(error);
				}

				Util.toast(getBaseContext(), msg, false);
			}
		}.execute();
	}
	
	private void displayPodcastInfo(final PodcastChannel channel) {
		String message = ((channel.getName()) == null ? "" : "Title: " + channel.getName()) +
			"\nURL: " + channel.getUrl() + "\nStatus: " + channel.getStatus() +
			((channel.getErrorMessage()) == null ? "" : "\nError Message: " + channel.getErrorMessage()) +
			((channel.getDescription()) == null ? "" : "\nDescription: " + channel.getDescription());
		
		Util.info(this, R.string.main_about, message);
	}
	
	private void deletePodcast(final PodcastChannel channel) {
		
		Util.confirmDialog(this, R.string.common_delete, channel.getName(), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				new LoadingTask<Void>(SelectPodcastsActivity.this, false) {
					@Override
					protected Void doInBackground() throws Throwable {
						MusicService musicService = MusicServiceFactory.getMusicService(SelectPodcastsActivity.this);
						musicService.deletePodcastChannel(channel.getId(), SelectPodcastsActivity.this, null);
						return null;
					}

					@Override
					protected void done(Void result) {
						podcastAdapter.remove(channel);
						podcastAdapter.notifyDataSetChanged();
						Util.toast(SelectPodcastsActivity.this, SelectPodcastsActivity.this.getResources().getString(R.string.select_podcasts_deleted, channel.getName()));
					}

					@Override
					protected void error(Throwable error) {
						String msg;
						if (error instanceof OfflineException || error instanceof ServerTooOldException) {
							msg = getErrorMessage(error);
						} else {
							msg = SelectPodcastsActivity.this.getResources().getString(R.string.select_podcasts_deleted_error, channel.getName()) + " " + getErrorMessage(error);
						}
						Util.toast(SelectPodcastsActivity.this, msg, false);
					}
				}.execute();
			}
		});
	}
	
	@Deprecated
	public static void confirmDialog(Context context, int action, String subject, DialogInterface.OnClickListener onClick) {
		confirmDialog(context, context.getResources().getString(action).toLowerCase(), subject, onClick);
	}
	
	@Deprecated
	public static void confirmDialog(Context context, String action, String subject, DialogInterface.OnClickListener onClick) {
		new AlertDialog.Builder(context)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.common_confirm)
			.setMessage(context.getResources().getString(R.string.common_confirm_message, action, subject))
			.setPositiveButton(R.string.common_ok, onClick)
			.setNegativeButton(R.string.common_cancel, null)
			.show();
	}

}
