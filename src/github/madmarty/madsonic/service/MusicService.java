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

import java.util.List;

import org.apache.http.HttpResponse;

import android.content.Context;
import android.graphics.Bitmap;
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
import github.madmarty.madsonic.domain.Version;
import github.madmarty.madsonic.util.CancellableTask;
import github.madmarty.madsonic.util.ProgressListener;

import java.net.URL;
import java.util.List;
/**
 * @author Sindre Mehus
 */
public interface MusicService {

    void ping(Context context, ProgressListener progressListener) throws Exception;
    
    Version getAPIVersion(Context context, ProgressListener progressListener) throws Exception;
	
    boolean isLicenseValid(Context context, ProgressListener progressListener) throws Exception;

    List<Genre> getGenres(Context context, ProgressListener progressListener) throws Exception;
    
    List<Genre> getArtistsGenres(Context context, ProgressListener progressListener) throws Exception;
    
    void startRescan(Context context, ProgressListener progressListener) throws Exception;

    List<MusicFolder> getMusicFolders(boolean refresh, Context context, ProgressListener progressListener) throws Exception;

    Indexes getIndexes(String musicFolderId, boolean refresh, Context context, ProgressListener progressListener) throws Exception;

    MusicDirectory getMusicDirectory(String id, boolean refresh, Context context, ProgressListener progressListener) throws Exception;

    SearchResult search(SearchCritera criteria, Context context, ProgressListener progressListener) throws Exception;

    MusicDirectory getPlaylist(String id, String name, Context context, ProgressListener progressListener) throws Exception;

    URL createShare(String id, Context context, ProgressListener progressListener) throws Exception;

    MusicDirectory getPlaylist(String id, Context context, ProgressListener progressListener) throws Exception;
    
    List<Playlist> getPlaylists(boolean refresh, Context context, ProgressListener progressListener) throws Exception;

    void createPlaylist(String id, String name, List<MusicDirectory.Entry> entries, Context context, ProgressListener progressListener) throws Exception;

	void deletePlaylist(String id, Context context, ProgressListener progressListener) throws Exception;
	
	void addToPlaylist(String id, List<MusicDirectory.Entry> toAdd, Context context, ProgressListener progressListener) throws Exception;
	
	void removeFromPlaylist(String id, List<Integer> toRemove, Context context, ProgressListener progressListener) throws Exception;
	
	void overwritePlaylist(String id, String name, int toRemove, List<MusicDirectory.Entry> toAdd, Context context, ProgressListener progressListener) throws Exception;
	
	void updatePlaylist(String id, String name, String comment, boolean pub, Context context, ProgressListener progressListener) throws Exception;

	@Deprecated
    Lyrics getLyrics(String artist, String title, Context context, ProgressListener progressListener) throws Exception;
	
    Lyrics getLyrics(String id, String artist, String title, Context context, ProgressListener progressListener) throws Exception;

    void scrobble(String id, boolean submission, Context context, ProgressListener progressListener) throws Exception;

    MusicDirectory getAlbumList(String type, int size, int offset, Context context, ProgressListener progressListener) throws Exception;

    MusicDirectory getRandomSongs(int size, Context context, ProgressListener progressListener) throws Exception;
    
    MusicDirectory getSongsByGenre(String genre, int count, int offset, Context context, ProgressListener progressListener) throws Exception;

	MusicDirectory getArtistsByGenre(String genre, int count, int offset, Context context, ProgressListener progressListener) throws Exception;

	MusicDirectory getLastplayedSongs(int size, Context context, ProgressListener progressListener) throws Exception;

	MusicDirectory getNewaddedSongs(int size, Context context, ProgressListener progressListener) throws Exception;

	MusicDirectory getSharedSongs(String sharename, Context context, ProgressListener progressListener) throws Exception;
	
    SearchResult getStarred(Context context, ProgressListener progressListener) throws Exception;

    Bitmap getCoverArt(Context context, MusicDirectory.Entry entry, int size, int saveSize, ProgressListener progressListener) throws Exception;

    HttpResponse getDownloadInputStream(Context context, MusicDirectory.Entry song, long offset, int maxBitrate, CancellableTask task) throws Exception;

    Version getLocalVersion(Context context) throws Exception;

    Version getLatestVersion(Context context, ProgressListener progressListener) throws Exception;

    String getVideoUrl(int maxBitrate, Context context, String id, boolean useFlash) throws Exception;

    String getVideoUrl(int maxBitrate, Context context, String id);
	
	String getVideoStreamUrl(int Bitrate, Context context, String id);

    JukeboxStatus updateJukeboxPlaylist(List<String> ids, Context context, ProgressListener progressListener) throws Exception;

    JukeboxStatus skipJukebox(int index, int offsetSeconds, Context context, ProgressListener progressListener) throws Exception;

    JukeboxStatus stopJukebox(Context context, ProgressListener progressListener) throws Exception;

    JukeboxStatus startJukebox(Context context, ProgressListener progressListener) throws Exception;

    JukeboxStatus getJukeboxStatus(Context context, ProgressListener progressListener) throws Exception;

    JukeboxStatus setJukeboxGain(float gain, Context context, ProgressListener progressListener) throws Exception;
    
    void setStarred(String id, boolean starred, Context context, ProgressListener progressListener) throws Exception;
    
    void star(String id, boolean star, Context context, ProgressListener progressListener) throws Exception;
	
    List<ChatMessage> getChatMessages(Long since, Context context, ProgressListener progressListener) throws Exception;
    
    void addChatMessage(String message, Context context, ProgressListener progressListener) throws Exception;
	
	List<PodcastChannel> getPodcastChannels(boolean refresh, Context context, ProgressListener progressListener) throws Exception;
	
	MusicDirectory getPodcastEpisodes(boolean refresh, String id, Context context, ProgressListener progressListener) throws Exception;
	
	void refreshPodcasts(Context context, ProgressListener progressListener) throws Exception;
	
	void createPodcastChannel(String url, Context context, ProgressListener progressListener) throws Exception;
	
	void deletePodcastChannel(String id, Context context, ProgressListener progressListener) throws Exception;
	
	void downloadPodcastEpisode(String id, Context context, ProgressListener progressListener) throws Exception;
	
	void deletePodcastEpisode(String id, Context context, ProgressListener progressListener) throws Exception;

	MusicDirectory getVideos(boolean refresh, Context context, ProgressListener progressListener) throws Exception;

}