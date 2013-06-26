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
package org.sixgun.ponyexpress.util.Bitmap;

import java.io.IOException;
import java.lang.ref.WeakReference;

import org.sixgun.ponyexpress.PonyExpressApp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;

/**
 *Image download and resampling is handled by this AsyncTask 
 *
 */
class BitmapWorkerTask extends AsyncTask<String, Void, BitmapDrawable> {
    private static final int DEFAULT_IMAGE_HEIGHT = 115;
	private static final int DEFAULT_IMAGE_WIDTH = 115;
	private final WeakReference<RecyclingImageView> imageViewReference;
	private Context mContext;
	
    String url;

    public BitmapWorkerTask(Context context, RecyclingImageView imageView) {
        mContext = context;
    	// Use a WeakReference to ensure the ImageView can be garbage collected
        imageViewReference = new WeakReference<RecyclingImageView>(imageView);
               
    }

    // Decode image in background.
    @Override
    protected BitmapDrawable doInBackground(String... params) {
    	//Download image if not on disc
    	url = params[0];
    	
    	final RecyclingImageView iv = imageViewReference.get();
		int image_height = DEFAULT_IMAGE_HEIGHT;
		int image_width = DEFAULT_IMAGE_WIDTH;
		if (iv != null){
			image_height = iv.getHeight();
			image_width = iv.getWidth();
		}
		
    	if (!PonyExpressApp.sBitmapManager.imageOnDisk(url)){
    		//Download image
    		Bitmap bitmap = null;
    		
			try {
				bitmap = PonyExpressApp.sBitmapManager.fetchImage(url);
			} catch (IOException e) {
				e.printStackTrace();
			}
    		if (bitmap != null){
    			PonyExpressApp.sBitmapManager.writeFile(url, bitmap);
    		} else {
    			return null;
    		}
    		bitmap.recycle();
    		bitmap = null;
    	}
    	//Read bitmap from disc
		Bitmap resampled_bitmap = PonyExpressApp.sBitmapManager.lookupFile(url, image_width, image_height);
		BitmapDrawable bd = null;
		if (resampled_bitmap != null){
			bd = new AsyncDrawable(mContext.getResources(), 
					resampled_bitmap, null);
			PonyExpressApp.sBitmapManager.addBitmapToCache(url, bd);
		}
    	return bd;
    	
    }

    // Once complete, get the correct ImageView (if still around) and set bitmap.
    @Override
    protected void onPostExecute(BitmapDrawable bd) {
    	if (isCancelled()) {
    		bd = null;
    	}

    	if (imageViewReference != null && bd != null) {
    		final RecyclingImageView imageView = imageViewReference.get();
    		final BitmapWorkerTask bitmapWorkerTask =
    				BitmapManager.getBitmapWorkerTask(imageView);
    		if (this == bitmapWorkerTask && imageView != null) {
    			imageView.setImageDrawable(bd);
    			imageView.refreshDrawableState();
    		}

    	}
    }
}
