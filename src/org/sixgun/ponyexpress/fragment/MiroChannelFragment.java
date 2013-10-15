/*
 * Copyright 2013 Paul Elms
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

import java.util.ArrayList;
import java.util.List;

import org.sixgun.ponyexpress.ChannelListAdapter;
import org.sixgun.ponyexpress.EndlessChannelListAdapter;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.activity.PonyExpressFragsActivity;
import org.sixgun.ponyexpress.miroguide.conn.MiroGuideException;
import org.sixgun.ponyexpress.miroguide.model.MiroGuideChannel;
import org.sixgun.ponyexpress.util.PonyLogger;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;


public class MiroChannelFragment extends MiroFragment {

	private static final String CATEGORY = "category";

	public static MiroChannelFragment newInstance(String category){
		MiroChannelFragment frag = new MiroChannelFragment();
		Bundle args = new Bundle();
		args.putString(CATEGORY, category);
		frag.setArguments(args);
		return frag;
	}

	private static final String TAG = "MiroChannelFragment";

	private String mCategory;
	private TextView mSubTitle;

	private PrivateChannelListAdapter mChannelAdapter;

	private PrivateEndlessChannelListAdapter mEndlessChannelAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle data;
		if (savedInstanceState != null){
			data = savedInstanceState;
		} else {
			data = getArguments();
		}
		 mCategory = data.getString(CATEGORY);
	}



	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.fragment.MiroFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v  = inflater.inflate(R.layout.miro_categories, container, false);
		
		mSubTitle = (TextView)v.findViewById(R.id.sixgun_subtitle);
		mSubTitle.setText(mCategory);
		
		ImageButton goHome = (ImageButton) v.findViewById(R.id.home_button);
		if (goHome != null){
			//only present on android <11
			goHome.setOnClickListener(this);
		}
		
		return v;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		listChannels();
	}

	private void listChannels() {
		new LoadChannels().execute(mCategory);
	}
	
	private class LoadChannels extends
	AsyncTask<String, Void, List<MiroGuideChannel>> {

		@Override
		protected void onPreExecute() {
			mProgDialog.show(getFragmentManager(), "progress_dialog");
		}

		@Override
		protected List<MiroGuideChannel> doInBackground(String... params) {
			try {
				return mMiroService.getChannelList("category", params[0],
						"name", MiroGuideChannel.DEFAULT_LIMIT, 0);
			} catch (MiroGuideException e) {
				PonyLogger.e(TAG, "Could not get Miro channels", e);
			}
			return null;
		}

		@Override
		protected void onCancelled(List<MiroGuideChannel> result) {
			mProgDialog.dismiss();
		}

		@Override
		protected void onPostExecute(List<MiroGuideChannel> result) {
			ArrayList<MiroGuideChannel> channels = new ArrayList<MiroGuideChannel>();
			if (result != null) {
				for (MiroGuideChannel channel : result) {
					channels.add(channel);
				}
			}
			if (channels.size() < MiroGuideChannel.DEFAULT_LIMIT) {
				makeChannelAdapter(channels);
			} else {
				makeEndlessChannelAdapter(channels);
			}

			mProgDialog.dismiss();
		}

	}

	private void makeChannelAdapter(ArrayList<MiroGuideChannel> channels) {
		mChannelAdapter = new PrivateChannelListAdapter(mPonyExpressApp,
				R.layout.episode_row, channels);
		setListAdapter(mChannelAdapter);
	}

	private void makeEndlessChannelAdapter(ArrayList<MiroGuideChannel> channels) {
		mEndlessChannelAdapter = new PrivateEndlessChannelListAdapter(
				mPonyExpressApp, mCategory, channels);
		setListAdapter(mEndlessChannelAdapter);
	}
	/**
	 * Enclosed sub-class of ChannelListAdapter which allows the calling of
	 * methods of this activity by OnClickListeners.
	 * 
	 */
	private class PrivateChannelListAdapter extends ChannelListAdapter {

		public PrivateChannelListAdapter(Context context,
				int textViewResourceId, List<MiroGuideChannel> channels) {
			super(context, textViewResourceId, channels);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			convertView = super.getView(position, convertView, parent);
			final MiroGuideChannel channel = getItem(position);

			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					//FIXME to use frags
					//listChannelItems(channel);

				}
			});
			return convertView;

		}

	}

	private class PrivateEndlessChannelListAdapter extends
			EndlessChannelListAdapter {

		public PrivateEndlessChannelListAdapter(Context context,
				String category_name, ArrayList<MiroGuideChannel> channels) {
			super(context, category_name, channels);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			convertView = super.getView(position, convertView, parent);
			final MiroGuideChannel channel = (MiroGuideChannel) getItem(position);

			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					//FIXME to use frags
					//listChannelItems(channel);

				}
			});
			return convertView;

		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.home_button:
			Intent intent = new Intent(mPonyExpressApp, PonyExpressFragsActivity.class);
			startActivity(intent);
			break;
		default:
			break;
		}
	}

}
