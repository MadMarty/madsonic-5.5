/*
 * This file is part of Subsonic.
 *
 * Subsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Subsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2013 (C) Sindre Mehus
 */
package github.madmarty.madsonic.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import github.madmarty.madsonic.R;
import github.madmarty.madsonic.activity.VideoPlayerActivity;
import github.madmarty.madsonic.audiofx.EqualizerController;
import github.madmarty.madsonic.domain.MusicDirectory;
import github.madmarty.madsonic.service.MusicServiceFactory;

/**
 * @author Sindre Mehus
 * @version $Id: VideoPlayerType.java 3474 2013-05-23 17:09:33Z sindre_mehus $
 */
public enum VideoPlayerType {

    MX("mx") {
        @Override
        public void playVideo(final Activity activity, MusicDirectory.Entry entry) throws Exception {

            // Check if MX Player is installed.
            boolean installedAd = Util.isPackageInstalled(activity, PACKAGE_NAME_MX_AD);
            boolean installedPro = Util.isPackageInstalled(activity, PACKAGE_NAME_MX_PRO);

            if (!installedAd && !installedPro) {
                new AlertDialog.Builder(activity)
                        .setMessage(R.string.video_get_mx_player_text)
                        .setPositiveButton(R.string.video_get_mx_player_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                try {
                                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + PACKAGE_NAME_MX_AD)));
                                } catch (android.content.ActivityNotFoundException x) {
                                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + PACKAGE_NAME_MX_AD)));
                                }
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        })
                        .show();

            } else {
                // See documentation on https://sites.google.com/site/mxvpen/api
            	int maxBitrate = Util.getMaxVideoBitrate(activity);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setPackage(installedAd ? PACKAGE_NAME_MX_AD : PACKAGE_NAME_MX_PRO);
                intent.putExtra("title", entry.getTitle());
                intent.setDataAndType(Uri.parse(MusicServiceFactory.getMusicService(activity).getVideoUrl(maxBitrate, activity, entry.getId(), false)), "video/*");
                activity.startActivity(intent);
            }
        }
    },
    
    BS("bs") {
        @Override
        public void playVideo(final Activity activity, MusicDirectory.Entry entry) throws Exception {

            // Check if MX Player is installed.
            boolean installedAd = Util.isPackageInstalled(activity, PACKAGE_NAME_BS_FREE);
            boolean installedPro = Util.isPackageInstalled(activity, PACKAGE_NAME_BS_FULL);

            if (!installedAd && !installedPro) {
                new AlertDialog.Builder(activity)
                        .setMessage(R.string.video_get_bs_player_text)
                        .setPositiveButton(R.string.video_get_bs_player_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                try {
                                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + PACKAGE_NAME_BS_FREE)));
                                } catch (android.content.ActivityNotFoundException x) {
                                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + PACKAGE_NAME_BS_FREE)));
                                }
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        })
                        .show();

            } else {
                // See documentation on https://sites.google.com/site/mxvpen/api
            	int maxBitrate = Util.getMaxVideoBitrate(activity);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setPackage(installedAd ? PACKAGE_NAME_BS_FREE : PACKAGE_NAME_BS_FULL);
                intent.putExtra("title", entry.getTitle());
                intent.setDataAndType(Uri.parse(MusicServiceFactory.getMusicService(activity).getVideoUrl(maxBitrate, activity, entry.getId(), false)), "video/*");
                activity.startActivity(intent);
            }
        }
    },    

    VLC("vlc") {
        @Override
        public void playVideo(final Activity activity, MusicDirectory.Entry entry) throws Exception {

            // Check if MX Player is installed.
            boolean installedAd = Util.isPackageInstalled(activity, PACKAGE_NAME_VLC);

            if (!installedAd) {
                new AlertDialog.Builder(activity)
                        .setMessage(R.string.video_get_vlc_player_text)
                        .setPositiveButton(R.string.video_get_vlc_player_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                try {
                                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + PACKAGE_NAME_VLC)));
                                } catch (android.content.ActivityNotFoundException x) {
                                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + PACKAGE_NAME_VLC)));
                                }
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        })
                        .show();

            } else {
                // See documentation on https://sites.google.com/site/mxvpen/api
            	int maxBitrate = Util.getMaxVideoBitrate(activity);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setPackage(PACKAGE_NAME_VLC);
                intent.putExtra("title", entry.getTitle());
                intent.setDataAndType(Uri.parse(MusicServiceFactory.getMusicService(activity).getVideoUrl(maxBitrate, activity, entry.getId(), false)), "video/*");
                activity.startActivity(intent);
            }
        }
    },      
    
    FLASH("flash") {
        @Override
        public void playVideo(Activity activity, MusicDirectory.Entry entry) throws Exception {
        	int maxBitrate = Util.getMaxVideoBitrate(activity);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(MusicServiceFactory.getMusicService(activity).getVideoUrl(maxBitrate, activity, entry.getId(), true)));
            activity.startActivity(intent);
        }
    },
    
    
    INTERNAL("internal") {
        @Override
        public void playVideo(Activity activity, MusicDirectory.Entry entry) throws Exception {

            // Load custom video player
            Intent intent = new Intent(activity, VideoPlayerActivity.class);
        	int maxBitrate = Util.getMaxVideoBitrate(activity);
            try {
    			intent.putExtra("source", MusicServiceFactory.getMusicService(activity).getVideoUrl(maxBitrate, activity, entry.getId()));
    		//	intent.putExtra("subtitles", MusicServiceFactory.getMusicService(activity).getSubtitlesUrl(activity, entry.getId()));
    			intent.putExtra("duration", entry.getDuration());
    			activity.startActivity(intent);
            } catch (Exception e) {
    			LOG.info("Unable to play video: " + entry.getTitle());
    		}
        }
    },    

    DEFAULT("default") {
        @Override
        public void playVideo(Activity activity, MusicDirectory.Entry entry) throws Exception {
        	int maxBitrate = Util.getMaxVideoBitrate(activity);
        	Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(MusicServiceFactory.getMusicService(activity).getVideoUrl(maxBitrate, activity, entry.getId(), false)), "video/*");
            activity.startActivity(intent);
        }
    };

    private final String key;

    VideoPlayerType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static VideoPlayerType forKey(String key) {
        for (VideoPlayerType type : VideoPlayerType.values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        return null;
    }

    private static final Logger LOG = new Logger(VideoPlayerType.class);
    
    public abstract void playVideo(Activity activity, MusicDirectory.Entry entry) throws Exception;

    private static final String PACKAGE_NAME_MX_AD = "com.mxtech.videoplayer.ad";
    private static final String PACKAGE_NAME_MX_PRO = "com.mxtech.videoplayer.pro";
    
    private static final String PACKAGE_NAME_BS_FREE = "com.bsplayer.bspandroid.free";
    private static final String PACKAGE_NAME_BS_FULL = "com.bsplayer.bspandroid.full";

    private static final String PACKAGE_NAME_VLC = "org.videolan.vlc.betav7neon";
    
}
