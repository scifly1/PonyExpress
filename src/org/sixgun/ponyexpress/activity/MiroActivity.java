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
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.SearchSuggestionsProvider;
import org.sixgun.ponyexpress.miroguide.conn.MiroGuideException;
import org.sixgun.ponyexpress.miroguide.conn.MiroGuideService;
import org.sixgun.ponyexpress.miroguide.model.MiroGuideChannel;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


public class MiroActivity<mPonyExpressApp> extends ListActivity {

	private static final String TAG = "MiroActivity";
	private PonyExpressApp mPonyExpressApp;
	private ArrayAdapter<String> categoryAdapter;
	private static String[] sCategories;
	private ProgressDialog mProgDialog;
	private ChannelListAdapter mChannelAdapter;
	private EndlessChannelListAdapter mEndlessChannelAdapter;
	private TextView mSubTitle;
	private boolean mListingChannels;
	private boolean mListingItems;
	private String mCurrentCategory;
	private boolean mListingSearch;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.miro_categories);
		
		mPonyExpressApp = (PonyExpressApp) getApplication();
		
		//Set up prog dialog for later
		mProgDialog = new ProgressDialog(this);
		mProgDialog.setMessage(getString(R.string.please_wait_simple));
		
		mSubTitle = (TextView) findViewById(R.id.sixgun_subtitle);
		
		// Get the intent, if a search action get the query
	    Intent intent = getIntent();
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	      String query = intent.getStringExtra(SearchManager.QUERY);
	      SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
	                SearchSuggestionsProvider.AUTHORITY, SearchSuggestionsProvider.MODE);
	        suggestions.saveRecentQuery(query, null);
	      searchForPodcast(query);
	    } else {
	    	listCategories();
	    }
	}
	
	/**
	 *  Cancel progress dialog and close any running updates when activity destroyed.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Dismiss dialog now or it will leak.
		if (mProgDialog.isShowing()){
			mProgDialog.dismiss();
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		//Handle pressing back when showing the episodes list. 
		if (mListingChannels){
			mListingChannels = false;
			listCategories();
		} else if (mListingItems){
			mListingItems = false;
			if (mListingSearch){
				searchForPodcast(mCurrentCategory);
			} else
				listChannels(mCurrentCategory);
		} else {
			finish();
		}
	}
	
	private void searchForPodcast(String query){
		mCurrentCategory = query;
		new SearchChannels().execute(mCurrentCategory);
		mSubTitle.setText(mCurrentCategory);
		mListingSearch = true;
		
	}

	private void listCategories() {
		if (sCategories == null){
			new LoadCategories().execute();
		} else {
			makeCategoryAdapter();
		}
		mSubTitle.setText(R.string.categories);
	}
	
	public void listChannels(String category){
		mCurrentCategory = category;
		new LoadChannels().execute(mCurrentCategory);
		mSubTitle.setText(mCurrentCategory);
		mListingChannels = true;
		
	}
	
	private void listChannelItems(MiroGuideChannel podcast){
		mListingItems = true;
		
		//TODO
	}
	
	private void makeCategoryAdapter(){
		if (sCategories != null) {
			categoryAdapter = new PrivateCategoryAdapter(mPonyExpressApp,
					R.layout.episode_row, sCategories);
			setListAdapter(categoryAdapter);

		}
	}
	private void makeChannelAdapter(ArrayList<MiroGuideChannel> channels){
		mChannelAdapter = new PrivateChannelListAdapter(mPonyExpressApp, 
				R.layout.episode_row, channels);
		setListAdapter(mChannelAdapter);
	}
	private void makeEndlessChannelAdapter(ArrayList<MiroGuideChannel> channels){
		mEndlessChannelAdapter = new PrivateEndlessChannelListAdapter(mPonyExpressApp, 
				mCurrentCategory, channels);
		setListAdapter(mEndlessChannelAdapter);
	}
	
	/**
	 * Bring up the Settings (preferences) menu via a button click.
	 * @param v, a reference to the button that was clicked to call this.
	 */
	public void showSettings(View v){
		startActivity(new Intent(
				mPonyExpressApp,PreferencesActivity.class));
	}
	
	private class LoadCategories extends AsyncTask<Void,Void,Void>{

		@Override
		protected void onPreExecute() {
			mProgDialog.show();
			
		}

		@Override
		protected Void doInBackground(Void... params) {
			MiroGuideService miro = new MiroGuideService();
			try {
				sCategories = miro.getCategories();
			} catch (MiroGuideException e) {
				Log.e(TAG, "Could not get Miro categories", e);
			} finally {
				miro.close();
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
	
	private class LoadChannels extends AsyncTask<String,Void,List<MiroGuideChannel>>{

		@Override
		protected void onPreExecute() {
			mProgDialog.show();
			
		}

		@Override
		protected List<MiroGuideChannel> doInBackground(String... params) {
			MiroGuideService miro = new MiroGuideService();
			
			try {
				return miro.getChannelList("category", params[0], "name", MiroGuideChannel.DEFAULT_LIMIT, 0);
			} catch (MiroGuideException e) {
				Log.e(TAG, "Could not get Miro channels", e);
			} finally {
				miro.close();
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
			for (MiroGuideChannel channel : result){
				channels.add(channel);
			}
			if (channels.size() < MiroGuideChannel.DEFAULT_LIMIT){
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
	private class SearchChannels extends LoadChannels{

		@Override
		protected List<MiroGuideChannel> doInBackground(String... params) {
			MiroGuideService miro = new MiroGuideService();
			
			try {
				return miro.getChannelList("name", params[0], "name", MiroGuideChannel.DEFAULT_LIMIT, 0);
			} catch (MiroGuideException e) {
				Log.e(TAG, "Could not get Miro channels", e);
			} finally {
				miro.close();
			}
			return null;
		}
		
	}
	
	/**
	 * Enclosed sub-class of CategoryAdapter which allows the calling
	 * of methods of this activity by OnClickListeners. 
	 *
	 */
	private class PrivateCategoryAdapter extends CategoryAdapter{

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
	 * Enclosed sub-class of ChannelListAdapter which allows the calling
	 * of methods of this activity by OnClickListeners. 
	 *
	 */
	private class PrivateChannelListAdapter extends ChannelListAdapter{

		
		
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
	
	private class PrivateEndlessChannelListAdapter extends EndlessChannelListAdapter{

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
	
}
