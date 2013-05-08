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

import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.miroguide.conn.MiroGuideException;
import org.sixgun.ponyexpress.miroguide.conn.MiroGuideService;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;


public class MiroCategoriesActivity<mPonyExpressApp> extends ListActivity {

	private static final String TAG = "MiroCategoriesActivity";
	private PonyExpressApp mPonyExpressApp;
	private ArrayAdapter<String> listAdapter;
	private static String[] sCategories;
	private ProgressDialog mProgDialog;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.miro_categories);
		
		mPonyExpressApp = (PonyExpressApp) getApplication();
		
		//Set up prog dialog for later
		mProgDialog = new ProgressDialog(this);
		mProgDialog.setMessage(getString(R.string.please_wait_simple));
		
		listCategories();
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



	private void listCategories() {
		if (sCategories == null){
			new LoadCategories().execute();
		} else {
			makeAdapter();
		}
	}
	
	private void makeAdapter(){
		if (sCategories != null) {
			listAdapter = new ArrayAdapter<String>(mPonyExpressApp,
					R.layout.episode_row, sCategories);
			setListAdapter(listAdapter);
		}
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
			makeAdapter();
			mProgDialog.hide();
		}
		
	}
	
	//TODO onListItem selected...
}
