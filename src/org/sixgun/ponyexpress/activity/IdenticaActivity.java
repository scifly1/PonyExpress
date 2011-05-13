/*
 * Copyright 2010-2011 Paul Elms
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

import org.sixgun.ponyexpress.Dent;
import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.Dent.DentKeys;
import org.sixgun.ponyexpress.service.IdenticaHandler;
import org.sixgun.ponyexpress.util.Utils;
import org.sixgun.ponyexpress.view.RemoteImageView;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Handles starting IdenticaHandler.
 *
 */
public class IdenticaActivity extends ListActivity {
	
	protected static final String TAG = "PonyExpress IdenticaActivity";
	protected static final int SETUP_ACCOUNT = 0;
	protected static final int ADD_FEED = 1;
	protected PonyExpressApp mPonyExpressApp; 
	protected IdenticaHandler mIdenticaHandler;
	protected boolean mIdenticaHandlerBound;
	protected Bundle mData;
	
	protected EditText mDentText;
	protected TextView mCharCounter;
	protected Button mDentButton;
	protected boolean mGroupDents = false;
	protected String mIdenticaTag;
	private String mIdenticaGroup;
	protected String mTagText;
	protected ViewGroup mBackground;
	protected String mAlbumArtUrl;
	
	//This is all responsible for connecting/disconnecting to the IdenticaHandler service.
	private ServiceConnection mConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        // Because it is running in our same process, we should never
	        // see this happen.
	        mIdenticaHandler = null;
			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  Because we have bound to an explicit
	        // service that we know is running in our own process, we can
	        // cast its IBinder to a concrete class and directly access it.
			mIdenticaHandler = ((IdenticaHandler.IdenticaHandlerBinder)service).getService();
			new GetLatestDents().execute();
		}
	};
	
	protected void doBindIdenticaHandler() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because we want a specific service implementation that
	    // we know will be running in our own process (and thus won't be
	    // supporting component replacement by other applications).
		
		//getApplicationContext().bindService() called instead of bindService(), as
		//bindService() does not work when called from the child Activity of an ActivityGroup
		//ie:TabActivity
	    getApplicationContext().bindService(new Intent(this, 
	            IdenticaHandler.class), mConnection, Context.BIND_AUTO_CREATE);
	    mIdenticaHandlerBound = true;
	}


	protected void doUnbindIdenticaHandler() {
	    if (mIdenticaHandlerBound) {
	        // Detach our existing connection.
	    	//Must use getApplicationContext.unbindService() as 
	    	//getApplicationContext().bindService was used to bind initially.
	        getApplicationContext().unbindService(mConnection);
	        mIdenticaHandlerBound = false;
	    }
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPonyExpressApp = (PonyExpressApp)getApplication();
		doBindIdenticaHandler();
		Log.d(TAG, "IdenticaActivity Started.");
		mData = getIntent().getExtras();
		if (mData.containsKey(PodcastKeys.GROUP)){
			mGroupDents = true;
			mIdenticaGroup = mData.getString(PodcastKeys.GROUP);
		}
		mIdenticaTag = mData.getString(PodcastKeys.TAG);
		mAlbumArtUrl = mData.getString(PodcastKeys.ALBUM_ART_URL);
		setContentView(R.layout.identica);
		
		//Set title
		TextView title = (TextView)findViewById(R.id.title);
		final String podcastName = mData.getString(PodcastKeys.NAME);
		title.setText(podcastName);
		
		//Set the background
		mBackground = (ViewGroup) findViewById(R.id.identica_body);
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
							Toast.makeText(IdenticaActivity.this, R.string.sending_dent, 
									 Toast.LENGTH_SHORT).show();
							mIdenticaHandler.new PostDent().execute(text);
						}
						mDentText.setText(mTagText);
						mDentText.setSelection(mDentText.length()); //Moves cursor to the end
						new GetLatestDents().execute();
					}
				} else {
					Toast.makeText(IdenticaActivity.this, R.string.login_failed, 
							Toast.LENGTH_LONG).show();
					//Fire off AccountSetup screen
					startActivityForResult(new Intent(
							IdenticaActivity.this,IdenticaAccountSetupActivity.class),
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


	/* (non-Javadoc)
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		String text = mDentText.getText().toString();
		outState.putString(DentKeys.PARTIALDENT, text);
	}

	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    doUnbindIdenticaHandler();
	}
	/**
	 * We subclass ArrayAdapter to handle our specific Dent ListArray.  The override of
	 * getView() provides a mapping between the fields of a Dent we want to view and the 
	 * TextView/ImageView instances in which we want them to appear.
	 *
	 */
	private class DentAdapter extends ArrayAdapter<Dent> {
		private ArrayList<Dent> items;

        public DentAdapter(Context context, int textViewResourceId, ArrayList<Dent> items) {
                super(context, textViewResourceId, items);
                this.items = items;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.dent, null);
                }
                Dent dent = items.get(position);
                if (dent != null) {
                        TextView content = (TextView) v.findViewById(R.id.dent_content);
                        TextView author = (TextView) v.findViewById(R.id.dent_author);
                        TextView userName = (TextView) v.findViewById(R.id.dent_screen_name);
                        RemoteImageView avatar = (RemoteImageView) v.findViewById(R.id.avatar);
                        if (content != null) {
                              content.setText(dent.getTitle());                            
                        }
                        if(author != null){
                              author.setText(dent.getUser());
                        }
                        if (userName != null && dent.getUserScreenName() != null){
                        	userName.setText(" -- " + dent.getUserScreenName());
                        }
                        if (avatar != null){
                        	String url = dent.getAvatarURI();
                        	if (url!= null && !"".equals(url) && !"null".equalsIgnoreCase(url)){
                        		avatar.setRemoteURI(url);
                        		avatar.loadImage();
                        	}
                        }
                }
                return v;
        }
	}
	
	protected class GetLatestDents extends AsyncTask<Void,Void,ArrayList<Dent> > {
		@Override
		protected ArrayList<Dent> doInBackground(Void... params) {
			ArrayList<Dent> dents;
			//Check for connectivity first.
			if (mPonyExpressApp.getInternetHelper().checkConnectivity()){
				if (!mGroupDents){ //Check if using group tag or not
					final String ep_number = mData.getString(EpisodeKeys.EP_NUMBER);
					dents = mIdenticaHandler.queryIdentica(mIdenticaTag +
							ep_number + ".xml");
				}else{
					dents = mIdenticaHandler.queryIdenticaGroup(mIdenticaGroup + ".xml");
				}
				if (dents.isEmpty()){ //If no dents then say so
					Dent no_dents = new Dent();
					no_dents.setTitle(getString(R.string.no_dents));
					dents = new ArrayList<Dent>(1);
					dents.add(no_dents);
				}
			} else { //No connection, so say so
				Dent no_dents = new Dent();
				no_dents.setTitle(getString(R.string.conn_err_query_failed));
				dents = new ArrayList<Dent>(1);
				dents.add(no_dents);
			}
			return dents;
		}
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(ArrayList<Dent> dents) {
			//Create a ListAdaptor to map dents to the ListView.
			DentAdapter adapter = new DentAdapter(mPonyExpressApp, R.layout.dent, dents);
			setListAdapter(adapter);
		}
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		 MenuInflater inflater = getMenuInflater();
		    inflater.inflate(R.menu.general_options_menu, menu);
		    return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    case R.id.settings_menu:
	        startActivity(new Intent(
	        		mPonyExpressApp,PreferencesActivity.class));
	        return true;
	    case R.id.identica_account_settings:
	    	//Fire off AccountSetup screen
			startActivityForResult(new Intent(
					mPonyExpressApp,IdenticaAccountSetupActivity.class),
					SETUP_ACCOUNT);
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
		}
	}
	
	/**
	 * Bring up the IdenticaSettings via a button click.
	 * @param v, a reference to the button that was clicked to call this.
	 */
	public void showSettings(View v){
		startActivity(new Intent(
        		mPonyExpressApp,IdenticaAccountSetupActivity.class));
	}
	
	/**
	 * Go back down the task stack via a button click.
	 * @param v, a reference to the button that was clicked to call this.
	 */
	public void goBack(View v) {
		finish();
	}
}

