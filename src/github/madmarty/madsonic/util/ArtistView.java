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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import github.madmarty.madsonic.R;
import github.madmarty.madsonic.activity.SubsonicTabActivity;
import github.madmarty.madsonic.domain.MusicDirectory;

/**
 * Used to display artits in a {@code ListView}.
 *
 * @author Sindre Mehus
 */
public class ArtistView extends LinearLayout {
	
	private MusicDirectory.Entry artist;

    private TextView artistView;
    private TextView genreView;
    private TextView songCountView;
    private View coverArtView;
    private ImageButton starButton;

    public ArtistView(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.album_list_item, this, true);

        artistView = (TextView) findViewById(R.id.album_title);
        genreView = (TextView) findViewById(R.id.album_artist);
        
        coverArtView = findViewById(R.id.album_coverart);
        songCountView = (TextView) findViewById(R.id.artist_song_count);
        
        starButton = (ImageButton) findViewById(R.id.album_star);
        starButton.setVisibility(Util.isOffline(getContext()) ? View.GONE : View.VISIBLE);
        starButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SubsonicTabActivity activity = (SubsonicTabActivity) getContext();
				activity.toggleStarredInBackground(artist, starButton);
			}
		});
        View moreView = findViewById(R.id.album_more);

        moreView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ArtistView.this.showContextMenu();
            }
        });
    }

    public void setArtist(MusicDirectory.Entry artist, ImageLoader imageLoader) {
    	this.artist = artist;
        
        artistView.setText(artist.getTitle());
        genreView.setText(artist.getArtist());

//FIXME: todo more info        
//        if (artist.isArtist()){
//            songCountView.setVisibility(View.VISIBLE);
//            songCountView.setText("DEBUG: isARTIST");
//        }
        
        genreView.setVisibility(artist.getArtist() == null ? View.GONE : View.VISIBLE);
      
        imageLoader.loadImage(coverArtView, artist, false, true);
        
        starButton.setImageResource(artist.isStarred() ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        starButton.setFocusable(false);
    }
}
