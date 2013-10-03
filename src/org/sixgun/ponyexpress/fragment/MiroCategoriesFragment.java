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

import org.sixgun.ponyexpress.CategoryAdapter;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.miroguide.conn.MiroGuideException;
import org.sixgun.ponyexpress.util.PonyLogger;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class MiroCategoriesFragment extends MiroFragment {

	
	public static final String TAG = "MiroCategoriesFragment";
	private PrivateCategoryAdapter mCategoryAdapter;
	private static String[] sCategories;
	private TextView mSubTitle;

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.fragment.MiroFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		View v = inflater.inflate(R.layout.miro_categories, container, false);
		mSubTitle = (TextView) v.findViewById(R.id.sixgun_subtitle);
		mSubTitle.setText(R.string.categories);
		return v;
	}

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.fragment.MiroFragment#onResume()
	 */
	@Override
	public void onResume() {
		super.onResume();
		listCategories();

	}
	
	private void listCategories() {
		if (sCategories == null) {
			new LoadCategories().execute();
		} else {
			makeCategoryAdapter();
		}
	}

	private void makeCategoryAdapter() {
		if (sCategories != null) {
			mCategoryAdapter = new PrivateCategoryAdapter(mPonyExpressApp,
					R.layout.episode_row, sCategories);
			setListAdapter(mCategoryAdapter);

		}
	}
	
	private class LoadCategories extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			mProgDialog.show(getFragmentManager(), "progress_dialog");

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
			if (mProgDialog.isAdded()){
				mProgDialog.dismiss();
			}
			
		}

		@Override
		protected void onPostExecute(Void result) {
			makeCategoryAdapter();
			if (mProgDialog.isAdded()){
				mProgDialog.dismiss();
			}
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
					//FIXME
					//Needs to get channel fragment
					//listChannels(category);

				}
			});
			return convertView;

		}

	}
}
