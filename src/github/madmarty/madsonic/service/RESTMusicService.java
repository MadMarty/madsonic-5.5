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
package github.madmarty.madsonic.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.util.Xml;
import github.madmarty.madsonic.R;
import github.madmarty.madsonic.domain.ChatMessage;
import github.madmarty.madsonic.domain.Genre;
import github.madmarty.madsonic.domain.Indexes;
import github.madmarty.madsonic.domain.JukeboxStatus;
import github.madmarty.madsonic.domain.Lyrics;
import github.madmarty.madsonic.domain.MusicDirectory;
import github.madmarty.madsonic.domain.MusicFolder;
import github.madmarty.madsonic.domain.Playlist;
import github.madmarty.madsonic.domain.PodcastChannel;
import github.madmarty.madsonic.domain.SearchCritera;
import github.madmarty.madsonic.domain.SearchResult;
import github.madmarty.madsonic.domain.ServerInfo;
import github.madmarty.madsonic.domain.Version;
import github.madmarty.madsonic.service.parser.AlbumListParser;
import github.madmarty.madsonic.service.parser.ChatMessageParser;
import github.madmarty.madsonic.service.parser.ErrorParser;
import github.madmarty.madsonic.service.parser.GenreParser;
import github.madmarty.madsonic.service.parser.IndexesParser;
import github.madmarty.madsonic.service.parser.JukeboxStatusParser;
import github.madmarty.madsonic.service.parser.LicenseParser;
import github.madmarty.madsonic.service.parser.LyricsParser;
import github.madmarty.madsonic.service.parser.MusicDirectoryParser;
import github.madmarty.madsonic.service.parser.MusicFoldersParser;
import github.madmarty.madsonic.service.parser.PlaylistParser;
import github.madmarty.madsonic.service.parser.PlaylistsParser;
import github.madmarty.madsonic.service.parser.PodcastChannelParser;
import github.madmarty.madsonic.service.parser.PodcastEntryParser;
import github.madmarty.madsonic.service.parser.RandomSongsParser;
import github.madmarty.madsonic.service.parser.SearchResult2Parser;
import github.madmarty.madsonic.service.parser.SearchResultParser;
import github.madmarty.madsonic.service.parser.ShareParser;
import github.madmarty.madsonic.service.parser.VersionParser;
import github.madmarty.madsonic.service.ssl.SSLSocketFactory;
import github.madmarty.madsonic.service.ssl.TrustSelfSignedStrategy;
import github.madmarty.madsonic.util.CancellableTask;
import github.madmarty.madsonic.util.Constants;
import github.madmarty.madsonic.util.FileUtil;
import github.madmarty.madsonic.util.Logger;
import github.madmarty.madsonic.util.ProgressListener;
import github.madmarty.madsonic.util.Util;

/**
 * @author Sindre Mehus
 */
public class RESTMusicService implements MusicService {

    private static final Logger LOG = new Logger(RESTMusicService.class);

    private static final int SOCKET_CONNECT_TIMEOUT = 10 * 1000;
    private static final int SOCKET_READ_TIMEOUT_DEFAULT = 10 * 1000;
    private static final int SOCKET_READ_TIMEOUT_DOWNLOAD = 30 * 1000;
    private static final int SOCKET_READ_TIMEOUT_GET_RANDOM_SONGS = 60 * 1000;
    private static final int SOCKET_READ_TIMEOUT_GET_PLAYLIST = 60 * 1000;

    // Allow 20 seconds extra timeout per MB offset.
    private static final double TIMEOUT_MILLIS_PER_OFFSET_BYTE = 20000.0 / 1000000.0;

    /**
     * URL from which to fetch latest versions.
     */
    private static final String VERSION_URL = "http://subsonic.org/backend/version.view";

    private static final int HTTP_REQUEST_MAX_ATTEMPTS = 5;
    private static final long REDIRECTION_CHECK_INTERVAL_MILLIS = 60L * 60L * 1000L;

    private final DefaultHttpClient httpClient;
    private long redirectionLastChecked;
    private int redirectionNetworkType = -1;
    private String redirectFrom;
    private String redirectTo;
    private final ThreadSafeClientConnManager connManager;

    public RESTMusicService() {

        // Create and initialize default HTTP parameters
        HttpParams params = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(params, 20);
        ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRouteBean(20));
        HttpConnectionParams.setConnectionTimeout(params, SOCKET_CONNECT_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_DEFAULT);

        // Turn off stale checking.  Our connections break all the time anyway,
        // and it's not worth it to pay the penalty of checking every time.
        HttpConnectionParams.setStaleCheckingEnabled(params, false);

        // Create and initialize scheme registry
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", createSSLSocketFactory(), 443));

        // Create an HttpClient with the ThreadSafeClientConnManager.
        // This connection manager must be used if more than one thread will
        // be using the HttpClient.
        connManager = new ThreadSafeClientConnManager(params, schemeRegistry);
        httpClient = new DefaultHttpClient(connManager, params);
    }

    private SocketFactory createSSLSocketFactory() {
        try {
            return new SSLSocketFactory(new TrustSelfSignedStrategy(), SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        } catch (Throwable x) {
            LOG.error("Failed to create custom SSL socket factory, using default.", x);
            return org.apache.http.conn.ssl.SSLSocketFactory.getSocketFactory();
        }
    }

    @Override
    public void ping(Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, "ping", null);
        try {
            new ErrorParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
    }
    
	@Override
	public Version getAPIVersion(Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, "getLicense", null);
        try {
            ServerInfo serverInfo = new LicenseParser(context).parse(reader);
            return serverInfo.getRestVersion();
        } finally {
            Util.close(reader);
        }
    }
    
    @Override
    public boolean isLicenseValid(Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, "getLicense", null);
        try {
            ServerInfo serverInfo = new LicenseParser(context).parse(reader);
            return serverInfo.isLicenseValid();
        } finally {
            Util.close(reader);
        }
    }

//    public List<MusicFolder> getMusicFolders(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
//        Reader reader = getReader(context, progressListener, "getMusicFolders", null);
//        try {
//            return new MusicFoldersParser(context).parse(reader, progressListener);
//        } finally {
//            Util.close(reader);
//        }
//    }
    
    @Override
    public void startRescan(Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9.5", "Rescan not supported.");
		
        Reader reader = getReader(context, progressListener, "startRescan", null);
        try {
            new ErrorParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
    }    
    
    public List<MusicFolder> getMusicFolders(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        List<MusicFolder> cachedMusicFolders = readCachedMusicFolders(context);
        if (cachedMusicFolders != null && !refresh) {
            return cachedMusicFolders;
        }

        Reader reader = getReader(context, progressListener, "getMusicFolders", null);
        try {
            List<MusicFolder> musicFolders = new MusicFoldersParser(context).parse(reader, progressListener);
            writeCachedMusicFolders(context, musicFolders);
            return musicFolders;
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public Indexes getIndexes(String musicFolderId, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        Indexes cachedIndexes = readCachedIndexes(context, musicFolderId);
        if (cachedIndexes != null && !refresh) {
            return cachedIndexes;
        }

        long lastModified = cachedIndexes == null ? 0L : cachedIndexes.getLastModified();

        List<String> parameterNames = new ArrayList<String>();
        List<Object> parameterValues = new ArrayList<Object>();

        parameterNames.add("ifModifiedSince");
        parameterValues.add(lastModified);

        if (musicFolderId != null) {
            parameterNames.add("musicFolderId");
            parameterValues.add(musicFolderId);
        }

        Reader reader = getReader(context, progressListener, "getIndexes", null, parameterNames, parameterValues);
        try {
            Indexes indexes = new IndexesParser(context).parse(reader, progressListener);
            if (indexes != null) {
                writeCachedIndexes(context, indexes, musicFolderId);
                return indexes;
            }
            return cachedIndexes;
        } finally {
            Util.close(reader);
        }
    }

    private Indexes readCachedIndexes(Context context, String musicFolderId) {
        String filename = getCachedIndexesFilename(context, musicFolderId);
        return FileUtil.deserialize(context, filename);
    }

    private void writeCachedIndexes(Context context, Indexes indexes, String musicFolderId) {
        String filename = getCachedIndexesFilename(context, musicFolderId);
        FileUtil.serialize(context, indexes, filename);
    }

    private String getCachedIndexesFilename(Context context, String musicFolderId) {
        String s = Util.getRestUrl(context, null) + musicFolderId;
        return "indexes-" + Math.abs(s.hashCode()) + ".ser";
    }

    private ArrayList<MusicFolder> readCachedMusicFolders(Context context) {
        String filename = getCachedMusicFoldersFilename(context);
        return FileUtil.deserialize(context, filename);
    }

    private void writeCachedMusicFolders(Context context, List<MusicFolder> musicFolders) {
        String filename = getCachedMusicFoldersFilename(context);
        FileUtil.serialize(context, new ArrayList<MusicFolder>(musicFolders), filename);
    }

    private String getCachedMusicFoldersFilename(Context context) {
        String s = Util.getRestUrl(context, null);
        return "musicFolders-" + Math.abs(s.hashCode()) + ".ser";
    }

    @Override
    public MusicDirectory getMusicDirectory(String id, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, "getMusicDirectory", null, "id", id);
        try {
            return new MusicDirectoryParser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public SearchResult search(SearchCritera critera, Context context, ProgressListener progressListener) throws Exception {
        try {
            return searchNew(critera, context, progressListener);
        } catch (ServerTooOldException x) {
            // Ensure backward compatibility with REST 1.3.
            return searchOld(critera, context, progressListener);
        }
    }

    /**
     * Search using the "search" REST method.
     */
    private SearchResult searchOld(SearchCritera critera, Context context, ProgressListener progressListener) throws Exception {
        List<String> parameterNames = Arrays.asList("any", "songCount");
        List<Object> parameterValues = Arrays.<Object>asList(critera.getQuery(), critera.getSongCount());
        Reader reader = getReader(context, progressListener, "search", null, parameterNames, parameterValues);
        try {
            return new SearchResultParser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    /**
     * Search using the "search2" REST method, available in 1.4.0 and later.
     */
    private SearchResult searchNew(SearchCritera critera, Context context, ProgressListener progressListener) throws Exception {
        checkServerVersion(context, "1.4", null);

        List<String> parameterNames = Arrays.asList("query", "artistCount", "albumCount", "songCount");
        List<Object> parameterValues = Arrays.<Object>asList(critera.getQuery(), critera.getArtistCount(),
                                                             critera.getAlbumCount(), critera.getSongCount());
        Reader reader = getReader(context, progressListener, "search2", null, parameterNames, parameterValues);
        try {
            return new SearchResult2Parser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public MusicDirectory getPlaylist(String id, Context context, ProgressListener progressListener) throws Exception {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_GET_PLAYLIST);

        Reader reader = getReader(context, progressListener, "getPlaylist", params, "id", id);

        try {
			return new PlaylistParser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public URL createShare(String id, Context context, ProgressListener progressListener) throws Exception {
        checkServerVersion(context, "1.6", "Sharing not supported.");

        Reader reader = getReader(context, progressListener, "createShare", null, "id", id);
        try {
            return new ShareParser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public void star(String id, boolean star, Context context, ProgressListener progressListener) throws Exception {
        checkServerVersion(context, "1.8", "Starring not supported.");

        Reader reader = getReader(context, progressListener, star ? "star" : "unstar", null, "id", id);
        try {
            new ErrorParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
    }
    @Override
    public MusicDirectory getPlaylist(String id, String name, Context context, ProgressListener progressListener) throws Exception {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_GET_PLAYLIST);

        Reader reader = getReader(context, progressListener, "getPlaylist", params, "id", id);
        try {
			MusicDirectory playlist = new PlaylistParser(context).parse(reader, progressListener);
			
			File playlistFile = FileUtil.getPlaylistFile(name);
			FileWriter fw = new FileWriter(playlistFile);
			BufferedWriter bw = new BufferedWriter(fw);
			try {
				fw.write("#EXTM3U\n");
				for (MusicDirectory.Entry e : playlist.getChildren()) {
					String filePath = FileUtil.getSongFile(context, e).getAbsolutePath();
					if(! new File(filePath).exists()){
						String ext = FileUtil.getExtension(filePath);
						String base = FileUtil.getBaseName(filePath);
						filePath = base + ".complete." + ext;                
					}
					fw.write(filePath + "\n");
				}
			} catch(Exception e) {
				LOG.warn( "Failed to save playlist: " + name);
			} finally {
				bw.close();
				fw.close();
			}
			
			return playlist;
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public List<Playlist> getPlaylists(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, "getPlaylists", null);
        try {
            return new PlaylistsParser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public void createPlaylist(String id, String name, List<MusicDirectory.Entry> entries, Context context, ProgressListener progressListener) throws Exception {
    	checkServerVersion(context, "1.2", "Creating playlist not supported.");
        List<String> parameterNames = new LinkedList<String>();
        List<Object> parameterValues = new LinkedList<Object>();

        if (id != null) {
            parameterNames.add("playlistId");
            parameterValues.add(id);
        }
        if (name != null) {
            parameterNames.add("name");
            parameterValues.add(name);
        }
        for (MusicDirectory.Entry entry : entries) {
            parameterNames.add("songId");
            parameterValues.add(entry.getId());
        }

        Reader reader = getReader(context, progressListener, "createPlaylist", null, parameterNames, parameterValues);
        try {
            new ErrorParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
    }
	
	@Override
	public void deletePlaylist(String id, Context context, ProgressListener progressListener) throws Exception {		
		checkServerVersion(context, "1.2", "Deleting playlist not supported.");
		Reader reader = getReader(context, progressListener, "deletePlaylist", null, "id", id);
		try {
            new ErrorParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
	}
	
	@Override
	public void addToPlaylist(String id, List<MusicDirectory.Entry> toAdd, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.8", "Updating playlist not supported.");
		List<String> names = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();
		names.add("playlistId");
		values.add(id);
		for(MusicDirectory.Entry song: toAdd) {
			names.add("songIdToAdd");
			values.add(song.getId());
		}
		Reader reader = getReader(context, progressListener, "updatePlaylist", null, names, values);
    	try {
            new ErrorParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
	}
	
	@Override
	public void removeFromPlaylist(String id, List<Integer> toRemove, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.8", "Removing from playlist not supported.");
		List<String> names = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();
		names.add("playlistId");
		values.add(id);
		for(Integer song: toRemove) {
			names.add("songIndexToRemove");
			values.add(song);
		}
		Reader reader = getReader(context, progressListener, "updatePlaylist", null, names, values);
    	try {
            new ErrorParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
	}
	
	@Override
	public void overwritePlaylist(String id, String name, int toRemove, List<MusicDirectory.Entry> toAdd, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.8", "Updating playlists is not supported.");
		List<String> names = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();
		names.add("playlistId");
		values.add(id);
		names.add("name");
		values.add(name);
		for(MusicDirectory.Entry song: toAdd) {
			names.add("songIdToAdd");
			values.add(song.getId());
		}
		for(int i = 0; i < toRemove; i++) {
			names.add("songIndexToRemove");
			values.add(i);
		}
		Reader reader = getReader(context, progressListener, "updatePlaylist", null, names, values);
    	try {
            new ErrorParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
	}
	
	@Override
	public void updatePlaylist(String id, String name, String comment, boolean pub, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.8", "Updating playlist not supported.");
		Reader reader = getReader(context, progressListener, "updatePlaylist", null, Arrays.asList("playlistId", "name", "comment", "public"), Arrays.<Object>asList(id, name, comment, pub));
		try {
			new ErrorParser(context).parse(reader);
		} finally {
			Util.close(reader);
		}
	}
    
    
    @Deprecated
    public Lyrics getLyrics(String artist, String title, Context context, ProgressListener progressListener) throws Exception {
    	checkServerVersion(context, "1.2", "Lyrics not supported.");
    	
        Reader reader = getReader(context, progressListener, "getLyrics", null, Arrays.asList("artist", "title"), Arrays.<Object>asList(artist, title));
        try {
            return new LyricsParser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

	@Override
    public Lyrics getLyrics(String id, String artist, String title, Context context, ProgressListener progressListener) throws Exception {
    	checkServerVersion(context, "1.10", "Lyrics not supported.");
    	
        Reader reader = getReader(context, progressListener, "getLyrics", null, Arrays.asList("id","artist", "title"), Arrays.<Object>asList(id, artist, title));
        try {
            return new LyricsParser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }
    
    @Override
    public void scrobble(String id, boolean submission, Context context, ProgressListener progressListener) throws Exception {
        checkServerVersion(context, "1.5", "Scrobbling not supported.");
        Reader reader = getReader(context, progressListener, "scrobble", null, Arrays.asList("id", "submission"), Arrays.<Object>asList(id, submission));
        try {
            new ErrorParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public MusicDirectory getAlbumList(String type, int size, int offset, Context context, ProgressListener progressListener) throws Exception {
   	
    	if ("allArtist".equals(type) || "starredArtist".equals(type)) {
    		checkServerVersion(context, "1.10.5", "Listtype not supported.");
    	}    	
    	
    	if ("hot".equals(type) || "tip".equals(type)) {
    		checkServerVersion(context, "1.9.5", "Listtype not supported.");
    	}
		
        Reader reader = getReader(context, progressListener, "getAlbumList",
                                  null, Arrays.asList("type", "size", "offset"), Arrays.<Object>asList(type, size, offset));
        try {
            return new AlbumListParser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }

    @Override
    public MusicDirectory getRandomSongs(int size, Context context, ProgressListener progressListener) throws Exception {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_GET_RANDOM_SONGS);

        Reader reader = getReader(context, progressListener, "getRandomSongs", params, "size", size);
        try {
            return new RandomSongsParser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
    }
    
    @Override
    public SearchResult getStarred(Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, "getStarred", null);
        try {
            return new SearchResult2Parser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }	
    }

	@Override
	public MusicDirectory getLastplayedSongs(int size, Context context, ProgressListener progressListener) throws Exception {
        checkServerVersion(context, "1.10.5", "Lastplayed Songs not supported.");
		HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_GET_RANDOM_SONGS);

        Reader reader = getReader(context, progressListener, "getLastplayedSongs", params, "size", size);
        try {
            return new RandomSongsParser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public MusicDirectory getNewaddedSongs(int size, Context context, ProgressListener progressListener) throws Exception {
        checkServerVersion(context, "1.10.5", "Newadded Songs not supported.");
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_GET_RANDOM_SONGS);

        Reader reader = getReader(context, progressListener, "getNewaddedSongs", params, "size", size);
        try {
            return new RandomSongsParser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
	}    

	@Override
	public MusicDirectory getSharedSongs(String sharename, Context context, ProgressListener progressListener) throws Exception {
        checkServerVersion(context, "1.10.5", "get shared Songs not supported.");
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_GET_RANDOM_SONGS);

        Reader reader = getReader(context, progressListener, "getSharedFiles", params, "share", sharename);
        try {
            return new RandomSongsParser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
	}  	
	
    @Override
    public Version getLocalVersion(Context context) throws Exception {
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo("github.madmarty.madsonic", 0);
        return new Version(packageInfo.versionName);
    }

    @Override
    public Version getLatestVersion(Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReaderForURL(context, VERSION_URL, null, null, null, progressListener);
        try {
            return new VersionParser().parse(reader);
        } finally {
            Util.close(reader);
        }
    }

    private void checkServerVersion(Context context, String version, String text) throws ServerTooOldException {
        Version serverVersion = Util.getServerRestVersion(context);
        Version requiredVersion = new Version(version);
        boolean ok = serverVersion == null || serverVersion.compareTo(requiredVersion) >= 0;

        if (!ok) {
            throw new ServerTooOldException(text, serverVersion, requiredVersion);
        }
    }

    @Override
    public Bitmap getCoverArt(Context context, MusicDirectory.Entry entry, int size, int saveSize, ProgressListener progressListener) throws Exception {

        // Synchronize on the entry so that we don't download concurrently for the same song.
        synchronized (entry) {

            // Use cached file, if existing.
            Bitmap bitmap = FileUtil.getAlbumArtBitmap(context, entry, size);
            if (bitmap != null) {
                return bitmap;
            }

            String url = Util.getRestUrl(context, "getCoverArt");

            InputStream in = null;
            try {
                List<String> parameterNames = Arrays.asList("id", "size");
                List<Object> parameterValues = Arrays.<Object>asList(entry.getCoverArt(), saveSize);
                HttpEntity entity = getEntityForURL(context, url, null, parameterNames, parameterValues, progressListener);
                in = entity.getContent();

                // If content type is XML, an error occured.  Get it.
                String contentType = Util.getContentType(entity);
                if (contentType != null && contentType.startsWith("text/xml")) {
                    new ErrorParser(context).parse(new InputStreamReader(in, Constants.UTF_8));
                    return null; // Never reached.
                }

                byte[] bytes = Util.toByteArray(in);

				File albumDir = FileUtil.getAlbumDirectory(context, entry);
                if (albumDir.exists()) {
                    OutputStream out = null;
                    try {
                        out = new FileOutputStream(FileUtil.getAlbumArtFile(albumDir));
                        out.write(bytes);
                    } finally {
                        Util.close(out);
                    }
                }

                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
				if(size != saveSize) {
					bitmap = Bitmap.createScaledBitmap(bitmap, size, size, true);
				}
				return bitmap;

            } finally {
                Util.close(in);
            }
        }
    }

    @Override
    public HttpResponse getDownloadInputStream(Context context, MusicDirectory.Entry song, long offset, int maxBitrate, CancellableTask task) throws Exception {

        String url = Util.getRestUrl(context, "stream");

        // Set socket read timeout. Note: The timeout increases as the offset gets larger. This is
        // to avoid the thrashing effect seen when offset is combined with transcoding/downsampling on the server.
        // In that case, the server uses a long time before sending any data, causing the client to time out.
        HttpParams params = new BasicHttpParams();
        int timeout = (int) (SOCKET_READ_TIMEOUT_DOWNLOAD + offset * TIMEOUT_MILLIS_PER_OFFSET_BYTE);
        HttpConnectionParams.setSoTimeout(params, timeout);

        // Add "Range" header if offset is given.
        List<Header> headers = new ArrayList<Header>();
        if (offset > 0) {
            headers.add(new BasicHeader("Range", "bytes=" + offset + "-"));
        }
        List<String> parameterNames = Arrays.asList("id", "maxBitRate");
        List<Object> parameterValues = Arrays.<Object>asList(song.getId(), maxBitrate);
        HttpResponse response = getResponseForURL(context, url, params, parameterNames, parameterValues, headers, null, task);

        // If content type is XML, an error occurred.  Get it.
        String contentType = Util.getContentType(response.getEntity());
        if (contentType != null && contentType.startsWith("text/xml")) {
            InputStream in = response.getEntity().getContent();
            try {
                new ErrorParser(context).parse(new InputStreamReader(in, Constants.UTF_8));
            } finally {
                Util.close(in);
            }
        }

        return response;
    }

    @Override
    public String getVideoUrl(int maxBitrate, Context context, String id, boolean useFlash) throws Exception {
        StringBuilder builder = new StringBuilder();
        if (useFlash) {
            builder.append(Util.getRestUrl(context, "videoPlayer"));
            builder.append("&id=").append(id);
            builder.append("&maxBitRate=").append(maxBitrate);
            builder.append("&autoplay=true");
        } else {
            checkServerVersion(context, "1.9", "Video streaming not supported.");
            builder.append(Util.getRestUrl(context, "stream"));
            builder.append("&id=").append(id);
            builder.append("&format=raw");
        }

        String url = rewriteUrlWithRedirect(context, builder.toString());
        LOG.info( "Using video URL: " + url);
        return url;
    }

    @Deprecated
    public String getVideoUrl(int maxBitrate, Context context, String id) {
        StringBuilder builder = new StringBuilder(Util.getRestUrl(context, "videoPlayer"));
        builder.append("&id=").append(id);
        builder.append("&maxBitRate=").append(maxBitrate);
        builder.append("&autoplay=true");

        String url = rewriteUrlWithRedirect(context, builder.toString());
        LOG.info( "Using video URL: " + url);
        return url;
    }
	
    @Deprecated
	public String getVideoStreamUrl(int maxBitrate, Context context, String id) {
		StringBuilder builder = new StringBuilder(Util.getRestUrl(context, "stream"));
        builder.append("&id=").append(id);
        builder.append("&maxBitRate=").append(maxBitrate);

        String url = rewriteUrlWithRedirect(context, builder.toString());
        LOG.info( "Using video URL: " + url);
        return url;
	}

    @Override
    public JukeboxStatus updateJukeboxPlaylist(List<String> ids, Context context, ProgressListener progressListener) throws Exception {
        int n = ids.size();
        List<String> parameterNames = new ArrayList<String>(n + 1);
        parameterNames.add("action");
        for (int i = 0; i < n; i++) {
            parameterNames.add("id");
        }
        List<Object> parameterValues = new ArrayList<Object>();
        parameterValues.add("set");
        parameterValues.addAll(ids);

        return executeJukeboxCommand(context, progressListener, parameterNames, parameterValues);
    }

    @Override
    public JukeboxStatus skipJukebox(int index, int offsetSeconds, Context context, ProgressListener progressListener) throws Exception {
        List<String> parameterNames = Arrays.asList("action", "index", "offset");
        List<Object> parameterValues = Arrays.<Object>asList("skip", index, offsetSeconds);
        return executeJukeboxCommand(context, progressListener, parameterNames, parameterValues);
    }

    @Override
    public JukeboxStatus stopJukebox(Context context, ProgressListener progressListener) throws Exception {
        return executeJukeboxCommand(context, progressListener, Arrays.asList("action"), Arrays.<Object>asList("stop"));
    }

    @Override
    public JukeboxStatus startJukebox(Context context, ProgressListener progressListener) throws Exception {
        return executeJukeboxCommand(context, progressListener, Arrays.asList("action"), Arrays.<Object>asList("start"));
    }

    @Override
    public JukeboxStatus getJukeboxStatus(Context context, ProgressListener progressListener) throws Exception {
        return executeJukeboxCommand(context, progressListener, Arrays.asList("action"), Arrays.<Object>asList("status"));
    }

    @Override
    public JukeboxStatus setJukeboxGain(float gain, Context context, ProgressListener progressListener) throws Exception {
        List<String> parameterNames = Arrays.asList("action", "gain");
        List<Object> parameterValues = Arrays.<Object>asList("setGain", gain);
        return executeJukeboxCommand(context, progressListener, parameterNames, parameterValues);

    }

    private JukeboxStatus executeJukeboxCommand(Context context, ProgressListener progressListener, List<String> parameterNames, List<Object> parameterValues) throws Exception {
        checkServerVersion(context, "1.7", "Jukebox not supported.");
        Reader reader = getReader(context, progressListener, "jukeboxControl", null, parameterNames, parameterValues);
        try {
            return new JukeboxStatusParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
    }
    
    @Override
    public void setStarred(String id, boolean starred, Context context, ProgressListener progressListener) throws Exception {
    	checkServerVersion(context, "1.8", "Starring is not supported.");
    	Reader reader = getReader(context, progressListener, starred ? "star" : "unstar", null, "id", id);
    	try {
            new ErrorParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
    }
	
	@Override
	public List<PodcastChannel> getPodcastChannels(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.6", "Podcasts not supported.");
		
		Reader reader = getReader(context, progressListener, "getPodcasts", null, Arrays.asList("includeEpisodes"), Arrays.<Object>asList("false"));
        try {
            List<PodcastChannel> channels = new PodcastChannelParser(context).parse(reader, progressListener);
			
			String content = "";
			for(PodcastChannel channel: channels) {
				content += channel.getName() + "\n";
			}
			//FIXME:TPDO ,0
			File file = FileUtil.getPodcastFile(context, Util.getServerName(context,0));
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(content);
			bw.close();
			
			return channels;
        } finally {
            Util.close(reader);
        }
	}
	
	@Override
	public MusicDirectory getPodcastEpisodes(boolean refresh, String id, Context context, ProgressListener progressListener) throws Exception {
		//Reader reader = getReader(context, progressListener, "getPodcasts", null, Arrays.asList("id"), Arrays.<Object>asList(id));
		Reader reader = getReader(context, progressListener, "getPodcasts", null);
		
        try {
            return new PodcastEntryParser(context).parse(id, reader, progressListener);
        } finally {
            Util.close(reader);
        }
	}
	
	@Override
	public void refreshPodcasts(Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", "Refresh podcasts not supported.");
		
		Reader reader = getReader(context, progressListener, "refreshPodcasts", null);
		try {
            new ErrorParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
	}
	
	@Override
	public void createPodcastChannel(String url, Context context, ProgressListener progressListener) throws Exception{
		checkServerVersion(context, "1.9", "Creating podcasts not supported.");
		
		Reader reader = getReader(context, progressListener, "createPodcastChannel", null, "url", url);
		try {
            new ErrorParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
	}
	
	@Override
	public void deletePodcastChannel(String id, Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", "Deleting podcasts not supported.");
		
		Reader reader = getReader(context, progressListener, "deletePodcastChannel", null, "id", id);
		try {
            new ErrorParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
	}
	
	@Override
	public void downloadPodcastEpisode(String id, Context context, ProgressListener progressListener) throws Exception{
		checkServerVersion(context, "1.9", "Downloading podcasts not supported.");
		
		Reader reader = getReader(context, progressListener, "downloadPodcastEpisode", null, "id", id);
		try {
            new ErrorParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
	}
	
	@Override
	public void deletePodcastEpisode(String id, Context context, ProgressListener progressListener) throws Exception{
		checkServerVersion(context, "1.9", "Deleting podcasts not supported.");
		
		Reader reader = getReader(context, progressListener, "deletePodcastEpisode", null, "id", id);
		try {
            new ErrorParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
	}

    private Reader getReader(Context context, ProgressListener progressListener, String method, HttpParams requestParams) throws Exception {
        return getReader(context, progressListener, method, requestParams, Collections.<String>emptyList(), Collections.emptyList());
    }

    private Reader getReader(Context context, ProgressListener progressListener, String method,
                             HttpParams requestParams, String parameterName, Object parameterValue) throws Exception {
        return getReader(context, progressListener, method, requestParams, Arrays.asList(parameterName), Arrays.<Object>asList(parameterValue));
    }

    private Reader getReader(Context context, ProgressListener progressListener, String method,
                             HttpParams requestParams, List<String> parameterNames, List<Object> parameterValues) throws Exception {

        if (progressListener != null) {
            progressListener.updateProgress(R.string.service_connecting);
        }

        String url = Util.getRestUrl(context, method);
        return getReaderForURL(context, url, requestParams, parameterNames, parameterValues, progressListener);
    }

    private Reader getReaderForURL(Context context, String url, HttpParams requestParams, List<String> parameterNames,
                                   List<Object> parameterValues, ProgressListener progressListener) throws Exception {
        HttpEntity entity = getEntityForURL(context, url, requestParams, parameterNames, parameterValues, progressListener);
        if (entity == null) {
            throw new RuntimeException("No entity received for URL " + url);
        }

        InputStream in = entity.getContent();
        return new InputStreamReader(in, Constants.UTF_8);
    }

    private HttpEntity getEntityForURL(Context context, String url, HttpParams requestParams, List<String> parameterNames,
                                       List<Object> parameterValues, ProgressListener progressListener) throws Exception {
        return getResponseForURL(context, url, requestParams, parameterNames, parameterValues, null, progressListener, null).getEntity();
    }

    private HttpResponse getResponseForURL(Context context, String url, HttpParams requestParams,
                                           List<String> parameterNames, List<Object> parameterValues,
                                           List<Header> headers, ProgressListener progressListener, CancellableTask task) throws Exception {
        LOG.debug("Connections in pool: " + connManager.getConnectionsInPool());

        // If not too many parameters, extract them to the URL rather than relying on the HTTP POST request being
        // received intact. Remember, HTTP POST requests are converted to GET requests during HTTP redirects, thus
        // loosing its entity.
        if (parameterNames != null && parameterNames.size() < 10) {
            StringBuilder builder = new StringBuilder(url);
            for (int i = 0; i < parameterNames.size(); i++) {
                builder.append("&").append(parameterNames.get(i)).append("=");
                builder.append(URLEncoder.encode(String.valueOf(parameterValues.get(i)), "UTF-8"));
            }
            url = builder.toString();
            parameterNames = null;
            parameterValues = null;
        }

        String rewrittenUrl = rewriteUrlWithRedirect(context, url);
        return executeWithRetry(context, rewrittenUrl, url, requestParams, parameterNames, parameterValues, headers, progressListener, task);
    }

    private HttpResponse executeWithRetry(Context context, String url, String originalUrl, HttpParams requestParams,
                                          List<String> parameterNames, List<Object> parameterValues,
                                          List<Header> headers, ProgressListener progressListener, CancellableTask task) throws IOException {
        LOG.info("Using URL " + url);
		
		SharedPreferences prefs = Util.getPreferences(context);
		int networkTimeout = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_NETWORK_TIMEOUT, "15000"));
		HttpParams newParams = httpClient.getParams();
		HttpConnectionParams.setSoTimeout(newParams, networkTimeout);
		httpClient.setParams(newParams);

        final AtomicReference<Boolean> cancelled = new AtomicReference<Boolean>(false);
        int attempts = 0;
        while (true) {
            attempts++;
            HttpContext httpContext = new BasicHttpContext();
            final HttpPost request = new HttpPost(url);

            if (task != null) {
                // Attempt to abort the HTTP request if the task is cancelled.
                task.setOnCancelListener(new CancellableTask.OnCancelListener() {
                    @Override
                    public void onCancel() {
						new Thread(new Runnable() {
							public void run() {
								try {
									cancelled.set(true);
									request.abort();
								} catch(Exception e) {
									LOG.error("Failed to stop http task");
								}
							}
						}).start();
                    }
                });
            }

            if (parameterNames != null) {
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                for (int i = 0; i < parameterNames.size(); i++) {
                    params.add(new BasicNameValuePair(parameterNames.get(i), String.valueOf(parameterValues.get(i))));
                }
                request.setEntity(new UrlEncodedFormEntity(params, Constants.UTF_8));
            }

            if (requestParams != null) {
                request.setParams(requestParams);
                LOG.debug("Socket read timeout: " + HttpConnectionParams.getSoTimeout(requestParams) + " ms.");
            }

            if (headers != null) {
                for (Header header : headers) {
                    request.addHeader(header);
                }
            }

            // Set credentials to get through apache proxies that require authentication.
            
            int instance = prefs.getInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1);
            String username = prefs.getString(Constants.PREFERENCES_KEY_USERNAME + instance, null);
            String password = prefs.getString(Constants.PREFERENCES_KEY_PASSWORD + instance, null);
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(username, password));

            try {
                HttpResponse response = httpClient.execute(request, httpContext);
                detectRedirect(originalUrl, context, httpContext);
                return response;
            } catch (IOException x) {
                request.abort();
                if (attempts >= HTTP_REQUEST_MAX_ATTEMPTS || cancelled.get()) {
                    throw x;
                }
                if (progressListener != null) {
                    String msg = context.getResources().getString(R.string.music_service_retry, attempts, HTTP_REQUEST_MAX_ATTEMPTS - 1);
                    progressListener.updateProgress(msg);
                }
                LOG.warn("Got IOException (" + attempts + "), will retry", x);
                increaseTimeouts(requestParams);
                Util.sleepQuietly(2000L);
            }
        }
    }

    private void increaseTimeouts(HttpParams requestParams) {
        if (requestParams != null) {
            int connectTimeout = HttpConnectionParams.getConnectionTimeout(requestParams);
            if (connectTimeout != 0) {
                HttpConnectionParams.setConnectionTimeout(requestParams, (int) (connectTimeout * 1.3F));
            }
            int readTimeout = HttpConnectionParams.getSoTimeout(requestParams);
            if (readTimeout != 0) {
                HttpConnectionParams.setSoTimeout(requestParams, (int) (readTimeout * 1.5F));
            }
        }
    }

    private void detectRedirect(String originalUrl, Context context, HttpContext httpContext) {
        HttpUriRequest request = (HttpUriRequest) httpContext.getAttribute(ExecutionContext.HTTP_REQUEST);
        HttpHost host = (HttpHost) httpContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);

        // Sometimes the request doesn't contain the "http://host" part so we
        // must take from the HttpHost object.
        String redirectedUrl;
        if (request.getURI().getScheme() == null) {
            redirectedUrl = host.toURI() + request.getURI();
        } else {
            redirectedUrl = request.getURI().toString();
        }

        redirectFrom = originalUrl.substring(0, originalUrl.indexOf("/rest/"));
        redirectTo = redirectedUrl.substring(0, redirectedUrl.indexOf("/rest/"));

        LOG.info(redirectFrom + " redirects to " + redirectTo);
        redirectionLastChecked = System.currentTimeMillis();
        redirectionNetworkType = getCurrentNetworkType(context);
    }

    private String rewriteUrlWithRedirect(Context context, String url) {

        // Only cache for a certain time.
        if (System.currentTimeMillis() - redirectionLastChecked > REDIRECTION_CHECK_INTERVAL_MILLIS) {
            return url;
        }

        // Ignore cache if network type has changed.
        if (redirectionNetworkType != getCurrentNetworkType(context)) {
            return url;
        }

        if (redirectFrom == null || redirectTo == null) {
            return url;
        }

        return url.replace(redirectFrom, redirectTo);
    }

    private int getCurrentNetworkType(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        return networkInfo == null ? -1 : networkInfo.getType();
    }
	@Override
	public List<Genre> getGenres(Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.9", "Genres not supported.");
        Reader reader = getReader(context, progressListener, "getGenres", null);
        try {
            return new GenreParser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public List<Genre> getArtistsGenres(Context context, ProgressListener progressListener) throws Exception {
		checkServerVersion(context, "1.10", "Artist Genres not supported.");
        Reader reader = getReader(context, progressListener, "getArtistGenres", null);
        try {
            return new GenreParser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
	}	
	
	@Override
	public MusicDirectory getSongsByGenre(String genre, int count, int offset, Context context, ProgressListener progressListener) throws Exception {
    	checkServerVersion(context, "1.9", "Genres not supported.");
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_GET_RANDOM_SONGS);

        List<String> parameterNames = new ArrayList<String>();
        List<Object> parameterValues = new ArrayList<Object>();

        parameterNames.add("genre");
        parameterValues.add(genre);
        parameterNames.add("count");
        parameterValues.add(count);
        parameterNames.add("offset");
        parameterValues.add(offset);
       
        Reader reader = getReader(context, progressListener, "getSongsByGenre", params, parameterNames, parameterValues);
        
        try {
            return new RandomSongsParser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public MusicDirectory getArtistsByGenre(String genre, int count, int offset, Context context, ProgressListener progressListener) throws Exception {
    	checkServerVersion(context, "1.10.5", "Genres not supported.");
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_GET_RANDOM_SONGS);

        List<String> parameterNames = new ArrayList<String>();
        List<Object> parameterValues = new ArrayList<Object>();

        parameterNames.add("genre");
        parameterValues.add(genre);
        parameterNames.add("count");
        parameterValues.add(count);
        parameterNames.add("offset");
        parameterValues.add(offset);
       
        Reader reader = getReader(context, progressListener, "getArtistsByGenre", params, parameterNames, parameterValues);
        
        try {
            return new RandomSongsParser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
	}	

	@Override
	public List<ChatMessage> getChatMessages(Long since, Context context, ProgressListener progressListener) throws Exception {
    	checkServerVersion(context, "1.2", "Chat not supported.");
    	
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_GET_RANDOM_SONGS);

        List<String> parameterNames = new ArrayList<String>();
        List<Object> parameterValues = new ArrayList<Object>();

        parameterNames.add("since");
        parameterValues.add(since);
       
        Reader reader = getReader(context, progressListener, "getChatMessages", params, parameterNames, parameterValues);
        
        try {
            return new ChatMessageParser(context).parse(reader, progressListener);
        } finally {
            Util.close(reader);
        }
	}
	
	@Override
	public void addChatMessage(String message, Context context, ProgressListener progressListener) throws Exception {
    	checkServerVersion(context, "1.2", "Chat not supported.");
    	
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_GET_RANDOM_SONGS);

        List<String> parameterNames = new ArrayList<String>();
        List<Object> parameterValues = new ArrayList<Object>();

        parameterNames.add("message");
        parameterValues.add(message);
       
        Reader reader = getReader(context, progressListener, "addChatMessage", params, parameterNames, parameterValues);
        
        try {
            new ErrorParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
	}

	@Override
	public MusicDirectory getVideos(boolean refresh, Context context, ProgressListener progressListener) throws Exception
	{
		checkServerVersion(context, "1.8", "Videos not supported.");

		Reader reader = getReader(context, progressListener, "getVideos", null);

		try
		{
			return new MusicDirectoryParser(context).parse(reader, progressListener);
		}
		finally
		{
			Util.close(reader);
		}
	}
}
