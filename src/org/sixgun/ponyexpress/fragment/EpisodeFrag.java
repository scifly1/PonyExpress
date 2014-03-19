/*
 * Copyright 2014 Paul Elms
 *
 *  This file is part of PonyExpress.
 *
 *  PonyExpress is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  PonyExpress is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with PonyExpress.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sixgun.ponyexpress.fragment;

import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.util.Utils;
import org.sixgun.ponyexpress.util.Bitmap.RecyclingImageView;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;


public class EpisodeFrag extends Fragment implements OnClickListener, OnLongClickListener{

	private Bundle mData;
	private int mCurrentPosition;
	private String mAlbumArtUrl;
	private long mRow_ID;
	private String mEpisodeTitle;
	private RelativeLayout mPlayerControls;
	private ImageButton mPlayPauseButton;
	private ImageButton mRewindButton;
	private ImageButton mFastForwardButton;
	private SeekBar mSeekBar;
	private TextView mElapsed;
	protected boolean mUserSeeking;
	private TextView mEpisodeLength;
	private Button mDownloadButton;
	private Button mCancelButton;
	private ProgressBar mDownloadProgress;
	private PonyExpressApp mPonyExpressApp;
	private boolean mEpisodeDownloaded;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mData = getActivity().getIntent().getExtras();
		mCurrentPosition = mData.getInt(EpisodeKeys.LISTENED);
		mAlbumArtUrl = mData.getString(PodcastKeys.ALBUM_ART_URL);
		mRow_ID = mData.getLong(EpisodeKeys._ID);
		mEpisodeTitle = mData.getString(EpisodeKeys.TITLE);
		
		mPonyExpressApp = (PonyExpressApp)getActivity().getApplication();
		
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.player, null);
		
		//Set up some listeners
		OnSeekBarChangeListener seekBarListener = new OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser){
					mCurrentPosition = progress;
					mElapsed.setText(Utils.milliToTime(progress,false));
				}
				
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				mUserSeeking = true;
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				//TODO
			}
//				mPodcastPlayer.SeekTo(mCurrentPosition);
//				mPodcastPlayer.savePlaybackPosition(mCurrentPosition);
//				mUserSeeking = false;
				
			};
		
		//Get all controls
		mPlayerControls = (RelativeLayout)v.findViewById(R.id.player_controls);
		mPlayPauseButton = (ImageButton)v.findViewById(R.id.PlayButton);
		mPlayPauseButton.setOnClickListener(this);
		mRewindButton = (ImageButton)v.findViewById(R.id.rewind);
		mRewindButton.setOnClickListener(this);
		mFastForwardButton = (ImageButton)v.findViewById(R.id.fastforward);
		mFastForwardButton.setOnClickListener(this);
		mFastForwardButton.setOnLongClickListener(this);
		mSeekBar = (SeekBar)v.findViewById(R.id.PlayerSeekBar);	
		mSeekBar.setOnSeekBarChangeListener(seekBarListener);
		mElapsed = (TextView)v.findViewById(R.id.elapsed_time);
		mEpisodeLength = (TextView)v.findViewById(R.id.length);
		mDownloadButton = (Button)v.findViewById(R.id.DownloadButton);
		mCancelButton = (Button)v.findViewById(R.id.CancelButton);
		mDownloadProgress = (ProgressBar)v.findViewById(R.id.DownloadProgressBar);
		
		
		//Only make Download button available if we have internet connectivity.
		if (mPonyExpressApp.getInternetHelper().checkConnectivity()){
			mDownloadButton.setOnClickListener(this);
			mCancelButton.setOnClickListener(this);
		}else{
			mDownloadButton.setEnabled(false);
		}
		
		/**Check if episode is downloaded, show player buttons
		*if it is or download button if not.
		*/
		mEpisodeDownloaded = mPonyExpressApp.getDbHelper().isEpisodeDownloaded(mRow_ID);
		if (!mEpisodeDownloaded){
			mPlayerControls.setVisibility(View.GONE);
			mSeekBar.setVisibility(View.GONE);
			mDownloadProgress.setVisibility(View.VISIBLE);
			mDownloadButton.setVisibility(View.VISIBLE);
		} else {
			//TODO
//			initPlayer();
		}
		
		//Get Album art url and set image.
		//Get the orientation
		final int orientation = getResources().getConfiguration().orientation;
		//Skip if orientation is landscape
		//FIXME If landscape and large screen don't skip
		if (orientation != 2) {
			//Set the image
			RecyclingImageView album_art = (RecyclingImageView)v.findViewById(R.id.album_art);
			mAlbumArtUrl = getActivity().getIntent().getExtras().getString(PodcastKeys.ALBUM_ART_URL);
			if (mAlbumArtUrl != null && !"".equals(mAlbumArtUrl) 
					&& !"null".equalsIgnoreCase(mAlbumArtUrl) && album_art !=null){
				PonyExpressApp.sBitmapManager.loadImage(mAlbumArtUrl, album_art);
			}
		}
		return v;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onLongClick(View v) {
		// TODO Auto-generated method stub
		return false;
	}

	
}
