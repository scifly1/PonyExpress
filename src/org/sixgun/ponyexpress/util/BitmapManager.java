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
package org.sixgun.ponyexpress.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.sixgun.ponyexpress.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;

/**Utility class to handle bitmap loading, resizing and caching.
 * Based in part on ImageManager in Mustard which is Copyright 2009 Google Inc
 * and under the Apache Licence.
 */
public class BitmapManager {
	
	private final static String TAG = "BitmapManager";
	private static final int CONNECTION_TIMEOUT_MS = 10 * 1000;
	private static final int SOCKET_TIMEOUT_MS = 10 * 1000;
	private MessageDigest mDigest;
	private Context mContext;
	private LruCache<String, Bitmap> mThumbnailCache; //url string is the key
	private static final int MAX_CACHE_SIZE = 25; //number of bitmaps
	public static final int DEFAULT_IMAGE_HEIGHT = 115;
	public static final int DEFAULT_IMAGE_WIDTH = 115;
	private Bitmap mPlaceholderBitmap;
	
	public BitmapManager(Context context) {
		mContext = context;
		try {
		      mDigest = MessageDigest.getInstance("MD5");
		    } catch (NoSuchAlgorithmException e) {
		      // This shouldn't happen.
		      throw new RuntimeException("No MD5 algorithm.");
		    }

	    mThumbnailCache = new LruCache<String, Bitmap>(MAX_CACHE_SIZE);
	    mPlaceholderBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.nullavatar);
	}
	
	private void addBitmapToCache(String url, Bitmap image){
		if (getBitmapFromCache(url) == null){
			if (!url.equals(null) && image != null){
				mThumbnailCache.put(url, image);
			}
		}
	}
	
	private Bitmap getBitmapFromCache(String url){
		Log.d(TAG, "Fetching " + url + " from cache");
		if (url.equals(null)){
			return null;
		} else 
			return mThumbnailCache.get(url);
	}
	
	private Bitmap fetchImage(String url) throws IOException {
		
		Log.d(TAG, "Fetching image: " + url);

	    HttpGet get = new HttpGet(url);
	    HttpConnectionParams.setConnectionTimeout(get.getParams(),
	        CONNECTION_TIMEOUT_MS);
	    HttpConnectionParams.setSoTimeout(get.getParams(),
	        SOCKET_TIMEOUT_MS);

	    HttpResponse response;
	    HttpClient client = new DefaultHttpClient();;

	    try {
	      response = client.execute(get);
	    } catch (ClientProtocolException e) {
	    	Log.e(TAG, e.getMessage(), e);
	      throw new IOException("Invalid client protocol.");
	    }

	    if (response.getStatusLine().getStatusCode() != 200) {
	      throw new IOException("Non OK response: " +
	          response.getStatusLine().getStatusCode());
	    }
	    
	    HttpEntity entity = response.getEntity();
	    BufferedHttpEntity bhe = new BufferedHttpEntity(entity);
	    InputStream is = bhe.getContent();
	    Bitmap bitmap = BitmapFactory.decodeStream(is);
	    is.close();

	    return bitmap;
	  }

	
	/**
	 * Bitmap is saved to disk using the MD5 hashed url string as a filename.
	 * @param url
	 * @param bitmap
	 */
	private void writeFile(String url, Bitmap bitmap) {
	    if (bitmap == null) {
	    	Log.w(TAG, "Can't write file. Bitmap is null.");
	      return;
	    }

	    String hashedUrl = getMd5(url);

	    FileOutputStream fos;

	    try {
	      fos = mContext.openFileOutput(hashedUrl,
	          Context.MODE_PRIVATE);
	    } catch (FileNotFoundException e) {
	    	Log.w(TAG, "Error creating file.");
	      return;
	    }

	    Log.i(TAG, "Writing file: " + hashedUrl);
	    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);

	    try {
	      fos.close();
	    } catch (IOException e) {
	    	Log.w(TAG, "Could not close file.");
	    }
	    
	  }
	
	/**
	 * MD5 hashes are used as filenames for the image files
	 */
	private String getHashString(MessageDigest digest) {
	    StringBuilder builder = new StringBuilder();

	    for (byte b : digest.digest()) {
	      builder.append(Integer.toHexString((b >> 4) & 0xf));
	      builder.append(Integer.toHexString(b & 0xf));
	    }

	    return builder.toString();
	  }
	
	private String getMd5(String url) {
	    mDigest.update(url.getBytes());

	    return getHashString(mDigest);
	  }
	/**
	 * See if image is saved in the file system.
	 * @param url
	 * @return
	 */
	private boolean imageOnDisk(String url){
		return (lookupFile(url) != null);
	}
	
	private Bitmap lookupFile(String url) {
		  if (url == null)
		  {
			  // No url -> no bitmap.
			  return null;
		  }
	    String hashedUrl = getMd5(url);
	    FileInputStream fis = null;
	    Log.d(TAG, "Looking for file " + url + "on disk" );
	    try {
	      fis = mContext.openFileInput(hashedUrl);
	      Bitmap bitmap = BitmapFactory.decodeStream(fis);
	      addBitmapToCache(url, bitmap);
	      return bitmap;
	    } catch (FileNotFoundException e) {
	      // Not there.
	      return null;
	    } finally {
	      if (fis != null) {
	        try {
	          fis.close();
	        } catch (IOException e) {
	          // Ignore.
	        }
	      }
	    }
	  }
	 
	
	/** 
	 * Method to get the size and mime type of a bitmap.
	 * @param Bitmap file url
	 * @return BitmapFactory.options for the bitmap
	 */
	private BitmapFactory.Options getBitmapOptions(String url){
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		final String hashedUrl = getMd5(url);
		File path = mContext.getFileStreamPath(hashedUrl);
		BitmapFactory.decodeFile(path.getAbsolutePath(), options);
		return options;
	}
	
	/**
	 * Determine if the full image should be loaded into memory or subsampled
	 * first.
	 * @param options
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 */
	private static int calculateInSampleSize(
			BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// Calculate ratios of height and width to requested height and width
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will guarantee
			// a final image with both dimensions larger than or equal to the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}
	
	private Bitmap decodeSampledBitmap(String url, int reqWidth, 
			int reqHeight) {

	    // First decode with inJustDecodeBounds=true to check dimensions
	    BitmapFactory.Options options = getBitmapOptions(url);

	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    final String hashedUrl = getMd5(url);
		File path = mContext.getFileStreamPath(hashedUrl);
		Bitmap bitmap = BitmapFactory.decodeFile(path.getAbsolutePath(), options);
		addBitmapToCache(url, bitmap);
		return bitmap;
	}
	/**
	 * Retrieves a bitmap from an album art url, first looking in the cache,
	 * then on disc.
	 * 
	 * @param url
	 * @return
	 */
	private Bitmap loadImage(String url){
		Bitmap bitmap = null;// getBitmapFromCache(url);
		if (bitmap == null){
			//May need an async task here
			bitmap = lookupFile(url);
			Log.d(TAG, "Looking up image from disc");
		}
		return bitmap;
	}
	
	/**
	 * Retrives a bitmap and puts it in an imageView.
	 * @param url
	 * @param imageView
	 */
	public void loadImage(String url, ImageView imageView){
		Bitmap bitmap = getBitmapFromCache(url);
		if (bitmap != null){
			imageView.setImageBitmap(bitmap);
		} else {
			if (cancelPotentialWork(url, imageView)) {
				BitmapWorkerTask task = new BitmapWorkerTask(imageView);
				final AsyncDrawable asyncDrawable =
		                new AsyncDrawable(mContext.getResources(), mPlaceholderBitmap, task);
		        imageView.setImageDrawable(asyncDrawable);
				task.execute(url);
			}
		}
		
	}
	private static boolean cancelPotentialWork(String data, ImageView imageView) {
	    final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

	    if (bitmapWorkerTask != null) {
	        final String bitmapUrl = bitmapWorkerTask.url;
	        if (!bitmapUrl.equals(data)){
	            // Cancel previous task
	            bitmapWorkerTask.cancel(true);
	        } else {
	            // The same work is already in progress
	            return false;
	        }
	    }
	    // No task associated with the ImageView, or an existing task was cancelled
	    return true;
	}
	
	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		   if (imageView != null) {
		       final Drawable drawable = imageView.getDrawable();
		       if (drawable instanceof AsyncDrawable) {
		           final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
		           return asyncDrawable.getBitmapWorkerTask();
		       }
		    }
		    return null;
		}
	
	public void clear() {
	    String [] files = mContext.fileList();

	    for (String file : files) {
	      mContext.deleteFile(file);
	    }
	    
	    mThumbnailCache.evictAll();
	  }
	/**
	 * Creates a BitmapDrawable of a specific size from the given album art.
	 * @param res
	 * @param url
	 * @param height
	 * @param width
	 * @return BitmapDrawable
	 */
	public BitmapDrawable createBackgroundFromAlbumArt(Resources res, String url, int height, int width){
		Bitmap art = loadImage(url);
		Bitmap new_image;
		if (height >  width){
			new_image = Bitmap.createScaledBitmap(art, height, height, true);
		} else {
			new_image = Bitmap.createScaledBitmap(art, width, width, true);	
		}
		BitmapDrawable new_background = new BitmapDrawable(res, new_image);
		
		new_background.setGravity(Gravity.TOP| Gravity.LEFT);
		new_background.setAlpha(80);
		return new_background;
	}
	
	
	/**
	 *Image download and resampling is handled by this AsyncTask 
	 *
	 */
	class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
	    private final WeakReference<ImageView> imageViewReference;
	    private String url;

	    public BitmapWorkerTask(ImageView imageView) {
	        // Use a WeakReference to ensure the ImageView can be garbage collected
	        imageViewReference = new WeakReference<ImageView>(imageView);
	       
	    }

	    // Decode image in background.
	    @Override
	    protected Bitmap doInBackground(String... params) {
	    	//Download image if not on disc
	    	url = params[0];
	    	if (!imageOnDisk(url)){
	    		Bitmap bitmap = null;
				try {
					bitmap = fetchImage(url);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    		if (bitmap != null){
	    			writeFile(url, bitmap);
	    			final ImageView iv = imageViewReference.get();
	    			int image_height = DEFAULT_IMAGE_HEIGHT;
	    			int image_width = DEFAULT_IMAGE_WIDTH;
	    			if (iv != null){
	    				image_height = iv.getHeight();
	    				image_width = iv.getWidth();
	    			}
	    			return decodeSampledBitmap(url,image_width,image_height);
	    		} else {
	    			return null;
	    		}
	    	}
			return lookupFile(url);
	    	
	    }

	    // Once complete, get the correct ImageView (if still around) and set bitmap.
	    @Override
	    protected void onPostExecute(Bitmap bitmap) {
	    	if (isCancelled()) {
	    		bitmap = null;
	    	}

	    	if (imageViewReference != null && bitmap != null) {
	    		final ImageView imageView = imageViewReference.get();
	    		final BitmapWorkerTask bitmapWorkerTask =
	    				getBitmapWorkerTask(imageView);
	    		if (this == bitmapWorkerTask && imageView != null) {
	    			imageView.setImageBitmap(bitmap);
	    			imageView.refreshDrawableState();
	    		}

	    	}
	    }
	}

	
	/**
	 * Stores a reference back to the BitmapWorkerTask which is handling the 
	 * bitmap.  A placeholder image is displayed while it works.
	 *
	 */
	static class AsyncDrawable extends BitmapDrawable {
	    private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

	    public AsyncDrawable(Resources res, Bitmap bitmap,
	    		BitmapWorkerTask bitmapWorkerTask) {
	        super(res, bitmap);
	        bitmapWorkerTaskReference =
	            new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
	    }

	    public BitmapWorkerTask getBitmapWorkerTask() {
	        return bitmapWorkerTaskReference.get();
	    }
	}
}

