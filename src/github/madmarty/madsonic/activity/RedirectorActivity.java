package github.madmarty.madsonic.activity;

import github.madmarty.madsonic.R;
import github.madmarty.madsonic.service.DownloadService;
import github.madmarty.madsonic.util.Constants;
import github.madmarty.madsonic.util.PopupMenuHelper;
import github.madmarty.madsonic.util.Util;

import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

/**
*
* @author Madevil
*/
public class RedirectorActivity extends SubsonicTabActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    
    if ("Madsonic Flawless".equals(theme) || "Madsonic Flawless Fullscreen".equals(theme)) {
        mainBar = findViewById(R.id.button_bar);
        mainBar.setBackgroundResource(R.drawable.menubar_button_normal_green);
    } 
    String theme = Util.getTheme(getBaseContext());
    if ("Madsonic Pink".equals(theme) || "Madsonic Pink Fullscreen".equals(theme)) {
        mainBar = findViewById(R.id.button_bar);
        mainBar.setBackgroundResource(R.drawable.menubar_button_normal_pink);
    } 
    if ("Madsonic Light".equals(theme) || "Madsonic Light Fullscreen".equals(theme)) {
        mainBar = findViewById(R.id.button_bar);
        mainBar.setBackgroundResource(R.drawable.menubar_button_light);
    } 
    
    Util.changeLanguage(getBaseContext());        

    // Button 1: shuffle
    ImageButton shuffleButton = (ImageButton) findViewById(R.id.action_button_0);
    shuffleButton.setVisibility(View.GONE);
    
    // Button 2: search
    ImageButton actionSearchButton = (ImageButton) findViewById(R.id.action_button_1);
    actionSearchButton.setVisibility(View.GONE);	
    
    // Button 3: refresh
    ImageButton refreshButton = (ImageButton) findViewById(R.id.action_button_2);
    refreshButton.setVisibility(View.GONE);
    
	// Button 4: Settings
    final ImageButton actionSettingsButton = (ImageButton)findViewById(R.id.action_button_3);
    actionSettingsButton.setVisibility(View.GONE);
    actionSettingsButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
        	new PopupMenuHelper().showMenu(RedirectorActivity.this, actionSettingsButton, R.menu.common);
        }
    });

    Uri data = getIntent().getData();
    String scheme = data.getScheme(); 				// "madsonic
    String host = data.getHost(); 					// "10.10.1.1:8080"
    List<String> params = data.getPathSegments();	// "all params"
    String method = null;		 					// "GET, SEARCH"
    String object = null; 						    // "id, playlist, artist, album, title, share"
    String value = null; 							// "1234, abba, album, title, share"
   
    try	{
    	method = params.get(0); 					// "GET, SEARCH"
        object = params.get(1); 					// "id, playlist, artist, album, title, share"
        value = params.get(2); 						// "1234, abba, album, title, share"
	} 
    catch(Exception e) {}

    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    int activeServers = settings.getInt(Constants.PREFERENCES_KEY_ACTIVE_SERVERS, 0);
    int foundServer = 0;
    for (int i=0; i<activeServers+1; i++) {
    	String serverUrl = settings.getString(Constants.PREFERENCES_KEY_SERVER_URL + i, null);
    	if (serverUrl != null){
	    	if (serverUrl.contains(host)) {
	    		foundServer=i;
	    	}
    	}
    }
    if (foundServer > 0) {
    	Util.setActiveServer(this, foundServer);
    }
    
    // Check Server host
    if (Util.getRestUrl(this, null).contains(host)) {
    	
        if (method.equalsIgnoreCase("get")) {
        	
	        	if (object.equalsIgnoreCase("id")){
	                Intent intent = new Intent(RedirectorActivity.this, SelectAlbumActivity.class);
	                intent.putExtra(Constants.INTENT_EXTRA_NAME_ID, value);
	                Util.startActivityWithoutTransition(RedirectorActivity.this, intent);
	                
	            } else if (object.equalsIgnoreCase("playlist")) {
	                Intent intent = new Intent(this, SelectAlbumActivity.class);
	                intent.putExtra(Constants.INTENT_EXTRA_NAME_PLAYLIST_ID, value);
	                Util.startActivityWithoutTransition(this, intent);    	
	                
	            } else if (object.equalsIgnoreCase("share")) {
	                Intent intent = new Intent(this, SelectAlbumActivity.class);
	                intent.putExtra(Constants.INTENT_EXTRA_NAME_SHARED, 1);
	                intent.putExtra(Constants.INTENT_EXTRA_NAME_SHARE, value);
	                Util.startActivityWithoutTransition(this, intent);    	
	
	            } else {
	        		Util.toast(this, "WARNING: Methode not implemented!");
	        	}

	    } else if (method.equalsIgnoreCase("search")) {
    	
        if (object.equalsIgnoreCase("all")){
            Intent intent = new Intent(this, SearchActivity.class);
            intent.putExtra(Constants.INTENT_EXTRA_NAME_QUERY, value);
            Util.startActivityWithoutTransition(this, intent);            	   
        }
        else {
    	        Intent intent = new Intent(this, MainActivity.class);
    	        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	        intent.putExtra(Constants.INTENT_EXTRA_NAME_EXIT, true);
    	        Util.startActivityWithoutTransition(this, intent);    	
    	    }
        
    } else {
    	Util.toast(this, "WARNING: Server not found! \n Switch to " + host,false);
    }
        }
    
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

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
}
