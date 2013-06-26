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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;

/**Utility class to handle bitmap loading, resizing and caching.
 * Based in part on ImageManager in Mustard which is Copyright 2009 Google Inc
 * and under the Apache Licence.
 */
public class BitmapManager {
	
	private final static String TAG = "BitmapManager";
	private static final int CONNECTION_TIMEOUT_MS = 20 * 1000;
	private static final int SOCKET_TIMEOUT_MS = 20 * 1000;
	private static final int BUFFER_SIZE = 8192;
	private MessageDigest mDigest;
	private Context mContext;
	private LruCache<String, BitmapDrawable> mThumbnailCache; //url string is the key
	private int mMaxBitmapWidth;
	private int mMaxBitmapHeight;
	
	public BitmapManager(Context context) {
		mContext = context;
		try {
		      mDigest = MessageDigest.getInstance("MD5");
		    } catch (NoSuchAlgorithmException e) {
		      // This shouldn't happen.
		      throw new RuntimeException("No MD5 algorithm.");
		    }
		//get the maximum dimensions of a bitmap from the screen size.
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		mMaxBitmapWidth = display.getWidth();
		mMaxBitmapHeight = display.getHeight();
		Log.d(TAG, "Max Width = " + mMaxBitmapWidth);
		Log.d(TAG, "Max Height = " + mMaxBitmapHeight);
		
		//Get the maximum memory available and set the cache to use an 8th of it.
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
	    int memClassBytes = am.getMemoryClass() * 1024 * 1024;
	    int cacheSize = memClassBytes / 8;
		mThumbnailCache = new LruCache<String, BitmapDrawable>(cacheSize){
			
			@Override
			protected int sizeOf(String key, BitmapDrawable bd) {
				// The cache size will be measured in bytes 
				//rather than number of items.

				return bd.getBitmap().getRowBytes() * bd.getBitmap().getHeight(); 
			}

			/* (non-Javadoc)
			 * @see android.support.v4.util.LruCache#entryRemoved(boolean, java.lang.Object, java.lang.Object, java.lang.Object)
			 */
			@Override
			protected void entryRemoved(boolean evicted, String key,
					BitmapDrawable oldValue, BitmapDrawable newValue) {
				Log.d(TAG, "removed from cache");
				if (AsyncDrawable.class.isInstance(oldValue)){
					//Notify that it has been removed from cache
					((AsyncDrawable) oldValue).setIsCached(false);
				}
			}
			
		};
		
		Log.d(TAG,"Cache size = " + cacheSize);
		
	}
	
	
	
	void addBitmapToCache(String url, BitmapDrawable image){
		if (getBitmapFromCache(url) == null){
			Log.d(TAG, "Image added to cache");
			//Mark as in the cache so it is not gc'ed
			Log.d(TAG, "Image is " + image.getClass());
			if (AsyncDrawable.class.isInstance(image)){
				((AsyncDrawable) image).setIsCached(true);
			}
			mThumbnailCache.put(url, image);
		}
	}
	
	private BitmapDrawable getBitmapFromCache(String url){
		Log.d(TAG, "Fetching " + url + " from cache");
		if (url.equals(null)){
			return null;
		} else {
			return mThumbnailCache.get(url);
		}	
	}
	
	Bitmap fetchImage(String url) throws IOException {
		
		Log.d(TAG, "Fetching image: " + url);

	    InputStream is = getHttpInputStream(url);
	    BitmapFactory.Options opts = getBitmapOptions(is);
	    if (opts.outWidth > mMaxBitmapWidth || opts.outHeight > mMaxBitmapHeight){
	    	//downsample the bitmap as it is too large
	    	Log.d(TAG, "Downsampling the bitmap!");
	    	opts.inSampleSize = calculateInSampleSize(opts, mMaxBitmapWidth, mMaxBitmapHeight);
	    	Log.d(TAG,"inSampleSize = " + opts.inSampleSize);
	    }
	    is.close();
	    //Now get the bitmap
	    opts.inJustDecodeBounds = false;
	    opts.inDither=false;                     //Disable Dithering mode
	    opts.inPurgeable=true;                   //Tell the gc that when it needs free memory, the Bitmap can be cleared
	    opts.inInputShareable=true; 
	    opts.inTempStorage=new byte[16 * 1024];
	    
	    //Need to open a new inputstream as cannot read from it a second time.
	    is = getHttpInputStream(url);
	   
	    Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);
	    is.close();

	    return bitmap;
	  }
	
	private InputStream getHttpInputStream(String url) throws IOException{
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
	    return new BufferedInputStream(bhe.getContent(),BUFFER_SIZE);
	}

	
	/**
	 * Bitmap is saved to disk using the MD5 hashed url string as a filename
	 * @param url
	 * @param bitmap
	 */
	void writeFile(String url, Bitmap bitmap) {
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
	
	private String getMd5(String url){
		mDigest.update(url.getBytes());
		return getHashString(mDigest);
	}

	/**
	 * See if image is saved in the file system.
	 * @param url
	 * @return
	 */
	boolean imageOnDisk(String url){
		return (lookupFile(url, 0, 0) != null);
	}
	
	/**
	 * Looks for an album art file on the disk, resampling it if a width and
	 *  height are not 0.
	 * @param url, width, height
	 * @return bitmap from disc
	 */
	Bitmap lookupFile(String url, int width, int height) {
		if (url == null)
		{
			// No url -> no bitmap.
			return null;
		}
		String hashedUrl = getMd5(url);
	    FileInputStream fis = null;
	    Log.d(TAG, "Looking for file " + url + " on disk" );
	    Bitmap bitmap;
	    if (width == 0 || height == 0){
	    	//Get unsampled bitmap
	    	try {
	    		fis = mContext.openFileInput(hashedUrl);
	    		bitmap = BitmapFactory.decodeStream(fis);

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
	    }else {
	    	//resample bitmap first
	    	bitmap = decodeSampledBitmap(url, width, height);
	    	return bitmap;
	    }
	  }
	 
	
	/** 
	 * Method to get the size and mime type of a bitmap.
	 * @param Bitmap file url
	 * @return BitmapFactory.options for the bitmap
	 */
	private BitmapFactory.Options getBitmapOptions(InputStream is){
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(is, null, options);
		
		return options;
	}
	private BitmapFactory.Options getBitmapOptions(String url){
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(url, options);
		
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
	
//	private Bitmap decodeSampledBitmapFromStream(InputStream is, int reqWidth, 
//			int reqHeight){
//		// First decode with inJustDecodeBounds=true to check dimensions
//		BitmapFactory.Options options = getBitmapOptions(is);
//		// Calculate inSampleSize
//	    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
//	    // Decode bitmap with inSampleSize set
//	    options.inJustDecodeBounds = false;
//	    Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
//	    
//		return bitmap;
//	}
	
	private Bitmap decodeSampledBitmap(String url, int reqWidth, int reqHeight){
		// First decode with inJustDecodeBounds=true to check dimensions
		BitmapFactory.Options options = getBitmapOptions(url);
		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		final String hashedUrl = getMd5(url);
		File path = mContext.getFileStreamPath(hashedUrl);
		Bitmap bitmap = BitmapFactory.decodeFile(path.getAbsolutePath(), options);
		
		return bitmap;
	}
	
	
	/**
	 * Retrieves a larger bitmap from an album art url from disc.
	 * 
	 * @param url
	 * @return
	 */
	private Bitmap loadImage(String url){
		Bitmap bitmap = null;
		if (bitmap == null){
			//FIXME May need an async task here
			bitmap = lookupFile(url, 0, 0);
			Log.d(TAG, "Looking up image from disc");
		}
		return bitmap;
	}
	
	/**
	 * Retrives a bitmap and puts it in an imageView.
	 * @param url
	 * @param imageView
	 */
	public void loadImage(String url, RecyclingImageView imageView){
		BitmapDrawable bd = getBitmapFromCache(url);
		if (bd != null){
			imageView.setImageDrawable(bd);
		} else {
			if (cancelPotentialWork(url, imageView)) {
				BitmapWorkerTask task = new BitmapWorkerTask(mContext, imageView);
				final AsyncDrawable asyncDrawable =
		                new AsyncDrawable(mContext.getResources(), null, task);
		        imageView.setImageDrawable(asyncDrawable);
				task.execute(url);
			}
		}
		
	}
	private static boolean cancelPotentialWork(String data, RecyclingImageView imageView) {
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
	
	static BitmapWorkerTask getBitmapWorkerTask(RecyclingImageView imageView) {
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
	
	
	
}

