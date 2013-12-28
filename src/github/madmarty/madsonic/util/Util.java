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
package github.madmarty.madsonic.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import github.madmarty.madsonic.R;
import github.madmarty.madsonic.activity.DownloadActivity;
import github.madmarty.madsonic.activity.SubsonicTabActivity;
import github.madmarty.madsonic.domain.MusicDirectory;
import github.madmarty.madsonic.domain.MusicDirectory.Entry;
import github.madmarty.madsonic.domain.PlayerState;
import github.madmarty.madsonic.domain.RepeatMode;
import github.madmarty.madsonic.domain.SearchResult;
import github.madmarty.madsonic.domain.Version;
import github.madmarty.madsonic.provider.MadsonicWidgetProvider;
import github.madmarty.madsonic.receiver.MediaButtonIntentReceiver;
import github.madmarty.madsonic.service.DownloadService;
import github.madmarty.madsonic.service.DownloadServiceImpl;
import github.madmarty.madsonic.service.MusicService;
import github.madmarty.madsonic.service.MusicServiceFactory;
import github.madmarty.madsonic.service.OfflineException;
import github.madmarty.madsonic.service.ServerTooOldException;

import org.apache.http.HttpEntity;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public final class Util {

    private static final Logger LOG = new Logger(Util.class);

    private static final DecimalFormat GIGA_BYTE_FORMAT = new DecimalFormat("0.00 GB");
    private static final DecimalFormat MEGA_BYTE_FORMAT = new DecimalFormat("0.00 MB");
    private static final DecimalFormat KILO_BYTE_FORMAT = new DecimalFormat("0 KB");

    private static DecimalFormat GIGA_BYTE_LOCALIZED_FORMAT = null;
    private static DecimalFormat MEGA_BYTE_LOCALIZED_FORMAT = null;
    private static DecimalFormat KILO_BYTE_LOCALIZED_FORMAT = null;
    private static DecimalFormat BYTE_LOCALIZED_FORMAT = null;

    public static final String EVENT_META_CHANGED	   = "github.madmarty.madsonic.EVENT_META_CHANGED";
    public static final String EVENT_PLAYSTATE_CHANGED = "github.madmarty.madsonic.EVENT_PLAYSTATE_CHANGED";
	
	public static final String AVRCP_PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
	public static final String AVRCP_METADATA_CHANGED  = "com.android.music.metachanged";
	
	private static boolean hasFocus = false;
	private static boolean pauseFocus = false;
	private static boolean lowerFocus = false;

    private static final Map<Integer, Version> SERVER_REST_VERSIONS = new ConcurrentHashMap<Integer, Version>();

    // Used by hexEncode()
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private final static Pair<Integer, Integer> NOTIFICATION_TEXT_COLORS = new Pair<Integer, Integer>();
    private static Toast toast;

    private Util() {
    }

    public static boolean isOffline(Context context) {
        if (context == null)
        	return false;
        else
        return getActiveServer(context) == 0;
    }

    public static boolean isScreenLitOnDownload(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getBoolean(Constants.PREFERENCES_KEY_SCREEN_LIT_ON_DOWNLOAD, false);
    }

    public static RepeatMode getRepeatMode(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return RepeatMode.valueOf(prefs.getString(Constants.PREFERENCES_KEY_REPEAT_MODE, RepeatMode.OFF.name()));
    }

    public static void setRepeatMode(Context context, RepeatMode repeatMode) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.PREFERENCES_KEY_REPEAT_MODE, repeatMode.name());
        editor.commit();
    }

    public static boolean isScrobblingEnabled(Context context) {
        if (isOffline(context)) {
            return false;
        }
        SharedPreferences prefs = getPreferences(context);
        return prefs.getBoolean(Constants.PREFERENCES_KEY_SCROBBLE, false);
    }

    public static void setActiveServer(Context context, int instance) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, instance);
        editor.commit();
    }

    public static int getActiveServer(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1);
    }
    
    public static int getActiveServers(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getInt(Constants.PREFERENCES_KEY_ACTIVE_SERVERS, 0);
    }
 
    public static boolean getServerEnabled(Context context, int instance) {
        if (instance == 0) {
            return true;
        }
        SharedPreferences prefs = getPreferences(context);
        return prefs.getBoolean(Constants.PREFERENCES_KEY_SERVER_ENABLED + instance, true);
    }

    public static boolean isStreamProxyEnabled(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getBoolean(Constants.PREFERENCES_KEY_USE_STREAM_PROXY, true);
    }    
     
    public static void removeInstanceName(Context context, int instance) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        
		editor.putString(Constants.PREFERENCES_KEY_SERVER + instance, null);
		editor.putString(Constants.PREFERENCES_KEY_SERVER_NAME + instance, null);
		editor.putString(Constants.PREFERENCES_KEY_SERVER_URL + instance, null);
		editor.putString(Constants.PREFERENCES_KEY_USERNAME + instance, null);
		editor.putString(Constants.PREFERENCES_KEY_PASSWORD + instance, null);
		editor.putBoolean(Constants.PREFERENCES_KEY_SERVER_ENABLED + instance, true);
		editor.commit();
    }
    
    public static void removeInstanceName(Context context, int instance, int activeInstance) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        
        int newInstance = instance + 1;
        
        String server = prefs.getString(Constants.PREFERENCES_KEY_SERVER + newInstance, null);
        String serverName = prefs.getString(Constants.PREFERENCES_KEY_SERVER_NAME + newInstance, null);
        String serverUrl = prefs.getString(Constants.PREFERENCES_KEY_SERVER_URL + newInstance, null);
        String userName = prefs.getString(Constants.PREFERENCES_KEY_USERNAME + newInstance, null);
        String password = prefs.getString(Constants.PREFERENCES_KEY_PASSWORD + newInstance, null);
        boolean serverEnabled = prefs.getBoolean(Constants.PREFERENCES_KEY_SERVER_ENABLED + newInstance, true);
        
		editor.putString(Constants.PREFERENCES_KEY_SERVER + instance, server);
		editor.putString(Constants.PREFERENCES_KEY_SERVER_NAME + instance, serverName);
		editor.putString(Constants.PREFERENCES_KEY_SERVER_URL + instance, serverUrl);
		editor.putString(Constants.PREFERENCES_KEY_USERNAME + instance, userName);
		editor.putString(Constants.PREFERENCES_KEY_PASSWORD + instance, password);
		editor.putBoolean(Constants.PREFERENCES_KEY_SERVER_ENABLED + instance, serverEnabled);
		
		editor.putString(Constants.PREFERENCES_KEY_SERVER + newInstance, null);
		editor.putString(Constants.PREFERENCES_KEY_SERVER_NAME + newInstance, null);
		editor.putString(Constants.PREFERENCES_KEY_SERVER_URL + newInstance, null);
		editor.putString(Constants.PREFERENCES_KEY_USERNAME + newInstance, null);
		editor.putString(Constants.PREFERENCES_KEY_PASSWORD + newInstance, null);
		editor.putBoolean(Constants.PREFERENCES_KEY_SERVER_ENABLED + newInstance, true);
		editor.commit();
		
		if (instance == activeInstance) {
			Util.setActiveServer(context, 0);
		}
		
		if (newInstance == activeInstance) {
			Util.setActiveServer(context, instance);
		}
	}

	public static String getServerName(Context context) {
		SharedPreferences prefs = getPreferences(context);
		int instance = prefs.getInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1);
        return prefs.getString(Constants.PREFERENCES_KEY_SERVER_NAME + instance, null);
	}
	
//    public static String getServerName(Context context, int instance) {
//        SharedPreferences prefs = getPreferences(context);
//        return prefs.getString(Constants.PREFERENCES_KEY_SERVER_NAME + instance, null);
//    }
	
    public static String getServerName(Context context, int instance) {
        if (instance == 0) {
            return context.getResources().getString(R.string.main_offline);
        }
        SharedPreferences prefs = getPreferences(context);
        return prefs.getString(Constants.PREFERENCES_KEY_SERVER_NAME + instance, null);
    }
      
	public static String getUserName(Context context, int instance) {
		if (instance == 0) {
			return context.getResources().getString(R.string.main_offline);
		}
		SharedPreferences prefs = getPreferences(context);
		return prefs.getString(Constants.PREFERENCES_KEY_USERNAME + instance, null);
	}

//    public static void setServerRestVersion(Context context, Version version) {
//        SERVER_REST_VERSIONS.put(getActiveServer(context), version);
//    }
//
//    public static Version getServerRestVersion(Context context) {
//        return SERVER_REST_VERSIONS.get(getActiveServer(context));
//    }	
	
    public static void setServerRestVersion(Context context, Version version) {
		int instance = getActiveServer(context);
		Version current = SERVER_REST_VERSIONS.get(instance);
		if(current != version) {
			SERVER_REST_VERSIONS.put(instance, version);
			SharedPreferences.Editor editor = getPreferences(context).edit();
			editor.putString(Constants.PREFERENCES_KEY_SERVER_VERSION + instance, version.getVersion());
			editor.commit();
		}
    }

    public static Version getServerRestVersion(Context context) {
		int instance = getActiveServer(context);
        Version version = SERVER_REST_VERSIONS.get(instance);
		if(version == null) {
			SharedPreferences prefs = getPreferences(context);
			String versionString = prefs.getString(Constants.PREFERENCES_KEY_SERVER_VERSION + instance, null);
			if(versionString != null && versionString != "") {
				version = new Version(versionString);
				SERVER_REST_VERSIONS.put(instance, version);
			}
		}
		return version;
    }

    public static void setSelectedMusicFolderId(Context context, String musicFolderId) {
        int instance = getActiveServer(context);
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.PREFERENCES_KEY_MUSIC_FOLDER_ID + instance, musicFolderId);
        editor.commit();
    }

    public static String getSelectedMusicFolderId(Context context) {
        SharedPreferences prefs = getPreferences(context);
        int instance = getActiveServer(context);
        return prefs.getString(Constants.PREFERENCES_KEY_MUSIC_FOLDER_ID + instance, null);
    }

    public static String getTheme(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getString(Constants.PREFERENCES_KEY_THEME, "Madsonic Dark");
    }

    public static int getCoverSize(Context context) {
        SharedPreferences prefs = getPreferences(context);
        int coverSize = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_COVER_SIZE, "128"));
        return coverSize;
    }
    
    
    public static int getMaxBitrate(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return 0;
        }

        boolean wifi = networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        SharedPreferences prefs = getPreferences(context);
        return Integer.parseInt(prefs.getString(wifi ? Constants.PREFERENCES_KEY_MAX_BITRATE_WIFI : Constants.PREFERENCES_KEY_MAX_BITRATE_MOBILE, "0"));
    }
	
	public static int getMaxVideoBitrate(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return 0;
        }

    boolean wifi = networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        SharedPreferences prefs = getPreferences(context);
        return Integer.parseInt(prefs.getString(wifi ? Constants.PREFERENCES_KEY_MAX_VIDEO_BITRATE_WIFI : Constants.PREFERENCES_KEY_MAX_VIDEO_BITRATE_MOBILE, "0"));
    }

    public static boolean shouldDisplayBitrateWithArtist(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getBoolean(Constants.PREFERENCES_KEY_DISPLAY_BITRATE_WITH_ARTIST, true);
    }
    
	
	public static int getDefaultVideoplayer(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_DEFAULT_VIDEOPLAYER, "1"));
	}
	
    public static int getPreloadCount(Context context) {
        SharedPreferences prefs = getPreferences(context);
        int preloadCount = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_PRELOAD_COUNT, "-1"));
        return preloadCount == -1 ? Integer.MAX_VALUE : preloadCount;
    }

    public static VideoPlayerType getVideoPlayerType(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return VideoPlayerType.forKey(prefs.getString(Constants.PREFERENCES_KEY_VIDEO_PLAYER, VideoPlayerType.MX.getKey()));
    }

    public static int getCacheSizeMB(Context context) {
        SharedPreferences prefs = getPreferences(context);
        int cacheSize = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_CACHE_SIZE, "-1"));
        return cacheSize == -1 ? Integer.MAX_VALUE : cacheSize;
    }

    public static String getRestUrl(Context context, String method) {
        StringBuilder builder = new StringBuilder();

        SharedPreferences prefs = getPreferences(context);

        int instance = prefs.getInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1);
        return getRestUrl(context, method, prefs, instance);
    }
    public static String getRestUrl(Context context, String method, int instance) {
    	SharedPreferences prefs = getPreferences(context);
    	return getRestUrl(context, method, prefs, instance);
    }
    public static String getRestUrl(Context context, String method, SharedPreferences prefs, int instance) {
        StringBuilder builder = new StringBuilder();
        
        String serverUrl = prefs.getString(Constants.PREFERENCES_KEY_SERVER_URL + instance, null);
        String username = prefs.getString(Constants.PREFERENCES_KEY_USERNAME + instance, null);
        String password = prefs.getString(Constants.PREFERENCES_KEY_PASSWORD + instance, null);

        // Slightly obfuscate password
        password = "enc:" + Util.utf8HexEncode(password);

        builder.append(serverUrl);
        if (builder.charAt(builder.length() - 1) != '/') {
            builder.append("/");
        }
        builder.append("rest/").append(method).append(".view");
        builder.append("?u=").append(username);
        builder.append("&p=").append(password);
        builder.append("&v=").append(Constants.REST_PROTOCOL_VERSION);
        builder.append("&c=").append(Constants.REST_CLIENT_ID);

        return builder.toString();
    }

    public static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(Constants.PREFERENCES_FILE_NAME, 0);
    }

    public static String getContentType(HttpEntity entity) {
        if (entity == null || entity.getContentType() == null) {
            return null;
        }
        return entity.getContentType().getValue();
    }

    /**
     * Get the contents of an <code>InputStream</code> as a <code>byte[]</code>.
     * <p/>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     *
     * @param input the <code>InputStream</code> to read from
     * @return the requested byte array
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs
     */
    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output);
        return output.toByteArray();
    }

    public static long copy(InputStream input, OutputStream output)
            throws IOException {
        byte[] buffer = new byte[1024 * 4];
        long count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static void atomicCopy(File from, File to) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;
        File tmp = null;
        try {
            tmp = new File(to.getPath() + ".tmp");
            in = new FileInputStream(from);
            out = new FileOutputStream(tmp);
            in.getChannel().transferTo(0, from.length(), out.getChannel());
            out.close();
            if (!tmp.renameTo(to)) {
                throw new IOException("Failed to rename " + tmp + " to " + to);
            }
            LOG.info("Copied " + from + " to " + to);
        } catch (IOException x) {
            close(out);
            delete(to);
            throw x;
        } finally {
            close(in);
            close(out);
            delete(tmp);
        }
    }
    
    public static void renameFile(File from, File to) throws IOException {
		if(from.renameTo(to)) {
			LOG.info( "Renamed " + from + " to " + to);
		} else {
			atomicCopy(from, to);
		}
	}    

    public static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Throwable x) {
            // Ignored
        }
    }

    public static boolean delete(File file) {
        if (file != null && file.exists()) {
            if (!file.delete()) {
                LOG.warn("Failed to delete file " + file);
                return false;
            }
            LOG.info("Deleted file " + file);
        }
        return true;
    }
    
    public static boolean recursiveDelete(File dir) {
		if (dir != null && dir.exists()) {
			for(File file: dir.listFiles()) {
				if(file.isDirectory()) {
					if(!recursiveDelete(file)) {
						return false;
					}
				} else if(file.exists()) {
					if(!file.delete()) {
						return false;
					}
				}
			}
			return dir.delete();
		}
		return false;
	}

    public static void toast(Context context, int messageId) {
        toast(context, messageId, true);
    }

    public static void toast(Context context, int messageId, boolean shortDuration, Object... formatArgs) {
        toast(context, context.getString(messageId, formatArgs), shortDuration);
    }

    public static void toast(Context context, String message) {
        toast(context, message, true);
    }

    public static void toast(Context context, String message, boolean shortDuration) {
        if (toast == null) {
            toast = Toast.makeText(context, message, shortDuration ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
        } else {
            toast.setText(message);
            toast.setDuration(shortDuration ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
        }
        toast.show();
    }

    /**
     * Converts a byte-count to a formatted string suitable for display to the user.
     * For instance:
     * <ul>
     * <li><code>format(918)</code> returns <em>"918 B"</em>.</li>
     * <li><code>format(98765)</code> returns <em>"96 KB"</em>.</li>
     * <li><code>format(1238476)</code> returns <em>"1.2 MB"</em>.</li>
     * </ul>
     * This method assumes that 1 KB is 1024 bytes.
     * To get a localized string, please use formatLocalizedBytes instead.
     *
     * @param byteCount The number of bytes.
     * @return The formatted string.
     */
    public static synchronized String formatBytes(long byteCount) {

        // More than 1 GB?
        if (byteCount >= 1024 * 1024 * 1024) {
            NumberFormat gigaByteFormat = GIGA_BYTE_FORMAT;
            return gigaByteFormat.format((double) byteCount / (1024 * 1024 * 1024));
        }

        // More than 1 MB?
        if (byteCount >= 1024 * 1024) {
            NumberFormat megaByteFormat = MEGA_BYTE_FORMAT;
            return megaByteFormat.format((double) byteCount / (1024 * 1024));
        }

        // More than 1 KB?
        if (byteCount >= 1024) {
            NumberFormat kiloByteFormat = KILO_BYTE_FORMAT;
            return kiloByteFormat.format((double) byteCount / 1024);
        }

        return byteCount + " B";
    }

    /**
     * Converts a byte-count to a formatted string suitable for display to the user.
     * For instance:
     * <ul>
     * <li><code>format(918)</code> returns <em>"918 B"</em>.</li>
     * <li><code>format(98765)</code> returns <em>"96 KB"</em>.</li>
     * <li><code>format(1238476)</code> returns <em>"1.2 MB"</em>.</li>
     * </ul>
     * This method assumes that 1 KB is 1024 bytes.
     * This version of the method returns a localized string.
     *
     * @param byteCount The number of bytes.
     * @return The formatted string.
     */
    public static synchronized String formatLocalizedBytes(long byteCount, Context context) {

        // More than 1 GB?
        if (byteCount >= 1024 * 1024 * 1024) {
            if (GIGA_BYTE_LOCALIZED_FORMAT == null) {
                GIGA_BYTE_LOCALIZED_FORMAT = new DecimalFormat(context.getResources().getString(R.string.util_bytes_format_gigabyte));
            }

            return GIGA_BYTE_LOCALIZED_FORMAT.format((double) byteCount / (1024 * 1024 * 1024));
        }

        // More than 1 MB?
        if (byteCount >= 1024 * 1024) {
            if (MEGA_BYTE_LOCALIZED_FORMAT == null) {
                MEGA_BYTE_LOCALIZED_FORMAT = new DecimalFormat(context.getResources().getString(R.string.util_bytes_format_megabyte));
            }

            return MEGA_BYTE_LOCALIZED_FORMAT.format((double) byteCount / (1024 * 1024));
        }

        // More than 1 KB?
        if (byteCount >= 1024) {
            if (KILO_BYTE_LOCALIZED_FORMAT == null) {
                KILO_BYTE_LOCALIZED_FORMAT = new DecimalFormat(context.getResources().getString(R.string.util_bytes_format_kilobyte));
            }

            return KILO_BYTE_LOCALIZED_FORMAT.format((double) byteCount / 1024);
        }

        if (BYTE_LOCALIZED_FORMAT == null) {
            BYTE_LOCALIZED_FORMAT = new DecimalFormat(context.getResources().getString(R.string.util_bytes_format_byte));
        }

        return BYTE_LOCALIZED_FORMAT.format((double) byteCount);
    }

    public static String formatDuration(Integer seconds) {
        if (seconds == null) {
            return null;
        }

		int hours = seconds / 3600;
        int minutes = (seconds / 60) % 60;
        int secs = seconds % 60;

        StringBuilder builder = new StringBuilder(7);
		if(hours > 0) {
			builder.append(hours).append(":");
			if(minutes < 10) {
				builder.append("0");
			}
		}
        builder.append(minutes).append(":");
        if (secs < 10) {
            builder.append("0");
        }
        builder.append(secs);
        return builder.toString();
    }

    public static boolean equals(Object object1, Object object2) {
        if (object1 == object2) {
            return true;
        }
        if (object1 == null || object2 == null) {
            return false;
        }
        return object1.equals(object2);

    }

    /**
     * Encodes the given string by using the hexadecimal representation of its UTF-8 bytes.
     *
     * @param s The string to encode.
     * @return The encoded string.
     */
    public static String utf8HexEncode(String s) {
        if (s == null) {
            return null;
        }
        byte[] utf8;
        try {
            utf8 = s.getBytes(Constants.UTF_8);
        } catch (UnsupportedEncodingException x) {
            throw new RuntimeException(x);
        }
        return hexEncode(utf8);
    }

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal values of each byte in order.
     * The returned array will be double the length of the passed array, as it takes two characters to represent any
     * given byte.
     *
     * @param data Bytes to convert to hexadecimal characters.
     * @return A string containing hexadecimal characters.
     */
    public static String hexEncode(byte[] data) {
        int length = data.length;
        char[] out = new char[length << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < length; i++) {
            out[j++] = HEX_DIGITS[(0xF0 & data[i]) >>> 4];
            out[j++] = HEX_DIGITS[0x0F & data[i]];
        }
        return new String(out);
    }

    /**
     * Calculates the MD5 digest and returns the value as a 32 character hex string.
     *
     * @param s Data to digest.
     * @return MD5 digest as a hex string.
     */
    public static String md5Hex(String s) {
        if (s == null) {
            return null;
        }

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return hexEncode(md5.digest(s.getBytes(Constants.UTF_8)));
        } catch (Exception x) {
            throw new RuntimeException(x.getMessage(), x);
        }
    }

    public static String decrypt(String s) {
        if (s == null) {
            return null;
        }
        if (!s.startsWith("enc:")) {
            return s;
        }
        try {
            //return utf8HexDecode(s.substring(4));
            return hexToString(s.substring(4));
            
        } catch (Exception e) {
            return s;
        }
    }
    
    public static String hexToString(final String str) {
    	 return new String(new BigInteger(str, 16).toByteArray());
    	}
    
//    public static String utf8HexDecode(String s) throws Exception {
//        if (s == null) {
//            return null;
//        }
//        return new String(Hex.decodeHex(s.toCharArray()), ENCODING_UTF8);
//    }    
    
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean connected = networkInfo != null && networkInfo.isConnected();

        boolean wifiConnected = connected && networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        boolean wifiRequired = isWifiRequiredForDownload(context);

        return connected && (!wifiRequired || wifiConnected);
    }

    public static boolean isExternalStoragePresent() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    private static boolean isWifiRequiredForDownload(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getBoolean(Constants.PREFERENCES_KEY_WIFI_REQUIRED_FOR_DOWNLOAD, false);
    }

    public static void info(Context context, int titleId, int messageId) {
        showDialog(context, android.R.drawable.ic_dialog_info, titleId, messageId);
    }
	public static void info(Context context, int titleId, String message) {
		showDialog(context, android.R.drawable.ic_dialog_info, titleId, message);
	}

    private static void showDialog(Context context, int icon, int titleId, int messageId) {
        new AlertDialog.Builder(context)
                .setIcon(icon)
                .setTitle(titleId)
                .setMessage(messageId)
                .setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
	private static void showDialog(Context context, int icon, int titleId, String message) {
        new AlertDialog.Builder(context)
                .setIcon(icon)
                .setTitle(titleId)
                .setMessage(message)
                .setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private static void setupViews(RemoteViews rv, Context context, MusicDirectory.Entry song, boolean playing){
    
     // Use the same text for the ticker and the expanded notification
        String title = song.getTitle();
        String arist = song.getArtist();
        String album = song.getAlbum();
    
     // Set the album art.
        try {
            int size = context.getResources().getDrawable(R.drawable.unknown_album).getIntrinsicHeight();
            Bitmap bitmap = FileUtil.getAlbumArtBitmap(context, song, size);
            if (bitmap == null) {
             // set default album art
             rv.setImageViewResource(R.id.notification_image, R.drawable.unknown_album);
            } else {
             rv.setImageViewBitmap(R.id.notification_image, bitmap);
            }
        } catch (Exception x) {
            LOG.warn( "Failed to get notification cover art", x);
            rv.setImageViewResource(R.id.notification_image, R.drawable.unknown_album);
        }
        
        rv.setImageViewResource(R.id.control_starred, song.isStarred() ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);        
     
        
        // set the text for the notifications
        rv.setTextViewText(R.id.notification_title, title);
        rv.setTextViewText(R.id.notification_artist, arist);
        rv.setTextViewText(R.id.notification_album, album);
        
        Pair<Integer, Integer> colors = getNotificationTextColors(context);
        if (colors.getFirst() != null) {
            rv.setTextColor(R.id.notification_title, colors.getFirst());
        }
        if (colors.getSecond() != null) {
            rv.setTextColor(R.id.notification_artist, colors.getSecond());
        }
		
		if(!playing) {
			rv.setImageViewResource(R.id.control_pause, R.drawable.notification_play);
			rv.setImageViewResource(R.id.control_previous, R.drawable.notification_stop);
		}
        
        // Create actions for media buttons
        PendingIntent pendingIntent;
		if(playing) {
			Intent prevIntent = new Intent("KEYCODE_MEDIA_PREVIOUS");
			prevIntent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
			prevIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
			pendingIntent = PendingIntent.getService(context, 0, prevIntent, 0);
			rv.setOnClickPendingIntent(R.id.control_previous, pendingIntent);
		} else {
			Intent prevIntent = new Intent("KEYCODE_MEDIA_STOP");
			prevIntent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
			prevIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP));
			pendingIntent = PendingIntent.getService(context, 0, prevIntent, 0);
			rv.setOnClickPendingIntent(R.id.control_previous, pendingIntent);
		}
        
        Intent starredIntent = new Intent("KEYCODE_MEDIA_STARRED");
        starredIntent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
        starredIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_STAR));
        pendingIntent = PendingIntent.getService(context, 0, starredIntent, 0);
        rv.setOnClickPendingIntent(R.id.control_starred, pendingIntent);
		
        Intent pauseIntent = new Intent("KEYCODE_MEDIA_PLAY_PAUSE");
        pauseIntent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
        pauseIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        pendingIntent = PendingIntent.getService(context, 0, pauseIntent, 0);
        rv.setOnClickPendingIntent(R.id.control_pause, pendingIntent);
        
        Intent nextIntent = new Intent("KEYCODE_MEDIA_NEXT");
        nextIntent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
        nextIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
        pendingIntent = PendingIntent.getService(context, 0, nextIntent, 0);
        rv.setOnClickPendingIntent(R.id.control_next, pendingIntent);
    }

    private static boolean useSimpleNotification() {
        return Build.VERSION.SDK_INT < 11;
    }    

    private static Notification createSimpleNotification(Context context, MusicDirectory.Entry song) {
        Bitmap albumArt;
        try {
            albumArt = FileUtil.getAlbumArtBitmap(context, song, (int) Util.convertDpToPixel(64.0F, context));
            if (albumArt == null) {
                albumArt = Util.decodeBitmap(context, R.drawable.unknown_album);
            }
        } catch (Exception x) {
            LOG.warn("Failed to get notification cover art", x);
            albumArt = Util.decodeBitmap(context, R.drawable.unknown_album);
        }
        Intent notificationIntent = new Intent(context, DownloadActivity.class);
        return new NotificationCompat.Builder(context).setOngoing(true)
                .setSmallIcon(R.drawable.stat_notify_playing)
                .setContentTitle(song.getTitle())
                .setContentText(song.getArtist())
                .setContentIntent(PendingIntent.getActivity(context, 0, notificationIntent, 0))
                .setLargeIcon(albumArt)
                .build();
    }

    private static Notification createCustomNotification(Context context, MusicDirectory.Entry song, boolean playing) {

        Bitmap albumArt;
//        try {
//            albumArt = FileUtil.getUnscaledAlbumArtBitmap(context, song);
//            if (albumArt == null) {
//                albumArt = Util.decodeBitmap(context, R.drawable.unknown_album_large);
//            }
//        } catch (Exception x) {
//            LOG.warn("Failed to get notification cover art", x);
//            albumArt = Util.decodeBitmap(context, R.drawable.unknown_album_large);
//        }
//
//        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification);
//        contentView.setTextViewText(R.id.notification_title, song.getTitle());
//        contentView.setTextViewText(R.id.notification_artist, song.getArtist());
//        contentView.setImageViewBitmap(R.id.notification_image, albumArt);
//        contentView.setImageViewResource(R.id.notification_playpause, playing ? R.drawable.media_pause : R.drawable.media_start);
//
//        Intent intent = new Intent("1");
//        intent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
//        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
//        contentView.setOnClickPendingIntent(R.id.notification_playpause, PendingIntent.getService(context, 0, intent, 0));
//
//        intent = new Intent("2");
//        intent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
//        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
//        contentView.setOnClickPendingIntent(R.id.notification_next, PendingIntent.getService(context, 0, intent, 0));
//
//        intent = new Intent("4");
//        intent.setComponent(new ComponentName(context, DownloadServiceImpl.class));
//        intent.putExtra(Constants.INTENT_EXTRA_NAME_HIDE_NOTIFICATION, true);
//        contentView.setOnClickPendingIntent(R.id.notification_close, PendingIntent.getService(context, 0, intent, 0));

        Intent notificationIntent = new Intent(context, DownloadActivity.class);

        Notification notification = new NotificationCompat.Builder(context)
                .setOngoing(true)
                .setSmallIcon(R.drawable.stat_notify_playing)
//                .setContent(contentView)
                .setContentIntent(PendingIntent.getActivity(context, 0, notificationIntent, 0))
                .build();
        if (Build.VERSION.SDK_INT >= 16) {
//            notification.bigContentView = createBigContentView(context, song, albumArt, playing);
        }
        return notification;
    }    
    
	public static void showPlayingNotification(final Context context, final DownloadServiceImpl downloadService, Handler handler, MusicDirectory.Entry song) {
		 
		// Set the icon, scrolling text and timestamp
		final Notification notification = new Notification(R.drawable.stat_notify, song.getTitle(), System.currentTimeMillis());
        notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

		boolean playing = downloadService.getPlayerState() == PlayerState.STARTED;
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.JELLY_BEAN){
			RemoteViews expandedContentView = new RemoteViews(context.getPackageName(), R.layout.notification_expanded);
            setupViews(expandedContentView,context,song, playing);
            notification.bigContentView = expandedContentView;
        }
        
        RemoteViews smallContentView = new RemoteViews(context.getPackageName(), R.layout.notification);
        setupViews(smallContentView, context, song, playing);
        notification.contentView = smallContentView;
        
        Intent notificationIntent = new Intent(context, DownloadActivity.class);
        notification.contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        
		handler.post(new Runnable() {
			@Override
			public void run() {
				downloadService.startForeground(Constants.NOTIFICATION_ID_PLAYING, notification);
			}
		});

        // Update widget
        MadsonicWidgetProvider.notifyInstances(context, downloadService, true);
    }
        
    
    public static void updateNotification(final Context context, final DownloadServiceImpl downloadService,
            Handler handler, MusicDirectory.Entry song, boolean playing) {

        // On older platforms, show a notification without buttons.
        if (useSimpleNotification()) {
            updateSimpleNotification(context, downloadService, handler, song, playing);
        } else {
            updateCustomNotification(context, downloadService, handler, song, playing);
        }

        // Update widget
        MadsonicWidgetProvider.notifyInstances(context, downloadService, playing);
    }

    private static void updateSimpleNotification(Context context, final DownloadServiceImpl downloadService, Handler handler,
            MusicDirectory.Entry song, boolean playing) {

        if (song == null || !playing) {
        	hidePlayingNotification(context, downloadService, handler);
        } else {
            final Notification notification = createSimpleNotification(context, song);

            // Send the notification and put the service in the foreground.
            handler.post(new Runnable() {
                @Override
                public void run() {
                    downloadService.startForeground(Constants.NOTIFICATION_ID_PLAYING, notification);
                }
            });
        }
    }
    
    private static void updateCustomNotification(Context context, final DownloadServiceImpl downloadService,
            Handler handler, MusicDirectory.Entry song, boolean playing) {

        if (song == null) {
        	hidePlayingNotification(context, downloadService, handler);

//        } else if (!isNotificationHiddenByUser(context)) {
            final Notification notification = createCustomNotification(context, song, playing);

            // Send the notification and put the service in the foreground.
            handler.post(new Runnable() {
                @Override
                public void run() {
                    downloadService.startForeground(Constants.NOTIFICATION_ID_PLAYING, notification);
                }
            });
        }
    }
    
    public static void hidePlayingNotification(final Context context, final DownloadServiceImpl downloadService, Handler handler) {
		// Remove notification and remove the service from the foreground
		handler.post(new Runnable() {
			@Override
			public void run() {
				downloadService.stopForeground(true);
			}
		}); 

        // Update widget
        MadsonicWidgetProvider.notifyInstances(context, downloadService, false);
    }

    public static boolean isPackageInstalled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException x) {
            LOG.warn( "Interrupted from sleep.", x);
        }
    }

    public static void startActivityWithoutTransition(Activity currentActivity, Class<? extends Activity> newActivitiy) {
        startActivityWithoutTransition(currentActivity, new Intent(currentActivity, newActivitiy));
    }

    public static void startActivityWithoutTransition(Activity currentActivity, Intent intent) {
        currentActivity.startActivity(intent);
        disablePendingTransition(currentActivity);
    }

    public static void disablePendingTransition(Activity activity) {

        // Activity.overridePendingTransition() was introduced in Android 2.0.  Use reflection to maintain
        // compatibility with 1.5.
        try {
            Method method = Activity.class.getMethod("overridePendingTransition", int.class, int.class);
            method.invoke(activity, 0, 0);
        } catch (Throwable x) {
            // Ignored
        }
    }

    @SuppressWarnings("deprecation")
	public static Drawable createDrawableFromBitmap(Context context, Bitmap bitmap) {
        // BitmapDrawable(Resources, Bitmap) was introduced in Android 1.6.  Use reflection to maintain
        // compatibility with 1.5.
        try {
            Constructor<BitmapDrawable> constructor = BitmapDrawable.class.getConstructor(Resources.class, Bitmap.class);
            return constructor.newInstance(context.getResources(), bitmap);
        } catch (Throwable x) {
            return new BitmapDrawable(bitmap);
        }
    }

    public static void registerMediaButtonEventReceiver(Context context) {

        // Only do it if enabled in the settings.
        SharedPreferences prefs = getPreferences(context);
        boolean enabled = prefs.getBoolean(Constants.PREFERENCES_KEY_MEDIA_BUTTONS, true);

        if (enabled) {

            // AudioManager.registerMediaButtonEventReceiver() was introduced in Android 2.2.
            // Use reflection to maintain compatibility with 1.5.
            try {
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                ComponentName componentName = new ComponentName(context.getPackageName(), MediaButtonIntentReceiver.class.getName());
                Method method = AudioManager.class.getMethod("registerMediaButtonEventReceiver", ComponentName.class);
                method.invoke(audioManager, componentName);
            } catch (Throwable x) {
                // Ignored.
            }
        }
    }

    public static void unregisterMediaButtonEventReceiver(Context context) {
        // AudioManager.unregisterMediaButtonEventReceiver() was introduced in Android 2.2.
        // Use reflection to maintain compatibility with 1.5.
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            ComponentName componentName = new ComponentName(context.getPackageName(), MediaButtonIntentReceiver.class.getName());
            Method method = AudioManager.class.getMethod("unregisterMediaButtonEventReceiver", ComponentName.class);
            method.invoke(audioManager, componentName);
        } catch (Throwable x) {
            // Ignored.
        }
    }
    
    @TargetApi(8)
	public static void requestAudioFocus(final Context context) {
    	if (Build.VERSION.SDK_INT >= 8 && !hasFocus) {
    		final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
			hasFocus = true;
    		audioManager.requestAudioFocus(new OnAudioFocusChangeListener() {
				public void onAudioFocusChange(int focusChange) {
					DownloadServiceImpl downloadService = (DownloadServiceImpl)context;
					if((focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) && !downloadService.isJukeboxEnabled()) {
						if(downloadService.getPlayerState() == PlayerState.STARTED) {							
							SharedPreferences prefs = getPreferences(context);
							int lossPref = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_AUDIO_FOCUS, "1"));
							if(lossPref == 2 || (lossPref == 1 && focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)) {
								lowerFocus = true;
								downloadService.setVolume(0.1f);
							} else if(lossPref == 0 || (lossPref == 1 && focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)) {
								pauseFocus = true;
								downloadService.pause();
							}
						}
					} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
						if(pauseFocus) {
							pauseFocus = false;
							downloadService.start();
						} else if(lowerFocus) {
							lowerFocus = false;
							downloadService.setVolume(1.0f);
						}
					} else if(focusChange == AudioManager.AUDIOFOCUS_LOSS && !downloadService.isJukeboxEnabled()) {
						hasFocus = false;
						downloadService.pause();
						audioManager.abandonAudioFocus(this);
					}
				}
			}, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    	}
    }

    
    public static MusicDirectory getSongsFromSearchResult(SearchResult searchResult) {
    	MusicDirectory musicDirectory = new MusicDirectory();
    	
    	for (Entry entry : searchResult.getSongs()) {
    		musicDirectory.addChild(entry);	
    	}
    	
    	return musicDirectory;
    }

    /**
     * <p>Broadcasts the given song info as the new song being played.</p>
     */
    public static void broadcastNewTrackInfo(Context context, MusicDirectory.Entry song) {
		DownloadService downloadService = (DownloadServiceImpl)context;
        Intent intent = new Intent(EVENT_META_CHANGED);
		Intent avrcpIntent = new Intent(AVRCP_METADATA_CHANGED);

        if (song != null) {
            intent.putExtra("title", song.getTitle());
            intent.putExtra("artist", song.getArtist());
            intent.putExtra("album", song.getAlbum());

            File albumArtFile = FileUtil.getAlbumArtFile(context, song);
            intent.putExtra("coverart", albumArtFile.getAbsolutePath());
			
			avrcpIntent.putExtra("playing", true);
			avrcpIntent.putExtra("track", song.getTitle());
			avrcpIntent.putExtra("artist", song.getArtist());
			avrcpIntent.putExtra("album", song.getAlbum());
			avrcpIntent.putExtra("ListSize",(long) downloadService.getSongs().size());
			avrcpIntent.putExtra("id", (long) downloadService.getCurrentPlayingIndex()+1);
			avrcpIntent.putExtra("duration", (long) downloadService.getPlayerDuration());
			avrcpIntent.putExtra("position", (long) downloadService.getPlayerPosition());
			avrcpIntent.putExtra("coverart", albumArtFile.getAbsolutePath());
        } else {
            intent.putExtra("title", "");
            intent.putExtra("artist", "");
            intent.putExtra("album", "");
            intent.putExtra("coverart", "");
			
			avrcpIntent.putExtra("playing", false);
			avrcpIntent.putExtra("track", "");
			avrcpIntent.putExtra("artist", "");
			avrcpIntent.putExtra("album", "");
			avrcpIntent.putExtra("ListSize",(long)0);
			avrcpIntent.putExtra("id", (long) 0);
			avrcpIntent.putExtra("duration", (long )0);
			avrcpIntent.putExtra("position", (long) 0);
			avrcpIntent.putExtra("coverart", "");
        }

        context.sendBroadcast(intent);
		context.sendBroadcast(avrcpIntent);
    }

    /**
     * <p>Broadcasts the given player state as the one being set.</p>
     */
    public static void broadcastPlaybackStatusChange(Context context, PlayerState state) {
        Intent intent = new Intent(EVENT_PLAYSTATE_CHANGED);
		Intent avrcpIntent = new Intent(AVRCP_PLAYSTATE_CHANGED);

        switch (state) {
            case STARTED:
                intent.putExtra("state", "play");
				avrcpIntent.putExtra("playing", true);
                break;
            case STOPPED:
                intent.putExtra("state", "stop");
				avrcpIntent.putExtra("playing", false);
                break;
            case PAUSED:
                intent.putExtra("state", "pause");
				avrcpIntent.putExtra("playing", false);
                break;
            case COMPLETED:
                intent.putExtra("state", "complete");
				avrcpIntent.putExtra("playing", false);
                break;
            default:
                return; // No need to broadcast.
        }

        context.sendBroadcast(intent);
		context.sendBroadcast(avrcpIntent);
    }

    /**
     * Resolves the default text color for notifications.
     *
     * Based on http://stackoverflow.com/questions/4867338/custom-notification-layouts-and-text-colors/7320604#7320604
     */
    @SuppressWarnings("deprecation")
	private static Pair<Integer, Integer> getNotificationTextColors(Context context) {
        if (NOTIFICATION_TEXT_COLORS.getFirst() == null && NOTIFICATION_TEXT_COLORS.getSecond() == null) {
            try {
                Notification notification = new Notification();
                String title = "title";
                String content = "content";
                notification.setLatestEventInfo(context, title, content, null);
                LinearLayout group = new LinearLayout(context);
                ViewGroup event = (ViewGroup) notification.contentView.apply(context, group);
                findNotificationTextColors(event, title, content);
                group.removeAllViews();
            } catch (Exception x) {
                LOG.warn( "Failed to resolve notification text colors.", x);
            }
        }
        return NOTIFICATION_TEXT_COLORS;
    }

    private static void findNotificationTextColors(ViewGroup group, String title, String content) {
        for (int i = 0; i < group.getChildCount(); i++) {
            if (group.getChildAt(i) instanceof TextView) {
                TextView textView = (TextView) group.getChildAt(i);
                String text = textView.getText().toString();
                if (title.equals(text)) {
                    NOTIFICATION_TEXT_COLORS.setFirst(textView.getTextColors().getDefaultColor());
                }
                else if (content.equals(text)) {
                    NOTIFICATION_TEXT_COLORS.setSecond(textView.getTextColors().getDefaultColor());
                }
            }
            else if (group.getChildAt(i) instanceof ViewGroup)
                findNotificationTextColors((ViewGroup) group.getChildAt(i), title, content);
        }
    }

    public static void changeLanguage(Context context) {

    	Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();

        android.content.res.Configuration conf = res.getConfiguration();
        
        SharedPreferences prefs = getPreferences(context);
        String appLanguage = prefs.getString(Constants.PREFERENCES_KEY_LANGUAGE, "us");

        if ("auto".equalsIgnoreCase(appLanguage)) {
            conf.locale = new Locale("en");
            res.updateConfiguration(conf, dm);
        } else {
	        conf.locale = new Locale(appLanguage);
	        res.updateConfiguration(conf, dm);
        }
    }
    
    
    public static int getDefaultAlbums(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_DEFAULT_ALBUMS, "5"));
    }
    
    public static int getMaxAlbums(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_MAX_ALBUMS, "20"));
    }

    public static int getDefaultSongs(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_DEFAULT_SONGS, "10"));
    }
    
    public static int getMaxSongs(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_MAX_SONGS, "20"));
    }
    
    public static int getMaxArtists(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_MAX_ARTISTS, "20"));
    }
    
    public static int getDefaultArtists(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_DEFAULT_ARTISTS, "3"));
    }

    public static int getNetworkTimeout(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_NETWORK_TIMEOUT, "15000"));
    }	
	
    public static int getBufferLength(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_BUFFER_LENGTH, "5"));
    }

    public static boolean getGaplessPlaybackPreference(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getBoolean(Constants.PREFERENCES_KEY_GAPLESS_PLAYBACK, true);
    }	    
    
    public static boolean isNullOrWhiteSpace(String string) {
    	return string == null || string.isEmpty() || string.trim().isEmpty();
    }
	public static int getDirectoryCacheTime(Context context) {
		SharedPreferences preferences = getPreferences(context);
		return Integer.parseInt(preferences.getString(Constants.PREFERENCES_KEY_DIRECTORY_CACHE_TIME, "300"));
	} 
	
    public static int getChatRefreshInterval(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_CHAT_REFRESH_INTERVAL, "5000"));
    }
	
	public static WifiManager.WifiLock createWifiLock(Context context, String tag) {
		WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		int lockType = WifiManager.WIFI_MODE_FULL;
		if (Build.VERSION.SDK_INT >= 12) {
			lockType = 3;
		}
		return wm.createWifiLock(lockType, tag);
	}	
	
	    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / (float) DisplayMetrics.DENSITY_MEDIUM);
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / (metrics.densityDpi / (float) DisplayMetrics.DENSITY_MEDIUM);
    }
	
    public static Bitmap decodeBitmap(Context context, int resourceId) {
        return BitmapFactory.decodeResource(context.getResources(), resourceId);
    }

    public static boolean isServerCompatibleTo(Context context, String version) {
        Version serverVersion = getServerRestVersion(context);
        Version requiredVersion = new Version(version);
        return serverVersion == null || serverVersion.compareTo(requiredVersion) >= 0;
    }
    
	
	public static void confirmDialog(Context context, int action, String subject, DialogInterface.OnClickListener onClick) {
		Util.confirmDialog(context, context.getResources().getString(action).toLowerCase(), subject, onClick);
	}
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
