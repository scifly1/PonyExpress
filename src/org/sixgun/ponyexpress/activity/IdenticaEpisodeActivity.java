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
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.Dent.DentKeys;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity that extends IdenticaActivity with Episode specific view.
 *
 */
public class IdenticaEpisodeActivity extends IdenticaActivity {

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.IdenticaActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.identica_episodes);
		mIdenticaTag = mData.getString(PodcastKeys.TAG);
		
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
							Toast.makeText(IdenticaEpisodeActivity.this, R.string.sending_dent, 
									 Toast.LENGTH_SHORT).show();
							mIdenticaHandler.new PostDent().execute(text);
						}
						mDentText.setText(mTagText);
						mDentText.setSelection(mDentText.length()); //Moves cursor to the end
						new GetLatestDents().execute();
					}
				} else {
					Toast.makeText(IdenticaEpisodeActivity.this, R.string.login_failed, 
							Toast.LENGTH_LONG).show();
					//Fire off AccountSetup screen
					startActivityForResult(new Intent(
							IdenticaEpisodeActivity.this,IdenticaAccountSetupActivity.class),
							SETUP_ACCOUNT);
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
	}

	protected void updateCounter() {
		final int chars = 140;
		int charsRemaining = chars-mDentText.length();
		mCharCounter.setText("" + charsRemaining);
		if (charsRemaining < 0) {
			mDentButton.setEnabled(false);
		} else {
			mDentButton.setEnabled(true);
		}
	}
	
	
}
