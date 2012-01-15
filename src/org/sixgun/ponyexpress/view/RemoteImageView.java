/*
 * Copyright (C) 2009-2010 macno.org, Michele Azzolari
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
package org.sixgun.ponyexpress.view;

import org.sixgun.ponyexpress.Controller;
import org.sixgun.ponyexpress.MessagingListener;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.Toast;


public class RemoteImageView extends ImageView {
	private String mRemote;
	private Context mContext;
	private int mResource = R.drawable.nullavatar;
	
	public RemoteImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		setImageResource(R.drawable.nullavatar);
		mContext=context;
	}

	public RemoteImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setImageResource(R.drawable.nullavatar);
		mContext=context;
	}

	public void setRemoteURI(String uri) {
		if (uri.startsWith("http")) {
			mRemote = uri;
		}
	}

	public void loadDefault() {
		setImageResource(R.drawable.pony_icon);
	}
	
	public void loadImage(int resource) {
		mResource=resource;
		if (mRemote != null) {
			if (PonyExpressApp.sImageManager.contains(mRemote)) {
				setFromLocal();
			} else {
				setImageResource(resource);
				doImageDownload();
			}
		}
	}
	
	public void loadImage() {
		loadImage(mResource);
	}
	
	private void doImageDownload() {
		new Thread() {
            public void run() {
            	Controller.getInstance(mContext)
            		.loadRemoteImage(mContext, mRemote, mListener);
            }
		}.start();
	}
	
	private void setFromLocal() {
		Bitmap bm = PonyExpressApp.sImageManager.get(mRemote);
		if(bm != null)
			setImageBitmap(bm);
		else {
			setImageResource(mResource);
		}
	}
	
	private void endLoadRemote() {
		Bitmap bm = PonyExpressApp.sImageManager.get(mRemote);
		if(bm != null)
			setImageBitmap(bm);
		else {
			Toast.makeText(mContext, mContext.getString(R.string.error_generic), Toast.LENGTH_LONG).show();
		}
	}

	private RemoteImageHandler mHandler = new RemoteImageHandler();
	
	class RemoteImageHandler extends Handler {

		private static final int MSG_DOWNLOADED = 2;
		
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_DOWNLOADED:
				endLoadRemote();
				break;
			}
		}

		public void imageDownloaded() {
			sendEmptyMessage(MSG_DOWNLOADED);
		}
	
	}
	
	private MessagingListener mListener = new MessagingListener() {
		public void loadRemoteImageFinished(Context context) {
	    	mHandler.imageDownloaded();
	    }
	};

}
