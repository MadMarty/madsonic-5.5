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

import android.app.Activity;
import android.content.Intent;
import github.madmarty.madsonic.R;
import github.madmarty.madsonic.domain.MusicDirectory;
import github.madmarty.madsonic.service.MusicService;
import github.madmarty.madsonic.service.MusicServiceFactory;

import java.net.URL;

/**
 * @author Sindre Mehus
 * @version $Id: ShareUtil.java 3622 2013-11-12 20:35:39Z sindre_mehus $
 */
public final class ShareUtil {

    private ShareUtil() {
    }

    public static void shareInBackground(Activity activity, MusicDirectory directory) {
        shareInBackground(false, activity, directory.getId());
    }

    public static void shareInBackground(Activity activity, MusicDirectory.Entry entry) {
        shareInBackground(false, activity, entry.getId());
    }

    public static void shareWithMadsonicInBackground(Activity activity, MusicDirectory directory) {
    	boolean external = true;
        shareInBackground(external, activity, directory.getId());
    }
    
    public static void shareInBackground(final Activity activity, final String id) {
    	shareInBackground(false, activity, id);
    }
    
    public static void shareInBackground(boolean external, final Activity activity, final String id) {
    	
    	if (external) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "madsonic://search/id/" + id);
            activity.startActivity(Intent.createChooser(intent, activity.getResources().getString(R.string.share_via)));
    	}
    	else {

    		new SilentBackgroundTask<URL>(activity) {
            @Override
            protected URL doInBackground() throws Throwable {
                MusicService musicService = MusicServiceFactory.getMusicService(activity);
                return musicService.createShare(id, activity, null);
            }

            @Override
            protected void done(URL result) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, result.toExternalForm());
                activity.startActivity(Intent.createChooser(intent, activity.getResources().getString(R.string.share_via)));
            }

            @Override
            protected void error(Throwable error) {
                Util.toast(activity, R.string.share_failed, false, getErrorMessage(error));
            }
        }.execute();
    	}
    }
}
