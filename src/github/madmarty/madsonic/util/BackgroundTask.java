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

import java.io.FileNotFoundException;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import github.madmarty.madsonic.R;
import github.madmarty.madsonic.domain.Indexes;
import github.madmarty.madsonic.domain.Version;

/**
 * @author Sindre Mehus
 */
public abstract class BackgroundTask<T> implements ProgressListener {

    private static final Logger LOG = new Logger(BackgroundTask.class);
    private final Activity activity;
    private final Handler handler;

    public BackgroundTask(Activity activity) {
        this.activity = activity;
        handler = new Handler();
    }

    protected Activity getActivity() {
        return activity;
    }

    protected Handler getHandler() {
        return handler;
    }

    public abstract void execute();

    protected abstract T doInBackground() throws Throwable;

    protected abstract void done(T result);

    protected void error(Throwable error) {
        LOG.warn("Got exception: " + error.toString(), error);
      
        if (error.toString().contains("Rescan")) {
            new ErrorDialog(activity, error.toString(), false);
        } else {
            new ErrorDialog(activity, error.toString(), true);
        }
    }

    protected String getErrorMessage(Throwable error) {

        if (error instanceof IOException && !Util.isNetworkConnected(activity)) {
            return activity.getResources().getString(R.string.background_task_no_network);
        }

        if (error instanceof FileNotFoundException) {
            return activity.getResources().getString(R.string.background_task_not_found);
        }

        if (error instanceof IOException) {
            return activity.getResources().getString(R.string.background_task_network_error);
        }

        if (error instanceof XmlPullParserException) {
            return activity.getResources().getString(R.string.background_task_parse_error);
        }

        String message = error.getMessage();
        if (message != null) {
            return message;
        }
        return error.getClass().getSimpleName();
    }

    @Override
    public abstract void updateProgress(final String message);

    @Override
    public void updateProgress(int messageId) {
        updateProgress(activity.getResources().getString(messageId));
    }

	protected void onPostExecute(Version result) {
		// TODO Auto-generated method stub
		
	}

	protected void onPostExecute(Indexes result) {
		// TODO Auto-generated method stub
		
	}
}