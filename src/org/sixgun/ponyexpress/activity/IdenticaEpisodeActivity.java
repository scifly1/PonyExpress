/*
 * Copyright 2011 Paul Elms
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

import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.Dent.DentKeys;
import org.sixgun.ponyexpress.service.IdenticaHandler;
import org.sixgun.ponyexpress.util.Utils;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity that extends IdenticaActivity with Episode specific view.
 *
 */
public class IdenticaEpisodeActivity extends IdenticaActivity {
	
	protected PostDent mTask;
	
	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.IdenticaActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.identica_episodes);
		mIdenticaTag = mData.getString(PodcastKeys.TAG);
		mAlbumArtUrl = mData.getString(PodcastKeys.ALBUM_ART_URL);
		
		//Create Progress Dialogs for later use.
		mProgDialog = new ProgressDialog(this);
		mProgDialog.setMessage(getString(R.string.sending_dent));
		
		OnClickListener DentButtonListener = new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mIdenticaHandler.credentialsSet()){
					if (mDentText.getText().length() != 0) {
						//Easter egg for the observant :)
						String text = mDentText.getText().toString();
						if (text.equals("alloneword")){
							SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mPonyExpressApp);
							SharedPreferences.Editor editor = prefs.edit();
							editor.putBoolean(getString(R.string.add_podcasts), true);
							editor.commit();
							Log.d(TAG,"Easter egg activated");
						} else {
							mTask = new PostDent();
							mTask.execute(text);
							
						}
						mDentText.setText(mTagText);
						mDentText.setSelection(mDentText.length()); //Moves cursor to the end
						new GetLatestDents().execute();
					}
				} else {
					Toast.makeText(IdenticaEpisodeActivity.this, R.string.login_failed, 
							Toast.LENGTH_LONG).show();
					//Fire off AccountSetup screen
					startActivity(new Intent(
							IdenticaEpisodeActivity.this,IdenticaAccountSetupActivity.class));
				}				
			}
		};
		//Check connectivity first and inactivate button if no connection
		mDentButton = (Button) findViewById(R.id.dent_ok);
		if (mPonyExpressApp.getInternetHelper().checkConnectivity()){
			mDentButton.setOnClickListener(DentButtonListener);
			mDentButton.setEnabled(true);
		}else{
			mDentButton.setEnabled(false);
		}
		
		mCharCounter = (TextView) findViewById(R.id.char_count);
		mCharCounter.setText("140");
		
		mDentText = (EditText) findViewById(R.id.dent_entry);
		String text = "";
		if (savedInstanceState != null){
			text = savedInstanceState.getString(DentKeys.PARTIALDENT);
		} else if (!mGroupDents){
			text = mData.getString(EpisodeKeys.EP_NUMBER);
		}
		if (mGroupDents){
			mTagText = "!" + mIdenticaTag + text + " ";
		} else {
			mTagText = "#" + mIdenticaTag + text + " ";
		}
		mDentText.setText(mTagText);
		
		mDentText.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				updateCounter();
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				
			}
		});
		
		//Set the background
		mBackground = (ViewGroup) findViewById(R.id.IdenticaLayout);
		mBackground.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			
			@Override
			public void onGlobalLayout() {
				Resources res = getResources();
				Bitmap image = PonyExpressApp.sImageManager.get(mAlbumArtUrl);
				if (image != null){
					int new_height = mBackground.getHeight();
					int new_width = mBackground.getWidth();
					BitmapDrawable new_background = Utils.createBackgroundFromAlbumArt
					(res, image, new_height, new_width);
					mBackground.setBackgroundDrawable(new_background);
				}
				
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Dismiss dialog now or it will leak.
		if (mProgDialog.isShowing()){
			mProgDialog.dismiss();
		}
	}
	
	private class PostDent extends AsyncTask<String, Void, Integer> {
		
		@Override
		protected void onPreExecute() {
			mProgDialog.show();
		}

		@Override
		protected Integer doInBackground(String... dent) {
			Integer status = mIdenticaHandler.postDent(dent);
			//mIdenticaHandler.new PostDent().execute(text);
			return status;
		}
		
		@Override
		protected void onPostExecute(Integer status) {
			mProgDialog.hide();
			switch (status) {
			case IdenticaHandler.NO_CONNECTIVITY:
				Log.d(TAG,"No internet connection");
				Toast.makeText(IdenticaEpisodeActivity.this, R.string.no_internet_connection,Toast.LENGTH_LONG).show();
				break;
			case IdenticaHandler.CLIENTPROTOCOLEXCEPTION:
				Log.d(TAG,"ClientProtocolException thrown");
				Toast.makeText(IdenticaEpisodeActivity.this, "ClientProtocolException",Toast.LENGTH_LONG).show();
				break;
			case IdenticaHandler.IO_EXCEPTION:
				Log.d(TAG,"Identi.ca is offline, or internet connectivity has been lost");
				Toast.makeText(IdenticaEpisodeActivity.this, R.string.identica_offline,Toast.LENGTH_LONG).show();
				break;
			case IdenticaHandler.CANNOT_ENCODE_DENT:
				Log.d(TAG,"Can not encode the dent!");
				Toast.makeText(IdenticaEpisodeActivity.this, R.string.can_not_encode_dent,Toast.LENGTH_LONG).show();
				break;
			case IdenticaHandler.SUCCESSFUL_DENT:
				Log.d(TAG,"Dent sent");
				Toast.makeText(IdenticaEpisodeActivity.this, R.string.successful_dent,Toast.LENGTH_LONG).show();
				break;
			
			}
		}
	}
}
