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

/**
 * @author Sindre Mehus
 * @version $Id: Constants.java 3534 2013-10-30 14:36:49Z sindre_mehus $
 */
public final class Constants {

    // Character encoding used throughout.
    public static final String UTF_8 = "UTF-8";

    // REST protocol version and client ID.
    // Note: Keep it as low as possible to maintain compatibility with older servers.
    public static final String REST_PROTOCOL_VERSION = "1.7.0";
    public static final String REST_CLIENT_ID = "madsonic";

    // Names for intent extras.
    public static final String INTENT_EXTRA_NAME_ID = "subsonic.id";
    public static final String INTENT_EXTRA_NAME_NAME = "subsonic.name";
    public static final String INTENT_EXTRA_NAME_PARENT_ID = "subsonic.parent.id";
    public static final String INTENT_EXTRA_NAME_PARENT_NAME = "subsonic.parent.name";
    public static final String INTENT_EXTRA_NAME_ARTIST = "subsonic.artist";
    public static final String INTENT_EXTRA_NAME_TITLE = "subsonic.title";
    public static final String INTENT_EXTRA_NAME_AUTOPLAY = "subsonic.playall";
    public static final String INTENT_EXTRA_NAME_ERROR = "subsonic.error";
    public static final String INTENT_EXTRA_NAME_QUERY = "subsonic.query";
    public static final String INTENT_EXTRA_NAME_QUERY_STARRED = "subsonic.queryStarred";
    public static final String INTENT_EXTRA_NAME_PLAYLIST_ID = "subsonic.playlist.id";
    public static final String INTENT_EXTRA_NAME_PLAYLIST_NAME = "subsonic.playlist.name";
    public static final String INTENT_EXTRA_NAME_ALBUM_LIST_TYPE = "subsonic.albumlisttype";
    public static final String INTENT_EXTRA_NAME_ALBUM_LIST_TITLE = "subsonic.albumlisttitle";
    public static final String INTENT_EXTRA_NAME_ALBUM_LIST_SIZE = "subsonic.albumlistsize";
    public static final String INTENT_EXTRA_NAME_ALBUM_LIST_OFFSET = "subsonic.albumlistoffset";
    public static final String INTENT_EXTRA_NAME_SHUFFLE = "subsonic.shuffle";
    public static final String INTENT_EXTRA_NAME_REFRESH = "subsonic.refresh";
    public static final String INTENT_EXTRA_REQUEST_SEARCH = "subsonic.requestsearch";
    public static final String INTENT_EXTRA_NAME_EXIT = "subsonic.exit" ;
    public static final String INTENT_EXTRA_NAME_STARRED = "subsonic.starred";
    public static final String INTENT_EXTRA_NAME_SHARE = "subsonic.share";
    public static final String INTENT_EXTRA_NAME_SHARED = "subsonic.shared";
    public static final String INTENT_EXTRA_NAME_RANDOM = "subsonic.random";
    public static final String INTENT_EXTRA_NAME_LASTPLAYED = "subsonic.lastplayed";
    public static final String INTENT_EXTRA_NAME_NEWADDED = "subsonic.newadded";
    public static final String INTENT_EXTRA_NAME_GENRE_NAME = "subsonic.genre";
    public static final String INTENT_EXTRA_NAME_ARTIST_GENRE_NAME = "subsonic.artist.genre";
    public static final String INTENT_EXTRA_NAME_VIDEOS = "subsonic.videos";
    public static final String INTENT_EXTRA_NAME_PODCAST = "subsonic.podcast";
	public static final String INTENT_EXTRA_NAME_PODCAST_ID = "subsonic.podcast.id";
	public static final String INTENT_EXTRA_NAME_PODCAST_NAME = "subsonic.podcast.name";
	public static final String INTENT_EXTRA_NAME_PODCAST_DESCRIPTION = "subsonic.podcast.description";
			
    
    // Notification IDs.
    public static final int NOTIFICATION_ID_PLAYING = 100;
    public static final int NOTIFICATION_ID_ERROR = 101;

    // Preferences keys.
    public static final String PREFERENCES_KEY_SERVER = "server";
    public static final String PREFERENCES_KEY_SERVER_ENABLED = "serverEnabled";
    public static final String PREFERENCES_KEY_SERVER_INSTANCE = "serverInstanceId";
    public static final String PREFERENCES_KEY_SERVER_NAME = "serverName";
    public static final String PREFERENCES_KEY_SERVER_URL = "serverUrl";
	public static final String PREFERENCES_KEY_SERVER_VERSION = "serverVersion";
    public static final String PREFERENCES_KEY_SERVERS_KEY = "serversKey";
    public static final String PREFERENCES_KEY_ADD_SERVER = "addServer";
    public static final String PREFERENCES_KEY_ADD_DEMOSERVER = "addDemoServer";  
    public static final String PREFERENCES_KEY_SERVER_NUMBER = "serverNum";
    public static final String PREFERENCES_KEY_REMOVE_SERVER = "removeServer";
    public static final String PREFERENCES_KEY_ACTIVE_SERVERS = "activeServers";
    public static final String PREFERENCES_KEY_MUSIC_FOLDER_ID = "musicFolderId";
    public static final String PREFERENCES_KEY_USERNAME = "username";
    public static final String PREFERENCES_KEY_PASSWORD = "password";
	public static final String PREFERENCES_KEY_LANGUAGE = "language";
    public static final String PREFERENCES_KEY_THEME = "theme";
    public static final String PREFERENCES_KEY_MAX_BITRATE_WIFI = "maxBitrateWifi";
    public static final String PREFERENCES_KEY_MAX_BITRATE_MOBILE = "maxBitrateMobile";
	public static final String PREFERENCES_KEY_MAX_VIDEO_BITRATE_WIFI = "maxVideoBitrateWifi";
    public static final String PREFERENCES_KEY_MAX_VIDEO_BITRATE_MOBILE = "maxVideoBitrateMobile";
    public static final String PREFERENCES_KEY_DISPLAY_BITRATE_WITH_ARTIST = "displayBitrateWithArtist";
	public static final String PREFERENCES_KEY_NETWORK_TIMEOUT = "networkTimeout";
    public static final String PREFERENCES_KEY_CACHE_SIZE = "cacheSize";
    public static final String PREFERENCES_KEY_CACHE_LOCATION = "cacheLocation";
    public static final String PREFERENCES_KEY_PRELOAD_COUNT = "preloadCount";
    public static final String PREFERENCES_KEY_HIDE_MEDIA = "hideMedia";
    public static final String PREFERENCES_KEY_MEDIA_BUTTONS = "mediaButtons";
    public static final String PREFERENCES_KEY_SCREEN_LIT_ON_DOWNLOAD = "screenLitOnDownload";
    public static final String PREFERENCES_KEY_SCROBBLE = "scrobble";
    public static final String PREFERENCES_KEY_SERVER_SCALING = "serverScaling";
    public static final String PREFERENCES_KEY_REPEAT_MODE = "repeatMode";
    public static final String PREFERENCES_KEY_WIFI_REQUIRED_FOR_DOWNLOAD = "wifiRequiredForDownload";
    public static final String PREFERENCES_KEY_BUFFER_LENGTH = "bufferLength";
	public static final String PREFERENCES_KEY_RANDOM_SIZE = "randomSize";
    public static final String PREFERENCES_KEY_MAX_ALBUMS = "maxAlbums";
    public static final String PREFERENCES_KEY_MAX_SONGS = "maxSongs";
	public static final String PREFERENCES_KEY_AUDIO_FOCUS = "audioFocus";
    public static final String PREFERENCES_KEY_MAX_ARTISTS = "maxArtists";
    public static final String PREFERENCES_KEY_DEFAULT_ALBUMS = "defaultAlbums";
    public static final String PREFERENCES_KEY_DEFAULT_SONGS = "defaultSongs";
    public static final String PREFERENCES_KEY_DEFAULT_ARTISTS = "defaultArtists";
    public static final String PREFERENCES_KEY_USE_STREAM_PROXY = "useStreamProxy";
    public static final String PREFERENCES_KEY_GAPLESS_PLAYBACK = "gaplessPlayback";
    public static final String PREFERENCES_KEY_CLEAR_SEARCH_HISTORY = "clearSearchHistory";
	public static final String PREFERENCES_KEY_PERSISTENT_NOTIFICATION = "persistentNotification";
    public static final String PREFERENCES_KEY_TEST_CONNECTION = "testConnection";
    public static final String PREFERENCES_KEY_IMPORT_TEMPLATE = "ImportTemplate";
    public static final String PREFERENCES_KEY_DEFAULT_VIDEOPLAYER = "defaultVideoplayer";
    public static final String PREFERENCES_KEY_VIDEO_PLAYER = "videoPlayer";
    public static final String PREFERENCES_KEY_COVER_SIZE = "coverSize";
	public static final String PREFERENCES_KEY_SEARCH_ENABLED = "searchEnabled";
	public static final String PREFERENCES_KEY_PODCAST_ENABLED = "podcastEnabled";
	public static final String PREFERENCES_KEY_CHAT_ENABLED = "chatEnabled";
	public static final String PREFERENCES_KEY_CHAT_REFRESH_INTERVAL = "chatRefreshInterval";
	public static final String PREFERENCES_KEY_DIRECTORY_CACHE_TIME = "directoryCacheTime";

	public static final String CACHE_KEY_IGNORE = "ignoreArticles";
	public static final String CHANNEL_LIST = "channelList";
    
    // Name of the preferences file.
    public static final String PREFERENCES_FILE_NAME = "github.madmarty.madsonic_preferences";

    public static final String ALBUM_ART_FILE = "cover.jpg";

    private Constants() {
    }
}
