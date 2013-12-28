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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;

import android.content.Context;
import android.graphics.Bitmap;
import github.madmarty.madsonic.domain.Artist;
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
import github.madmarty.madsonic.util.Constants;
import github.madmarty.madsonic.util.FileUtil;
import github.madmarty.madsonic.util.LRUCache;
import github.madmarty.madsonic.util.ProgressListener;
import github.madmarty.madsonic.util.TimeLimitedCache;
import github.madmarty.madsonic.util.Util;

/**
 * @author Sindre Mehus
 */
public class CachedMusicService implements MusicService {

    private static final int MUSIC_DIR_CACHE_SIZE = 20;
    private static final int TTL_MUSIC_DIR = 5 * 60; // Five minutes

    private final MusicService musicService;
    private final LRUCache<String, TimeLimitedCache<MusicDirectory>> cachedMusicDirectories;
    private final TimeLimitedCache<Boolean> cachedLicenseValid = new TimeLimitedCache<Boolean>(120, TimeUnit.SECONDS);
    private final TimeLimitedCache<Indexes> cachedIndexes = new TimeLimitedCache<Indexes>(60 * 60, TimeUnit.SECONDS);
    private final TimeLimitedCache<SearchResult> cachedStarred = new TimeLimitedCache<SearchResult>(60 * 60, TimeUnit.SECONDS);
    private final TimeLimitedCache<List<Playlist>> cachedPlaylists = new TimeLimitedCache<List<Playlist>>(600, TimeUnit.SECONDS);
    private final TimeLimitedCache<List<MusicFolder>> cachedMusicFolders = new TimeLimitedCache<List<MusicFolder>>(10 * 3600, TimeUnit.SECONDS);
    private final TimeLimitedCache<List<Genre>> cachedGenres = new TimeLimitedCache<List<Genre>>(10 * 3600, TimeUnit.SECONDS);
    private final TimeLimitedCache<List<Genre>> cachedArtistGenres = new TimeLimitedCache<List<Genre>>(10 * 3600, TimeUnit.SECONDS);
	private final TimeLimitedCache<List<PodcastChannel>> cachedPodcastChannels = new TimeLimitedCache<List<PodcastChannel>>(10 * 3600, TimeUnit.SECONDS);

    private String restUrl;

    public CachedMusicService(MusicService musicService) {
        this.musicService = musicService;
        cachedMusicDirectories = new LRUCache<String, TimeLimitedCache<MusicDirectory>>(MUSIC_DIR_CACHE_SIZE);
    }

    @Override
    public void ping(Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        musicService.ping(context, progressListener);
    }
    

    public Version getAPIVersion(Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        Version result = new Version("1.0.0");
        result = musicService.getAPIVersion(context, progressListener);
        return result;
	}

    @Override
    public boolean isLicenseValid(Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        Boolean result = cachedLicenseValid.get();
        if (result == null) {
            result = musicService.isLicenseValid(context, progressListener);
            cachedLicenseValid.set(result, result ? 30L * 60L : 2L * 60L, TimeUnit.SECONDS);
        }
        return result;
    }

    @Override
    public void startRescan(Context context, ProgressListener progressListener) throws Exception {
        musicService.startRescan(context, progressListener);
    }  
    
    @Override
    public List<MusicFolder> getMusicFolders(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        if (refresh) {
            cachedMusicFolders.clear();
        }
        List<MusicFolder> result = cachedMusicFolders.get();
        if (result == null) {
            result = musicService.getMusicFolders(refresh, context, progressListener);
            cachedMusicFolders.set(result);
        }
        return result;
    }

    @Override
    public Indexes getIndexes(String musicFolderId, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        if (refresh) {
            cachedIndexes.clear();
            cachedMusicFolders.clear();
            cachedMusicDirectories.clear();
        }
        Indexes indexes = cachedIndexes.get();
        if (indexes == null) {
            indexes = musicService.getIndexes(musicFolderId, refresh, context, progressListener);
            populateStarred(indexes.getArtists(), context, progressListener);
            cachedIndexes.set(indexes);
        }
        return indexes;
    }

    @Override
    public MusicDirectory getMusicDirectory(String id, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        TimeLimitedCache<MusicDirectory> cache = refresh ? null : cachedMusicDirectories.get(id);
        MusicDirectory dir = cache == null ? null : cache.get();
        if (dir == null) {
            dir = musicService.getMusicDirectory(id, refresh, context, progressListener);
            cache = new TimeLimitedCache<MusicDirectory>(TTL_MUSIC_DIR, TimeUnit.SECONDS);
            cache.set(dir);
            cachedMusicDirectories.put(id, cache);
        }
        populateStarred(dir, context, progressListener);
        return dir;
    }

    private void populateStarred(MusicDirectory dir, Context context, ProgressListener progressListener) throws Exception {
        // MusicDirectory.starred was added to the REST API in 1.10, so for backward compatibility
        // we have to emulate it.
        if (Util.isServerCompatibleTo(context, "1.10.1")) {
            return;
        }

        // getStarred REST method was added in 1.8.  Ignore starring if server is older.
        if (!Util.isServerCompatibleTo(context, "1.8")) {
            return;
        }
        
        for (MusicDirectory.Entry starredDir : getStarred(context, progressListener).getAlbums()) {
            if (dir.getId().equals(starredDir.getId())) {
                dir.setStarred(true);
                return;
            }
        }
    }

    private void populateStarred(List<Artist> artists, Context context, ProgressListener progressListener) throws Exception {
        // Artist.starred was added to the REST API in 1.10, so for backward compatibility
        // we have to emulate it.
        if (Util.isServerCompatibleTo(context, "1.10.1")) {
            return;
        }
        
        // getStarred REST method was added in 1.8.  Ignore starring if server is older.
        if (!Util.isServerCompatibleTo(context, "1.8")) {
            return;
        }        
        
        List<Artist> starredArtists = getStarred(context, progressListener).getArtists();
        Set<String> starredArtistIds = new HashSet<String>();
        for (Artist starredArtist : starredArtists) {
            starredArtistIds.add(starredArtist.getId());
        }
        for (Artist artist : artists) {
            artist.setStarred(starredArtistIds.contains(artist.getId()));
        }
    }

    @Override
    public SearchResult search(SearchCritera criteria, Context context, ProgressListener progressListener) throws Exception {
        return musicService.search(criteria, context, progressListener);
    }

    @Override
    public MusicDirectory getPlaylist(String id, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getPlaylist(id, context, progressListener);
    }    
    
    @Override
    public URL createShare(String id, Context context, ProgressListener progressListener) throws Exception {
        return musicService.createShare(id, context, progressListener);
    }

    @Override
    public MusicDirectory getPlaylist(String id, String name, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getPlaylist(id, name, context, progressListener);
    }

    @Override
    public List<Playlist> getPlaylists(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        List<Playlist> result = refresh ? null : cachedPlaylists.get();
        if (result == null) {
            result = musicService.getPlaylists(refresh, context, progressListener);
            cachedPlaylists.set(result);
        }
        return result;
    }

    @Override
    public void createPlaylist(String id, String name, List<MusicDirectory.Entry> entries, Context context, ProgressListener progressListener) throws Exception {
		cachedPlaylists.clear();
        musicService.createPlaylist(id, name, entries, context, progressListener);
    }

	@Override
	public void deletePlaylist(String id, Context context, ProgressListener progressListener) throws Exception {
		musicService.deletePlaylist(id, context, progressListener);
	}
	
	@Override
	public void addToPlaylist(String id, List<MusicDirectory.Entry> toAdd, Context context, ProgressListener progressListener) throws Exception {
		musicService.addToPlaylist(id, toAdd, context, progressListener);
	}
	
	@Override
	public void removeFromPlaylist(String id, List<Integer> toRemove, Context context, ProgressListener progressListener) throws Exception {
		musicService.removeFromPlaylist(id, toRemove, context, progressListener);
	}
	
	@Override
	public void overwritePlaylist(String id, String name, int toRemove, List<MusicDirectory.Entry> toAdd, Context context, ProgressListener progressListener) throws Exception {
		musicService.overwritePlaylist(id, name, toRemove, toAdd, context, progressListener);
	}
	
	@Override
	public void updatePlaylist(String id, String name, String comment, boolean pub, Context context, ProgressListener progressListener) throws Exception {
		musicService.updatePlaylist(id, name, comment, pub, context, progressListener);
	}    
    
    @Deprecated
    public Lyrics getLyrics(String artist, String title, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getLyrics(artist, title, context, progressListener);
    }

    @Override
    public Lyrics getLyrics(String id, String artist, String title, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getLyrics(id, artist, title, context, progressListener);
    }
    
    @Override
    public void scrobble(String id, boolean submission, Context context, ProgressListener progressListener) throws Exception {
        musicService.scrobble(id, submission, context, progressListener);
    }

    @Override
    public MusicDirectory getAlbumList(String type, int size, int offset, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getAlbumList(type, size, offset, context, progressListener);
    }


	@Override
	public MusicDirectory getLastplayedSongs(int size, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getLastplayedSongs(size, context, progressListener);
	}

	@Override
	public MusicDirectory getNewaddedSongs(int size, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getNewaddedSongs(size, context, progressListener);
	}    
    
    @Override
    public MusicDirectory getRandomSongs(int size, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getRandomSongs(size, context, progressListener);
    }
    
	@Override
	public MusicDirectory getSharedSongs(String sharename, Context context, ProgressListener progressListener) throws Exception {
        return musicService.getSharedSongs(sharename, context, progressListener);
	}
    
    
    @Override
    public SearchResult getStarred(Context context, ProgressListener progressListener) throws Exception {
    	return musicService.getStarred(context, progressListener);
    }

    @Override
    public void star(String id, boolean star, Context context, ProgressListener progressListener) throws Exception {
        cachedStarred.clear();
        musicService.star(id, star, context, progressListener);
    }

    @Override
    public Bitmap getCoverArt(Context context, MusicDirectory.Entry entry, int size, int saveSize, ProgressListener progressListener) throws Exception {
        return musicService.getCoverArt(context, entry, size, saveSize, progressListener);
    }

    @Override
    public HttpResponse getDownloadInputStream(Context context, MusicDirectory.Entry song, long offset, int maxBitrate, CancellableTask task) throws Exception {
        return musicService.getDownloadInputStream(context, song, offset, maxBitrate, task);
    }

    @Override
    public Version getLocalVersion(Context context) throws Exception {
        return musicService.getLocalVersion(context);
    }

    @Override
    public Version getLatestVersion(Context context, ProgressListener progressListener) throws Exception {
        return musicService.getLatestVersion(context, progressListener);
    }

    @Override
    public String getVideoUrl(int maxBitrate, Context context, String id, boolean useFlash) throws Exception {
        return musicService.getVideoUrl(maxBitrate, context, id, useFlash);
    }

    @Override
    public String getVideoUrl(int maxBitrate, Context context, String id) {
        return musicService.getVideoUrl(maxBitrate, context, id);
    }
	
	@Override
    public String getVideoStreamUrl(int maxBitrate, Context context, String id) {
        return musicService.getVideoStreamUrl(maxBitrate, context, id);
    }

    @Override
    public JukeboxStatus updateJukeboxPlaylist(List<String> ids, Context context, ProgressListener progressListener) throws Exception {
        return musicService.updateJukeboxPlaylist(ids, context, progressListener);
    }

    @Override
    public JukeboxStatus skipJukebox(int index, int offsetSeconds, Context context, ProgressListener progressListener) throws Exception {
        return musicService.skipJukebox(index, offsetSeconds, context, progressListener);
    }

    @Override
    public JukeboxStatus stopJukebox(Context context, ProgressListener progressListener) throws Exception {
        return musicService.stopJukebox(context, progressListener);
    }

    @Override
    public JukeboxStatus startJukebox(Context context, ProgressListener progressListener) throws Exception {
        return musicService.startJukebox(context, progressListener);
    }

    @Override
    public JukeboxStatus getJukeboxStatus(Context context, ProgressListener progressListener) throws Exception {
        return musicService.getJukeboxStatus(context, progressListener);
    }

    @Override
    public JukeboxStatus setJukeboxGain(float gain, Context context, ProgressListener progressListener) throws Exception {
        return musicService.setJukeboxGain(gain, context, progressListener);
    }
    
	@Override
	public void setStarred(String id, boolean starred, Context context, ProgressListener progressListener) throws Exception {
		musicService.setStarred(id, starred, context, progressListener);
	}

    private void checkSettingsChanged(Context context) {
        String newUrl = Util.getRestUrl(context, null);
        if (!Util.equals(newUrl, restUrl)) {
            cachedMusicFolders.clear();
            cachedMusicDirectories.clear();
            cachedLicenseValid.clear();
            cachedIndexes.clear();
            cachedStarred.clear();
            cachedPlaylists.clear();
            restUrl = newUrl;
        }
    }
	@Override
	public List<Genre> getGenres(Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        List<Genre> result = cachedGenres.get();
        if (result == null) {
            result = musicService.getGenres(context, progressListener);
            cachedGenres.set(result);
        }
        return result;
	}

	@Override
	public List<Genre> getArtistsGenres(Context context, ProgressListener progressListener) throws Exception {
        checkSettingsChanged(context);
        List<Genre> result = cachedArtistGenres.get();
        if (result == null) {
            result = musicService.getArtistsGenres(context, progressListener);
            cachedArtistGenres.set(result);
        }
        return result;
    }
	
	
	@Override
	public MusicDirectory getSongsByGenre(String genre, int count, int offset, Context context, ProgressListener progressListener) throws Exception {
		return musicService.getSongsByGenre(genre, count, offset, context, progressListener);
	}
	
	@Override
	public MusicDirectory getArtistsByGenre(String genre, int count, int offset, Context context, ProgressListener progressListener) throws Exception {
		return musicService.getArtistsByGenre(genre, count, offset, context, progressListener);
	}	
 
	@Override
	public List<ChatMessage> getChatMessages(Long since, Context context, ProgressListener progressListener) throws Exception {
		return musicService.getChatMessages(since, context, progressListener);
	}

	@Override
	public void addChatMessage(String message, Context context, ProgressListener progressListener) throws Exception {
		musicService.addChatMessage(message, context, progressListener);
	}

	@Override
	public List<PodcastChannel> getPodcastChannels(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
		checkSettingsChanged(context);
		List<PodcastChannel> result = refresh ? null : cachedPodcastChannels.get();

		if (result == null) {
			if(!refresh) {
				//FIXME:TODO
//				result = FileUtil.deserialize(context, getCacheName(context, "podcast"), ArrayList.class);
			}
			
			if(result == null) {
				result = musicService.getPodcastChannels(refresh, context, progressListener);
				FileUtil.serialize(context, new ArrayList<PodcastChannel>(result), getCacheName(context, "podcast"));
			}
			cachedPodcastChannels.set(result);
		}

		return result;
	}

	@Override
	public MusicDirectory getVideos(boolean refresh, Context context, ProgressListener progressListener) throws Exception
	{
		checkSettingsChanged(context);
		TimeLimitedCache<MusicDirectory> cache = refresh ? null : cachedMusicDirectories.get(Constants.INTENT_EXTRA_NAME_VIDEOS);

		MusicDirectory dir = cache == null ? null : cache.get();

		if (dir == null)
		{
			dir = musicService.getVideos(refresh, context, progressListener);
			cache = new TimeLimitedCache<MusicDirectory>(Util.getDirectoryCacheTime(context), TimeUnit.SECONDS);
			cache.set(dir);
			cachedMusicDirectories.put(Constants.INTENT_EXTRA_NAME_VIDEOS, cache);
		}

		return dir;
	}
	
	@Override
	public MusicDirectory getPodcastEpisodes(boolean refresh, String id, Context context, ProgressListener progressListener) throws Exception {
		return musicService.getPodcastEpisodes(refresh, id, context, progressListener);
	}
	
	@Override
	public void refreshPodcasts(Context context, ProgressListener progressListener) throws Exception {
		musicService.refreshPodcasts(context, progressListener);
	}
	
	@Override
	public void createPodcastChannel(String url, Context context, ProgressListener progressListener) throws Exception{
		Util.delete(new File(context.getCacheDir(), getCacheName(context, "podcast")));
		cachedPodcastChannels.clear();
		musicService.createPodcastChannel(url, context, progressListener);
	}
	
	@Override
	public void deletePodcastChannel(String id, Context context, ProgressListener progressListener) throws Exception{
		Util.delete(new File(context.getCacheDir(), getCacheName(context, "podcast")));
		cachedPodcastChannels.clear();
		musicService.deletePodcastChannel(id, context, progressListener);
	}
	
	@Override
	public void downloadPodcastEpisode(String id, Context context, ProgressListener progressListener) throws Exception{
		musicService.downloadPodcastEpisode(id, context, progressListener);
	}
	
	@Override
	public void deletePodcastEpisode(String id, Context context, ProgressListener progressListener) throws Exception{
		musicService.deletePodcastEpisode(id, context, progressListener);
	}
  	private String getCacheName(Context context, String name, String id) {
  		String s = Util.getRestUrl(context, null) + id;
  		return name + "-" + s.hashCode() + ".ser";
  	}
  	private String getCacheName(Context context, String name) {
  		String s = Util.getRestUrl(context, null);
  		return name + "-" + s.hashCode() + ".ser";
  	}



}
