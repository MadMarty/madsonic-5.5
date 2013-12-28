package github.madmarty.madsonic.activity;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.Intent;

import github.madmarty.madsonic.R;
import github.madmarty.madsonic.view.VideoControllerView;

import github.madmarty.madsonic.util.SubtitleConverter.Caption;
import github.madmarty.madsonic.util.SubtitleConverter.FormatSRT;
import github.madmarty.madsonic.util.SubtitleConverter.TimedTextObject;

public class VideoPlayerActivity extends Activity implements SurfaceHolder.Callback, MediaPlayer.OnPreparedListener, VideoControllerView.MediaPlayerControl, OnItemSelectedListener {

	private static final String TAG = "VideoPlayerActivity";
	
	private static final int SUBTITLE_DISPLAY_CHECK = 100;
	
    SurfaceView videoSurface;
    MediaPlayer player = new MediaPlayer();
    VideoControllerView controller;
    String videoSource, currentSource, subtitlesSource;
    TimedTextObject srtSubtitles;
    private Handler subtitleDisplayHandler = new Handler();
    AsyncTask<Void, Void, Void> subtitlesTask;
    Spinner bitrateSpinner;
    Handler bitrateHandler;
    int duration = 0;
    int offset = 0;
    int bitRate = 500;
    boolean playerReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        
        videoSurface = (SurfaceView) findViewById(R.id.videoSurface);
        SurfaceHolder videoHolder = videoSurface.getHolder();
        videoHolder.addCallback(this);
        
        Intent intent = getIntent();
        
        controller = new VideoControllerView(this);
        videoSource = intent.getStringExtra("source").trim();
        bitrateSpinner = (Spinner) findViewById(R.id.bitrate_spinner);
        subtitlesSource = intent.getStringExtra("subtitles");
        duration = intent.getIntExtra("duration", 0);
        bitrateSpinner.setOnItemSelectedListener(this);
        bitrateSpinner.setSelection(1); // Default 500 Kbps
        bitrateSpinner.setVisibility(View.INVISIBLE);
        
        // Load Video
        subtitlesTask = new SubtitleAsyncTask().execute();
        updateUrl(0);
        updateDataSource();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        
        // Hide soft navigation buttons
        videoSurface.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
    
    @Override
    protected void onDestroy() {
    	subtitleDisplayHandler.removeCallbacks(subtitle);
    	player.release();
    	
    	super.onDestroy();
    }
    
    private void updateDataSource()
    {
    	if(player != null && currentSource != null)
    	{
	    	try {
	    		
				player.setDataSource(currentSource);
	    		
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    }
    
    private void updateUrl(int seekTime)
    {
    	StringBuilder builder = new StringBuilder();
    	
    	builder.append(videoSource);
    	builder.append("&maxBitRate=");
    	builder.append(bitRate);
    	builder.append("&timeOffset=");
    	builder.append(seekTime);
    	
    	currentSource = builder.toString().trim();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        controller.show();
        bitrateSpinner.setVisibility(View.VISIBLE);
        
        bitrateHandler = new Handler(); 
        bitrateHandler.postDelayed(new Runnable(){ 
             public void run(){
            	 
            	 // Hide bitrate spinner and soft navigation buttons after 3 seconds
            	 bitrateSpinner.setVisibility(View.INVISIBLE);
            	 videoSurface.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
             }
        }, VideoControllerView.UI_DEFAULT_TIMEOUT);
        
        return false;
    }

    // Implement SurfaceHolder.Callback
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        
    }

    public void surfaceCreated(SurfaceHolder holder) {
    	player.setDisplay(holder);
    	player.setScreenOnWhilePlaying(true);
        player.prepareAsync();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
    	
    }
    // End SurfaceHolder.Callback

    // Implement MediaPlayer.OnPreparedListener
    public void onPrepared(MediaPlayer mp) {
        controller.setMediaPlayer(this);
        controller.setAnchorView((FrameLayout) findViewById(R.id.videoSurfaceContainer));
        controller.setDuration(duration);
        player.start();
        playerReady = true;
    }
    // End MediaPlayer.OnPreparedListener

    // Implement VideoMediaController.MediaPlayerControl
    public boolean canPause() {
        return true;
    }

    public boolean canSeekBackward() {
        return false;
    }

    public boolean canSeekForward() {
        return false;
    }

    public int getBufferPercentage() {
        return 0;
    }

    public int getCurrentPosition() {
        return player.getCurrentPosition() + offset;
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public void pause() {
        player.pause();
    }

    public void seekTo(int i) {
    	offset = i;
        player.reset();
        updateUrl((int)(i * 0.001));
        updateDataSource();
        player.prepareAsync();
    }

    public void start() {
        player.start();
    }

    public boolean isFullScreen() {
        return false;
    }

    public void toggleFullScreen() {
        
    }

	public int getDuration() {
		return duration * 1000;
	}
	
	//
	// Bit Rate Controller
	//
	
	public void onItemSelected(AdapterView<?> parent, View view, 
            int pos, long id) {
        
		bitRate = Integer.parseInt(parent.getItemAtPosition(pos).toString());
		
		if(playerReady)
		{
			offset = player.getCurrentPosition() + offset;
	        updateUrl((int)(offset * 0.001));
	        Log.i(TAG, "Seek Time = " + (offset * 0.001));
	        player.reset();
	        updateDataSource();
	        player.prepareAsync();
		}
    }

    public void onNothingSelected(AdapterView<?> parent) {

    }
    
	//
	// Subtitles Controller
	//
	
	private Runnable subtitle = new Runnable() {
		public void run() {
			
			if (player != null) {
				
				int currentPos = player.getCurrentPosition() + offset;
				Collection<Caption> subtitles =  srtSubtitles.captions.values();
								
				for(Caption caption : subtitles) {
					if (currentPos >= caption.start.getMilliseconds() && currentPos <= caption.end.getMilliseconds()) {
						onTimedText(caption);
						break;
					} else if (currentPos > caption.end.getMilliseconds()) {
						onTimedText(null);
					}
				}
			}
			subtitleDisplayHandler.postDelayed(this, SUBTITLE_DISPLAY_CHECK);
		};
	};
	
	public void onTimedText(Caption text) {
		TextView subtitles = (TextView) findViewById(R.id.txtSubtitles);
		
		if (text == null) {  
			subtitles.setVisibility(View.INVISIBLE);
		    return;
		}
		
		subtitles.setText(Html.fromHtml(text.content));
		subtitles.setVisibility(View.VISIBLE);
	}
	
	public class SubtitleAsyncTask extends AsyncTask<Void, Void, Void> {
		 
		  @Override
		  protected Void doInBackground(Void... params) {
			  
			  if (subtitlesSource != null) {
				  try {
					  URL url = new URL(subtitlesSource);
					  InputStream stream = url.openStream();
					  FormatSRT formatSRT = new FormatSRT();
					  srtSubtitles = formatSRT.parseFile(null, stream);
					  subtitleDisplayHandler.post(subtitle);
					      					  					  
				  } catch (Exception e) {
					  Log.e(getClass().getName(), e.getMessage(), e);
				  }
		    }

		    return null;
		  }
	}
}