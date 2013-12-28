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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import github.madmarty.madsonic.util.Logger;

import github.madmarty.madsonic.domain.MusicDirectory;
import github.madmarty.madsonic.util.CancellableTask;
import github.madmarty.madsonic.util.FileUtil;
import github.madmarty.madsonic.util.Util;
import github.madmarty.madsonic.util.CacheCleaner;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class DownloadFile {

    private static final Logger LOG = new Logger(DownloadFile.class);

    private final Context context;
    private final MusicDirectory.Entry song;
    private final File partialFile;
    private final File completeFile;
    private final File saveFile;

    private final MediaStoreService mediaStoreService;
    private CancellableTask downloadTask;
    private boolean save;
    private boolean failed;
    private int bitRate;
	private boolean isPlaying = false;
	private boolean saveWhenDone = false;
	private boolean completeWhenDone = false;

    public DownloadFile(Context context, MusicDirectory.Entry song, boolean save) {
        this.context = context;
        this.song = song;
        this.save = save;
        saveFile = FileUtil.getSongFile(context, song);
        bitRate = Util.getMaxBitrate(context);
        partialFile = new File(saveFile.getParent(), FileUtil.getBaseName(saveFile.getName()) +
                ".partial." + FileUtil.getExtension(saveFile.getName()));
        completeFile = new File(saveFile.getParent(), FileUtil.getBaseName(saveFile.getName()) +
                ".complete." + FileUtil.getExtension(saveFile.getName()));
        mediaStoreService = new MediaStoreService(context);
    }

    public MusicDirectory.Entry getSong() {
        return song;
    }

    /**
     * Returns the effective bit rate.
     */
    public int getBitRate() {
		if(!partialFile.exists()) {
			bitRate = Util.getMaxBitrate(context);
		}
        if (bitRate > 0) {
            return bitRate;
        }
        return song.getBitRate() == null ? 160 : song.getBitRate();
    }
    
    public int getBufferLength() {
    	return Util.getBufferLength(this.context);
    }

    public synchronized void download() {
        FileUtil.createDirectoryForParent(saveFile);
        failed = false;
		if(!partialFile.exists()) {
			bitRate = Util.getMaxBitrate(context);
		}
        downloadTask = new DownloadTask();
        downloadTask.start();
    }

    public synchronized void cancelDownload() {
        if (downloadTask != null) {
            downloadTask.cancel();
        }
    }

    public File getCompleteFile() {
        if (saveFile.exists()) {
            return saveFile;
        }

        if (completeFile.exists()) {
            return completeFile;
        }

        return saveFile;
    }

    public File getPartialFile() {
        return partialFile;
    }

    public boolean isSaved() {
        return saveFile.exists();
    }

    public synchronized boolean isCompleteFileAvailable() {
        return saveFile.exists() || completeFile.exists();
    }

    public synchronized boolean isWorkDone() {
        return saveFile.exists() || (completeFile.exists() && !save) || saveWhenDone || completeWhenDone;
    }

    public synchronized boolean isDownloading() {
        return downloadTask != null && downloadTask.isRunning();
    }

    public synchronized boolean isDownloadCancelled() {
        return downloadTask != null && downloadTask.isCancelled();
    }

    public boolean shouldSave() {
        return save;
    }

    public boolean isFailed() {
        return failed;
    }

    public void delete() {
        cancelDownload();
        Util.delete(partialFile);
        Util.delete(completeFile);
        Util.delete(saveFile);
        mediaStoreService.deleteFromMediaStore(this);
    }

    public void unpin() {
        if (saveFile.exists()) {
            saveFile.renameTo(completeFile);
        }
    }

    public boolean cleanup() {
        boolean ok = true;
        if (completeFile.exists() || saveFile.exists()) {
            ok = Util.delete(partialFile);
        }
        if (saveFile.exists()) {
            ok &= Util.delete(completeFile);
        }
        return ok;
    }

    // In support of LRU caching.
    public void updateModificationDate() {
        updateModificationDate(saveFile);
        updateModificationDate(partialFile);
        updateModificationDate(completeFile);
    }

    private void updateModificationDate(File file) {
        if (file.exists()) {
            boolean ok = file.setLastModified(System.currentTimeMillis());
            if (!ok) {
                LOG.warn("Failed to set last-modified date on " + file);
            }
        }
    }
	
	public void setPlaying(boolean isPlaying) {
		try {
			if(saveWhenDone && isPlaying == false) {
				Util.renameFile(completeFile, saveFile);
				saveWhenDone = false;
			} else if(completeWhenDone && isPlaying == false) {
				if(save) {
					Util.renameFile(partialFile, saveFile);
                    mediaStoreService.saveInMediaStore(DownloadFile.this);
				} else {
					Util.renameFile(partialFile, completeFile);
				}
				completeWhenDone = false;
			}
		} catch(IOException ex) {
			LOG.warn("Failed to rename file " + completeFile + " to " + saveFile);
		}
		
		this.isPlaying = isPlaying;
	}
	
	public boolean getPlaying() {
		return isPlaying;
	}

    @Override
    public String toString() {
        return "DownloadFile (" + song + ")";
    }

    private class DownloadTask extends CancellableTask {

        @SuppressWarnings("deprecation")
		@Override
        public void execute() {

            InputStream in = null;
            FileOutputStream out = null;
            PowerManager.WakeLock wakeLock = null;
			WifiManager.WifiLock wifiLock = null;
            try {

                if (Util.isScreenLitOnDownload(context)) {
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, toString());
                    wakeLock.acquire();
                    LOG.info("Acquired wake lock " + wakeLock);
                }
				
				wifiLock = Util.createWifiLock(context, toString());
				wifiLock.acquire();
                LOG.info("Acquired wifi lock " + wifiLock);

                if (saveFile.exists()) {
                    LOG.info(saveFile + " already exists. Skipping.");
                    return;
                }
                if (completeFile.exists()) {
                    if (save) {
						if(isPlaying) {
							saveWhenDone = true;
						} else {
							Util.renameFile(completeFile, saveFile);
						}
                    } else {
                        LOG.info(completeFile + " already exists. Skipping.");
                    }
                    return;
                }

                MusicService musicService = MusicServiceFactory.getMusicService(context);

				// Some devices seem to throw error on partial file which doesn't exist
				boolean compare;
				try {
					compare = (bitRate == 0) || (song.getDuration() == 0) || (partialFile.length() == 0) || (bitRate * song.getDuration() * 1000 / 8) > partialFile.length();
				} catch(Exception e) {
					compare = true;
				}
				if(compare) {
					// Attempt partial HTTP GET, appending to the file if it exists.
					HttpResponse response = musicService.getDownloadInputStream(context, song, partialFile.length(), bitRate, DownloadTask.this);
					in = response.getEntity().getContent();
					boolean partial = response.getStatusLine().getStatusCode() == HttpStatus.SC_PARTIAL_CONTENT;
					if (partial) {
                    LOG.info("Executed partial HTTP GET, skipping " + partialFile.length() + " bytes");
					}

                out = new FileOutputStream(partialFile, partial);
                long n = copy(in, out);
                LOG.info("Downloaded " + n + " bytes to " + partialFile);
                out.flush();
                out.close();

                if (isCancelled()) {
                    throw new Exception("Download of '" + song + "' was cancelled");
                }

					downloadAndSaveCoverArt(musicService);
				}

				if(isPlaying) {
					completeWhenDone = true;
				} else {
					if(save) {
						Util.renameFile(partialFile, saveFile);
						mediaStoreService.saveInMediaStore(DownloadFile.this);
					} else {
						Util.renameFile(partialFile, completeFile);
					}
				}

            } catch (Exception x) {
                Util.close(out);
                Util.delete(completeFile);
                Util.delete(saveFile);
                if (!isCancelled()) {
                    failed = true;
                    LOG.warn("Failed to download '" + song + "'.", x);
                }

            } finally {
                Util.close(in);
                Util.close(out);
                if (wakeLock != null) {
                    wakeLock.release();
                    LOG.info("Released wake lock " + wakeLock);
                }
				if (wifiLock != null) {
					wifiLock.release();
                    LOG.info("Released wifi lock " + wifiLock);
				}
                new CacheCleaner(context, DownloadServiceImpl.getInstance()).clean();
            }
        }

        @Override
        public String toString() {
            return "DownloadTask (" + song + ")";
        }

        private void downloadAndSaveCoverArt(MusicService musicService) throws Exception {
            try {
                if (song.getCoverArt() != null) {
                    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                    int size = Math.min(metrics.widthPixels, metrics.heightPixels);
                    musicService.getCoverArt(context, song, size, size, null);
                }
            } catch (Exception x) {
                LOG.error("Failed to get cover art.", x);
            }
        }

        private long copy(final InputStream in, OutputStream out) throws IOException, InterruptedException {

            // Start a thread that will close the input stream if the task is
            // cancelled, thus causing the copy() method to return.
            new Thread() {
                @Override
                public void run() {
                    while (true) {
                        Util.sleepQuietly(3000L);
                        if (isCancelled()) {
                            Util.close(in);
                            return;
                        }
                        if (!isRunning()) {
                            return;
                        }
                    }
                }
            }.start();

            byte[] buffer = new byte[1024 * 16];
            long count = 0;
            int n;
            long lastLog = System.currentTimeMillis();

            while (!isCancelled() && (n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                count += n;

                long now = System.currentTimeMillis();
                if (now - lastLog > 3000L) {  // Only every so often.
                    LOG.info("Downloaded " + Util.formatBytes(count) + " of " + song);
                    lastLog = now;
                }
            }
            return count;
        }
    }
}
