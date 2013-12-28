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
import java.io.Reader;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import github.madmarty.madsonic.domain.Artist;
import github.madmarty.madsonic.domain.Genre;
import github.madmarty.madsonic.domain.Indexes;
import github.madmarty.madsonic.domain.JukeboxStatus;
import github.madmarty.madsonic.domain.Lyrics;
import github.madmarty.madsonic.domain.MusicDirectory;
import github.madmarty.madsonic.domain.MusicFolder;
import github.madmarty.madsonic.domain.Playlist;
import github.madmarty.madsonic.domain.SearchCritera;
import github.madmarty.madsonic.domain.SearchResult;
import github.madmarty.madsonic.service.parser.PlaylistParser;
import github.madmarty.madsonic.util.Constants;
import github.madmarty.madsonic.util.FileUtil;
import github.madmarty.madsonic.util.ProgressListener;
import github.madmarty.madsonic.util.Util;

/**
 * @author Sindre Mehus
 */
public class OfflineMusicService extends RESTMusicService {
	private static final String TAG = OfflineMusicService.class.getSimpleName();
	
    @Override
    public boolean isLicenseValid(Context context, ProgressListener progressListener) throws Exception {
        return true;
    }

    @Override
    public Indexes getIndexes(String musicFolderId, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        List<Artist> artists = new ArrayList<Artist>();
        File root = FileUtil.getMusicDirectory(context);
        for (File file : FileUtil.listFiles(root)) {
            if (file.isDirectory()) {
                Artist artist = new Artist();
                artist.setId(file.getPath());
                artist.setIndex(file.getName().substring(0, 1));
                artist.setName(file.getName());
                artists.add(artist);
            }
        }
		
		SharedPreferences prefs = Util.getPreferences(context);
		String ignoredArticlesString = prefs.getString(Constants.CACHE_KEY_IGNORE, "The El La Los Las Le Les");
		final String[] ignoredArticles = ignoredArticlesString.split(" ");
		
		Collections.sort(artists, new Comparator<Artist>() {
			public int compare(Artist lhsArtist, Artist rhsArtist) {
				String lhs = lhsArtist.getName().toLowerCase();
				String rhs = rhsArtist.getName().toLowerCase();
				
				char lhs1 = lhs.charAt(0);
				char rhs1 = rhs.charAt(0);
				
				if(Character.isDigit(lhs1) && !Character.isDigit(rhs1)) {
					return 1;
				} else if(Character.isDigit(rhs1) && !Character.isDigit(lhs1)) {
					return -1;
				}
				
				for(String article: ignoredArticles) {
					int index = lhs.indexOf(article.toLowerCase());
					if(index == 0) {
						lhs = lhs.substring(article.length() + 1);
					}
					index = rhs.indexOf(article.toLowerCase());
					if(index == 0) {
						rhs = rhs.substring(article.length() + 1);
					}
				}
				
				return lhs.compareTo(rhs);
			}
		});
		
        return new Indexes(0L, Collections.<Artist>emptyList(), artists);
    }

    @Override
    public MusicDirectory getMusicDirectory(String id, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        File dir = new File(id);
        MusicDirectory result = new MusicDirectory();
        result.setName(dir.getName());

        Set<String> names = new HashSet<String>();

        for (File file : FileUtil.listMediaFiles(dir)) {
            String name = getName(file);
            if (name != null & !names.contains(name)) {
                names.add(name);
                result.addChild(createEntry(context, file, name));
            }
        }
        return result;
    }

    private String getName(File file) {
        String name = file.getName();
        if (file.isDirectory()) {
            return name;
        }

        if (name.endsWith(".partial") || name.contains(".partial.") || name.equals(Constants.ALBUM_ART_FILE)) {
            return null;
        }

        name = name.replace(".complete", "");
        return FileUtil.getBaseName(name);
    }

    private MusicDirectory.Entry createEntry(Context context, File file, String name) {
        MusicDirectory.Entry entry = new MusicDirectory.Entry();
        entry.setDirectory(file.isDirectory());
        entry.setId(file.getPath());
        entry.setParent(file.getParent());
        entry.setSize(file.length());
        String root = FileUtil.getMusicDirectory(context).getPath();
        entry.setPath(file.getPath().replaceFirst("^" + root + "/" , ""));
		String title = name;
        if (file.isFile()) {
            entry.setArtist(file.getParentFile().getParentFile().getName());
            entry.setAlbum(file.getParentFile().getName());
			
			int index = name.indexOf('-');
			if(index != -1) {
				try {
					entry.setTrack(Integer.parseInt(name.substring(0, index)));
					title = title.substring(index + 1);
				} catch(Exception e) {
					// Failed parseInt, just means track filled out
				}
			}
        }
		
        entry.setTitle(title);
        entry.setSuffix(FileUtil.getExtension(file.getName().replace(".complete", "")));

        File albumArt = FileUtil.getAlbumArtFile(context, entry);
        if (albumArt.exists()) {
            entry.setCoverArt(albumArt.getPath());
        }
		if(FileUtil.isVideoFile(file)) {
			entry.setVideo(true);
		}
        return entry;
    }

    @Override
    public Bitmap getCoverArt(Context context, MusicDirectory.Entry entry, int size, int saveSize, ProgressListener progressListener) throws Exception {
		try {
			return FileUtil.getAlbumArtBitmap(context, entry, size);
		} catch(Exception e) {
			return null;
		}
    }

    @Override
    public List<MusicFolder> getMusicFolders(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Music folders not available in offline mode");
    }

    @Override
    public SearchResult search(SearchCritera criteria, Context context, ProgressListener progressListener) throws Exception {
		List<Artist> artists = new ArrayList<Artist>();
		List<MusicDirectory.Entry> albums = new ArrayList<MusicDirectory.Entry>();
		List<MusicDirectory.Entry> songs = new ArrayList<MusicDirectory.Entry>();
        File root = FileUtil.getMusicDirectory(context);
        for (File artistFile : FileUtil.listFiles(root)) {
			String artistName = artistFile.getName();
            if (artistFile.isDirectory()) {
				if(matchCriteria(criteria, artistName)) {
					Artist artist = new Artist();
					artist.setId(artistFile.getPath());
					artist.setIndex(artistFile.getName().substring(0, 1));
					artist.setName(artistName);
					artists.add(artist);
				}
				
				recursiveAlbumSearch(artistName, artistFile, criteria, context, albums, songs);
            }
        }
		
		return new SearchResult(artists, albums, songs);
    }
	
	private void recursiveAlbumSearch(String artistName, File file, SearchCritera criteria, Context context, List<MusicDirectory.Entry> albums, List<MusicDirectory.Entry> songs) {
		for(File albumFile : FileUtil.listMediaFiles(file)) {
			if(albumFile.isDirectory()) {
				String albumName = getName(albumFile);
				if(matchCriteria(criteria, albumName)) {
					MusicDirectory.Entry album = createEntry(context, albumFile, albumName);
					album.setArtist(artistName);
					albums.add(album);
				}

				for(File songFile : FileUtil.listMediaFiles(albumFile)) {
					String songName = getName(songFile);
					if(songFile.isDirectory()) {
						recursiveAlbumSearch(artistName, songFile, criteria, context, albums, songs);
					}
					else if(matchCriteria(criteria, songName)){
						MusicDirectory.Entry song = createEntry(context, albumFile, songName);
						song.setArtist(artistName);
						song.setAlbum(albumName);
						songs.add(song);
					}
				}
			}
			else {
				String songName = getName(albumFile);
				if(matchCriteria(criteria, songName)) {
					MusicDirectory.Entry song = createEntry(context, albumFile, songName);
					song.setArtist(artistName);
					song.setAlbum(songName);
					songs.add(song);
				}
			}
		}
	}
	private boolean matchCriteria(SearchCritera criteria, String name) {
		String query = criteria.getQuery().toLowerCase();
		String[] parts = query.split(" ");
		name = name.toLowerCase();
		
		for(String part : parts) {
			if(name.indexOf(part) != -1) {
				return true;
			}
		}
		
		return false;
	}

    @Override
    public void star(String id, boolean star, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Starring not available in offline mode");
    }

    @Override
    public URL createShare(String id, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Sharing not available in offline mode");
    }

    @Override
    public List<Playlist> getPlaylists(boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        List<Playlist> playlists = new ArrayList<Playlist>();
        File root = FileUtil.getPlaylistDirectory();
        for (File file : FileUtil.listFiles(root)) {
			Playlist playlist = new Playlist(file.getName(), file.getName());
			playlists.add(playlist);
        }
        return playlists;
    }

    @Override
    public MusicDirectory getPlaylist(String id, String name, Context context, ProgressListener progressListener) throws Exception {
		DownloadService downloadService = DownloadServiceImpl.getInstance();
        if (downloadService == null) {
            return new MusicDirectory();
        }
		
        Reader reader = null;
		try {
			reader = new FileReader(FileUtil.getPlaylistFile(name));
			MusicDirectory fullList = new PlaylistParser(context).parse(reader, progressListener);
			MusicDirectory playlist = new MusicDirectory();
			for(MusicDirectory.Entry song: fullList.getChildren()) {
				DownloadFile downloadFile = downloadService.forSong(song);
				File completeFile = downloadFile.getCompleteFile();
				if(completeFile.exists()) {
					playlist.addChild(song);
				}
			}
			return playlist;
		} finally {
			Util.close(reader);
		}
    }

    @Override
    public void createPlaylist(String id, String name, List<MusicDirectory.Entry> entries, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Playlists not available in offline mode");
    }

    @Override
	public void overwritePlaylist(String id, String name, int toRemove, List<MusicDirectory.Entry> toAdd, Context context, ProgressListener progressListener) throws Exception {
		throw new OfflineException("Overwriting playlist not available in offline mode");
	}
	
    @Override
    public Lyrics getLyrics(String artist, String title, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Lyrics not available in offline mode");
    }

    @Override
    public void scrobble(String id, boolean submission, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Scrobbling not available in offline mode");
    }

    @Override
    public MusicDirectory getAlbumList(String type, int size, int offset, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Album lists not available in offline mode");
    }
	
    @Override
    public String getVideoUrl(int maxBitrate, Context context, String id, boolean useFlash) {
        return null;
    }
    @Override
    public String getVideoUrl(int maxBitrate, Context context, String id) {
        return null;
    }
	
	@Override
    public String getVideoStreamUrl(int maxBitrate, Context context, String id) {
        return null;
    }

    @Override
    public JukeboxStatus updateJukeboxPlaylist(List<String> ids, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }

    @Override
    public JukeboxStatus skipJukebox(int index, int offsetSeconds, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }

    @Override
    public JukeboxStatus stopJukebox(Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }

    @Override
    public JukeboxStatus startJukebox(Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }

    @Override
    public JukeboxStatus getJukeboxStatus(Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }

    @Override
    public JukeboxStatus setJukeboxGain(float gain, Context context, ProgressListener progressListener) throws Exception {
        throw new OfflineException("Jukebox not available in offline mode");
    }
    
    @Override
    public void setStarred(String id, boolean starred, Context context, ProgressListener progressListener) throws Exception {
    	throw new OfflineException("Starring not available in offline mode");
    }
	
    @Override
    public SearchResult getStarred(Context context, ProgressListener progressListener) throws Exception {
    	throw new OfflineException("Starred not available in offline mode");
    }
    @Override
    public MusicDirectory getRandomSongs(int size, Context context, ProgressListener progressListener) throws Exception {
        File root = FileUtil.getMusicDirectory(context);
        List<File> children = new LinkedList<File>();
        listFilesRecursively(root, children);
        MusicDirectory result = new MusicDirectory();

        if (children.isEmpty()) {
            return result;
        }
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            File file = children.get(random.nextInt(children.size()));
            result.addChild(createEntry(context, file, getName(file)));
        }
        return result;
    }
    
    @Override
    public MusicDirectory getSongsByGenre(String genre, int count, int offset, Context context, ProgressListener progressListener) throws Exception {
    	throw new OfflineException("Getting Songs By Genre not available in offline mode");
    }
    
    @Override
    public List<Genre> getGenres(Context context, ProgressListener progressListener) throws Exception {
    	throw new OfflineException("Getting Genres not available in offline mode");
    }

	@Override
	public MusicDirectory getPodcastEpisodes(boolean refresh, String id, Context context, ProgressListener progressListener) throws Exception {
		return getMusicDirectory(FileUtil.getPodcastDirectory(context, id).getPath(), false, context, progressListener);
	}
	
	@Override
	public void refreshPodcasts(Context context, ProgressListener progressListener) throws Exception {
		throw new OfflineException("Getting Podcasts not available in offline mode");
	}
	
	@Override
	public void createPodcastChannel(String url, Context context, ProgressListener progressListener) throws Exception{
		throw new OfflineException("Getting Podcasts not available in offline mode");
	}
	
	@Override
	public void deletePodcastChannel(String id, Context context, ProgressListener progressListener) throws Exception{
		throw new OfflineException("Getting Podcasts not available in offline mode");
	}
	
	@Override
	public void downloadPodcastEpisode(String id, Context context, ProgressListener progressListener) throws Exception{
		throw new OfflineException("Getting Podcasts not available in offline mode");
	}
	
	@Override
	public void deletePodcastEpisode(String id, Context context, ProgressListener progressListener) throws Exception{
		throw new OfflineException("Getting Podcasts not available in offline mode");
	}
    

    private void listFilesRecursively(File parent, List<File> children) {
        for (File file : FileUtil.listMediaFiles(parent)) {
            if (file.isFile()) {
                children.add(file);
            } else {
                listFilesRecursively(file, children);
            }
        }
    }
}
