package github.madmarty.madsonic.service;

import android.content.Context;
import github.madmarty.madsonic.util.Logger;
import github.madmarty.madsonic.util.Util;

/**
 * Scrobbles played songs to Last.fm.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class Scrobbler {

    private static final Logger LOG = new Logger(Scrobbler.class);

    private String lastSubmission;
    private String lastNowPlaying;

    public void scrobble(final Context context, final DownloadFile song, final boolean submission) {
        if (song == null || !Util.isScrobblingEnabled(context)) {
            return;
        }
        final String id = song.getSong().getId();

        // Avoid duplicate registrations.
        if (submission && id.equals(lastSubmission)) {
            return;
        }
        if (!submission && id.equals(lastNowPlaying)) {
            return;
        }
        if (submission) {
            lastSubmission = id;
        } else {
            lastNowPlaying = id;
        }

        new Thread("Scrobble " + song) {
            @Override
            public void run() {
                MusicService service = MusicServiceFactory.getMusicService(context);
                try {
                    service.scrobble(id, submission, context, null);
                    LOG.info("Scrobbled '" + (submission ? "submission" : "now playing") + "' for " + song);
                } catch (Exception x) {
                    LOG.info("Failed to scrobble'" + (submission ? "submission" : "now playing") + "' for " + song, x);
                }
            }
        }.start();
    }
}
