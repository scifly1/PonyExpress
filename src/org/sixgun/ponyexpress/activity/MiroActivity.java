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
package org.sixgun.ponyexpress.activity;

import java.util.ArrayList;
import java.util.List;

import org.sixgun.ponyexpress.CategoryAdapter;
import org.sixgun.ponyexpress.ChannelListAdapter;
import org.sixgun.ponyexpress.EndlessChannelListAdapter;
import org.sixgun.ponyexpress.ItemListAdapter;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.fragment.AddNewPodcastsFragment;
import org.sixgun.ponyexpress.miroguide.conn.MiroGuideException;
import org.sixgun.ponyexpress.miroguide.conn.MiroGuideService;
import org.sixgun.ponyexpress.miroguide.model.MiroGuideChannel;
import org.sixgun.ponyexpress.miroguide.model.MiroGuideItem;
import org.sixgun.ponyexpress.util.PonyLogger;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

public class MiroActivity<mPonyExpressApp> extends ListActivity {

	private static final String TAG = "MiroActivity";
	private static final String LISTING_ITEMS = "items";
	private static final String LISTING_CHANNELS = "channels";
	private static final String LISTING_SEARCH = "search";
	private static final String CURRENT_CATEGORY = "category_name";
	private static final String CURRENT_PODCAST = "current_podcast";
	private PonyExpressApp mPonyExpressApp;
	private MiroGuideService mMiroService;
	private ArrayAdapter<String> mCategoryAdapter;
	private static String[] sCategories;
	private ProgressDialog mProgDialog;
	private ChannelListAdapter mChannelAdapter;
	private EndlessChannelListAdapter mEndlessChannelAdapter;
	private TextView mSubTitle;
	private boolean mListingChannels;
	private boolean mListingItems;
	private String mCurrentCategory;
	private boolean mListingSearch;
	private ImageButton mAddButton;
	private MiroGuideChannel mCurrentPodcast;
	private PrivateItemListAdapter mItemListAdapter;
	private TextView mDescriptionView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.miro_categories);

		mPonyExpressApp = (PonyExpressApp) getApplication();
		mMiroService = new MiroGuideService();

		if (savedInstanceState != null) {
			// Restore value of members from saved state
			PonyLogger.d(TAG, "Restoring state of members");
			mListingChannels = savedInstanceState.getBoolean(LISTING_CHANNELS);
			mListingItems = savedInstanceState.getBoolean(LISTING_ITEMS);
			mListingSearch = savedInstanceState.getBoolean(LISTING_SEARCH);
			mCurrentCategory = savedInstanceState.getString(CURRENT_CATEGORY);
			mCurrentPodcast = savedInstanceState.getParcelable(CURRENT_PODCAST);
		}

		// Set up prog dialog for later
		mProgDialog = new ProgressDialog(this);
		mProgDialog.setMessage(getString(R.string.please_wait_simple));

		mSubTitle = (TextView) findViewById(R.id.sixgun_subtitle);
		mAddButton = (ImageButton) findViewById(R.id.add_feeds_button);
		mDescriptionView = (TextView) getLayoutInflater().inflate(
				R.layout.podcast_description_header, null);

		// TODO Add album art to the Channel(podcast) lists.

		// Get the intent, if a search action get the query
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction()) && !mListingSearch) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			searchForPodcast(query);
		} else if (mListingItems) { // The order is important here, if items is
									// true so is channels
			listChannelItems(mCurrentPodcast);
		} else if (mListingChannels) {
			listChannels(mCurrentCategory);
		} else if (mListingSearch) {
			searchForPodcast(mCurrentCategory);
		} else
			listCategories();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(LISTING_ITEMS, mListingItems);
		outState.putBoolean(LISTING_CHANNELS, mListingChannels);
		outState.putBoolean(LISTING_SEARCH, mListingSearch);
		outState.putString(CURRENT_CATEGORY, mCurrentCategory);
		outState.putParcelable(CURRENT_PODCAST, mCurrentPodcast);
		super.onSaveInstanceState(outState);
	}

	/**
	 * Cancel progress dialog and close any running updates when activity
	 * destroyed.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Dismiss dialog now or it will leak.
		if (mProgDialog.isShowing()) {
			mProgDialog.dismiss();
		}
		mMiroService.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.miro_options_menu, menu);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings_menu:
			startActivity(new Intent(mPonyExpressApp, PreferencesActivity.class));
			return true;
		case R.id.search:
			onSearchRequested();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		// Handle pressing back when showing the episodes list.
		if (mListingItems) {
			mListingItems = false;
			if (mListingSearch) {
				searchForPodcast(mCurrentCategory);
			} else
				listChannels(mCurrentCategory);
		} else if (mListingChannels) {
			mListingChannels = false;
			listCategories();
		} else {
			finish();
		}
	}

	public void goHome(View v) {
		Intent intent = new Intent(mPonyExpressApp, PonyExpressActivity.class);
		startActivity(intent);
	}

	public void addPodcast(View v) {
		Intent intent = new Intent();
		intent.putExtra(PodcastKeys.FEED_URL, mCurrentPodcast.getDownloadUrl());
		setResult(RESULT_OK ,intent);
		finish();
	}

	private void searchForPodcast(String query) {
		mCurrentCategory = query;
		new SearchChannels().execute(mCurrentCategory);
		mSubTitle.setText(mCurrentCategory);
		mAddButton.setVisibility(View.GONE);
		mListingSearch = true;

	}

	private void listCategories() {
		if (sCategories == null) {
			new LoadCategories().execute();
		} else {
			makeCategoryAdapter();
		}
		mSubTitle.setText(R.string.categories);
		mAddButton.setVisibility(View.GONE);
	}

	public void listChannels(String category) {
		mCurrentCategory = category;
		new LoadChannels().execute(mCurrentCategory);
		mSubTitle.setText(mCurrentCategory);
		mListingChannels = true;

	}

	private void listChannelItems(MiroGuideChannel podcast) {
		mListingItems = true;
		mCurrentPodcast = podcast;
		mSubTitle.setText(mCurrentPodcast.getName());
		mAddButton.setVisibility(View.VISIBLE);
		new LoadItems().execute(mCurrentPodcast);
	}

	private void makeCategoryAdapter() {
		if (sCategories != null) {
			mCategoryAdapter = new PrivateCategoryAdapter(mPonyExpressApp,
					R.layout.episode_row, sCategories);
			setListAdapter(mCategoryAdapter);

		}
	}

	private void makeChannelAdapter(ArrayList<MiroGuideChannel> channels) {
		mChannelAdapter = new PrivateChannelListAdapter(mPonyExpressApp,
				R.layout.episode_row, channels);
		getListView().removeHeaderView(mDescriptionView);
		setListAdapter(mChannelAdapter);
	}

	private void makeEndlessChannelAdapter(ArrayList<MiroGuideChannel> channels) {
		mEndlessChannelAdapter = new PrivateEndlessChannelListAdapter(
				mPonyExpressApp, mCurrentCategory, channels);
		getListView().removeHeaderView(mDescriptionView);
		setListAdapter(mEndlessChannelAdapter);
	}

	private void makePodcastAdapter(MiroGuideChannel podcast) {
		mItemListAdapter = new PrivateItemListAdapter(mPonyExpressApp,
				R.layout.episode_row, podcast.getItems());
		mDescriptionView.setText(podcast.getDescription());
		setListAdapter(null);
		getListView().addHeaderView(mDescriptionView);
		setListAdapter(mItemListAdapter);

	}

	private class LoadCategories extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			mProgDialog.show();

		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				sCategories = mMiroService.getCategories();
			} catch (MiroGuideException e) {
				PonyLogger.e(TAG, "Could not get Miro categories", e);
			}
			return null;
		}

		@Override
		protected void onCancelled(Void result) {
			mProgDialog.hide();
		}

		@Override
		protected void onPostExecute(Void result) {
			makeCategoryAdapter();
			mProgDialog.hide();
		}

	}

	private class LoadChannels extends
			AsyncTask<String, Void, List<MiroGuideChannel>> {

		@Override
		protected void onPreExecute() {
			mProgDialog.show();

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
			mProgDialog.hide();
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

			mProgDialog.hide();
		}

	}

	/**
	 * A sub-class of LoadChannels as it does the same search but using a name
	 * as a search query rather than a category.
	 * 
	 */
	private class SearchChannels extends LoadChannels {

		@Override
		protected List<MiroGuideChannel> doInBackground(String... params) {
			try {
				return mMiroService.getChannelList("name", params[0], "name",
						MiroGuideChannel.DEFAULT_LIMIT, 0);
			} catch (MiroGuideException e) {
				PonyLogger.e(TAG, "Could not get Miro channels", e);
			}
			return null;
		}
	}

	private class LoadItems extends
			AsyncTask<MiroGuideChannel, Void, MiroGuideChannel> {

		@Override
		protected void onPreExecute() {
			mProgDialog.show();

		}

		@Override
		protected MiroGuideChannel doInBackground(MiroGuideChannel... params) {
			try {
				return mMiroService.getChannel(params[0].getId());
			} catch (MiroGuideException e) {
				PonyLogger.e(TAG, "Could not get Miro channels", e);
			}
			return null;
		}

		@Override
		protected void onCancelled(MiroGuideChannel result) {
			mProgDialog.hide();
		}

		@Override
		protected void onPostExecute(MiroGuideChannel result) {

			makePodcastAdapter(result);

			mProgDialog.hide();
		}

	}

	/**
	 * Enclosed sub-class of CategoryAdapter which allows the calling of methods
	 * of this activity by OnClickListeners.
	 * 
	 */
	private class PrivateCategoryAdapter extends CategoryAdapter {

		public PrivateCategoryAdapter(Context context, int textViewResourceId,
				String[] categories) {
			super(context, textViewResourceId, categories);

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			convertView = super.getView(position, convertView, parent);
			final String category = getItem(position);

			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					listChannels(category);

				}
			});
			return convertView;

		}

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
					listChannelItems(channel);

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
					listChannelItems(channel);

				}
			});
			return convertView;

		}
	}

	private class PrivateItemListAdapter extends ItemListAdapter {

		public PrivateItemListAdapter(Context context, int textViewResourceId,
				List<MiroGuideItem> items) {
			super(context, textViewResourceId, items);

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.sixgun.ponyexpress.ItemListAdapter#getView(int,
		 * android.view.View, android.view.ViewGroup)
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			convertView = super.getView(position, convertView, parent);
			// Make the episodes un-selectable
			convertView.setEnabled(false);

			return convertView;
		}

	}

}
