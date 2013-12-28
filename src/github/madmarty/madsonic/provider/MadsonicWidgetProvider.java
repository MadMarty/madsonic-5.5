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
package github.madmarty.madsonic.provider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import github.madmarty.madsonic.R;
import github.madmarty.madsonic.activity.DownloadActivity;
import github.madmarty.madsonic.activity.MainActivity;
import github.madmarty.madsonic.audiofx.EqualizerController;
import github.madmarty.madsonic.domain.MusicDirectory;
import github.madmarty.madsonic.service.DownloadService;
import github.madmarty.madsonic.service.DownloadServiceImpl;
import github.madmarty.madsonic.util.FileUtil;
import github.madmarty.madsonic.util.Logger;

/**
 * Simple widget to show currently playing album art along
 * with play/pause and next track buttons.
 * <p/>
 * Based on source code from the stock Android Music app.
 *
 * @author Sindre Mehus
 */
public class MadsonicWidgetProvider extends AppWidgetProvider {

    private static final Logger LOG = new Logger(MadsonicWidgetProvider.class);

	private static MadsonicWidget4x1 instance4x1;
	private static MadsonicWidget4x2 instance4x2;
	private static MadsonicWidget4x4 instance4x4;

	public static synchronized void notifyInstances(Context context, DownloadService service, boolean playing) {

		if(instance4x1 == null) {
			instance4x1 = new MadsonicWidget4x1();
		}
		if(instance4x2 == null) {
			instance4x2 = new MadsonicWidget4x2();
		}
		if(instance4x4 == null) {
			instance4x4 = new MadsonicWidget4x4();
		}

		instance4x1.notifyChange(context, service, playing);
		instance4x2.notifyChange(context, service, playing);
		instance4x4.notifyChange(context, service, playing);
	}

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);
    }
	
	protected int getLayout() {
		return 0;
	}

    /**
     * Initialize given widgets to default state, where we launch Madsonic on default click
     * and hide actions if service not running.
     */
    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        final Resources res = context.getResources();
        final RemoteViews views = new RemoteViews(context.getPackageName(), getLayout());

        views.setTextViewText(R.id.artist, res.getText(R.string.widget_initial_text));
		if(getLayout() != R.layout.appwidget4x1) {
			views.setTextViewText(R.id.album, "");
		}

        linkButtons(context, views, false);
        pushUpdate(context, appWidgetIds, views);
    }

    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager manager = AppWidgetManager.getInstance(context);

        if (manager != null) {
            if (appWidgetIds != null) {
                manager.updateAppWidget(appWidgetIds, views);
            } else {
                manager.updateAppWidget(new ComponentName(context, this.getClass()), views);
            }
        }
    }
    
    /**
     * Handle a change notification coming over from {@link DownloadService}
     */
    public void notifyChange(Context context, DownloadService service, boolean playing) {
        if (hasInstances(context)) {
            performUpdate(context, service, null, playing);
        }
    }

    /**
     * Check against {@link AppWidgetManager} if there are any instances of this widget.
     */
    private boolean hasInstances(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = new int[0];

        if (manager != null) {
            appWidgetIds = manager.getAppWidgetIds(new ComponentName(context, getClass()));
        }

        return (appWidgetIds.length > 0);
    }

    /**
     * Update all active widget instances by pushing changes
     */
    private void performUpdate(Context context, DownloadService service, int[] appWidgetIds, boolean playing) {
        final Resources res = context.getResources();
        final RemoteViews views = new RemoteViews(context.getPackageName(), getLayout());

        MusicDirectory.Entry currentPlaying = service.getCurrentPlaying() == null ? null : service.getCurrentPlaying().getSong();
        String title = currentPlaying == null ? null : currentPlaying.getTitle();
        CharSequence artist = currentPlaying == null ? null : currentPlaying.getArtist();
		CharSequence album = currentPlaying == null ? null : currentPlaying.getAlbum();
        CharSequence errorState = null;

        // Show error message?
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_SHARED) ||
            status.equals(Environment.MEDIA_UNMOUNTED)) {
            errorState = res.getText(R.string.widget_sdcard_busy);
        } else if (status.equals(Environment.MEDIA_REMOVED)) {
            errorState = res.getText(R.string.widget_sdcard_missing);
        } else if (currentPlaying == null) {
            errorState = res.getText(R.string.widget_initial_text);
        }

        if (errorState != null) {
            // Show error state to user
        	views.setTextViewText(R.id.title,null);
            views.setTextViewText(R.id.artist, errorState);
			views.setTextViewText(R.id.album, "");
			if(getLayout() != R.layout.appwidget4x1) {
				views.setImageViewResource(R.id.appwidget_coverart, R.drawable.appwidget_art_default);
			}
        } else {
            // No error, so show normal titles
            views.setTextViewText(R.id.title, title);
            views.setTextViewText(R.id.artist, artist);
			if(getLayout() != R.layout.appwidget4x1) {
				views.setTextViewText(R.id.album, album);
			}
        }

        // Set correct drawable for pause state
        if (playing) {
            views.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_pause);
        } else {
            views.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_play);
        }

        // Set the cover art
        try {
            int size = context.getResources().getDrawable(R.drawable.appwidget_art_default).getIntrinsicHeight();
            Bitmap bitmap = currentPlaying == null ? null : FileUtil.getAlbumArtBitmap(context, currentPlaying, size);

            if (bitmap == null) {
                // Set default cover art
                views.setImageViewResource(R.id.appwidget_coverart, R.drawable.unknown_album);
            } else {
            	
    			if(getLayout() == R.layout.appwidget4x4) {
                    bitmap = getRoundedCornerBitmap(bitmap);
    				
    			}
            	
                views.setImageViewBitmap(R.id.appwidget_coverart, bitmap);
            }
        } catch (Exception x) {
            LOG.error( "Failed to load cover art", x);
            views.setImageViewResource(R.id.appwidget_coverart, R.drawable.unknown_album);
        }

        // Link actions buttons to intents
        linkButtons(context, views, currentPlaying != null);

        pushUpdate(context, appWidgetIds, views);
    }
    
    /**
     * Round the corners of a bitmap for the cover art image
     */
    private static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final float roundPx = 10;

        // Add extra width to the rect so the right side wont be rounded.
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    /**
     * Link up various button actions using {@link PendingIntent}.
     *
     * @param playerActive True if player is active in background, which means
     *                     widget click will launch {@link DownloadActivity},
     *                     otherwise we launch {@link MainActivity}.
     */
    private void linkButtons(Context context, RemoteViews views, boolean playerActive) {
    	
        Intent intent = new Intent(context, playerActive ? DownloadActivity.class : MainActivity.class);

//		Intent intent = new Intent(context, MainActivity.class);
//		if(playerActive) {
//			intent.putExtra(Constants.INTENT_EXTRA_NAME_DOWNLOAD, true);
//			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//		}
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.appwidget_coverart, pendingIntent);
        views.setOnClickPendingIntent(R.id.appwidget_top, pendingIntent);
        
        // Emulate media button clicks.
        intent = new Intent("1");
        intent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_play, pendingIntent);

        intent = new Intent("2");  // Use a unique action name to ensure a different PendingIntent to be created.
        intent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_next, pendingIntent);
        
        intent = new Intent("3");  // Use a unique action name to ensure a different PendingIntent to be created.
        intent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_previous, pendingIntent);
    }
}
