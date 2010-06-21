/*
 * Copyright 2010 Paul Elms
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
package org.sixgun.ponyexpress;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Handles starting IdenticaHandler.
 *
 */
public class IdenticaActivity extends ListActivity {
	
	private static final String TAG = "PonyExpress IdenticaActivity";
	protected PonyExpressApp mPonyExpressApp; 
	protected IdenticaHandler mIdenticaHandler;
	private boolean mIdenticaHandlerBound;
	
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
			getLatestDents();
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
		setContentView(R.layout.identica);
	}

	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    doUnbindIdenticaHandler();
	}
	
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
                        if (content != null) {
                              content.setText(dent.getTitle());                            }
                        if(author != null){
                              author.setText(dent.getAuthor());
                        }
                }
                return v;
        }
	}
	
	protected void getLatestDents() {
		//Check for connectivity first.
		if (mPonyExpressApp.getInternetHelper().checkConnectivity()){
			Bundle data = getIntent().getExtras();
			final String ep_number = data.getString(EpisodeKeys.EP_NUMBER);
			ArrayList<Dent> dents = mIdenticaHandler.queryIdentica("#lo" + ep_number);
			
			//Create a ListAdaptor to map dents to the ListView.
			DentAdapter adapter = new DentAdapter(this, R.layout.dent, dents);
			setListAdapter(adapter);
		}
	}
}

