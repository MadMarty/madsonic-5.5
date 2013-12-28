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
package github.madmarty.madsonic.service.parser;

import android.content.Context;

import github.madmarty.madsonic.R;
import github.madmarty.madsonic.domain.PodcastChannel;
import github.madmarty.madsonic.util.ProgressListener;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;

/**
 * 
 * @author Scott
 */
public class PodcastChannelParser extends AbstractParser {
	public PodcastChannelParser(Context context) {
		super(context);
	}

	public List<PodcastChannel> parse(Reader reader, ProgressListener progressListener) throws Exception {
		updateProgress(progressListener, R.string.parser_reading);
		init(reader);

		List<PodcastChannel> channels = new ArrayList<PodcastChannel>();
		int eventType;
		do {
			eventType = nextParseEvent();
			if (eventType == XmlPullParser.START_TAG) {
				String name = getElementName();
				if ("channel".equals(name)) {
					PodcastChannel channel = new PodcastChannel();
					channel.setId(get("id"));
					channel.setUrl(get("url"));
					channel.setName(get("title"));
					channel.setDescription(get("description"));
					channel.setStatus(get("status"));
					channel.setErrorMessage(get("errorMessage"));
					channels.add(channel);
				} else if ("error".equals(name)) {
					handleError();
				}
			}
		} while (eventType != XmlPullParser.END_DOCUMENT);

		validate();
		return channels;
	}
}
