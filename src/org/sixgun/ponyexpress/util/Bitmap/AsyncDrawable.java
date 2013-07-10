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

import java.lang.ref.WeakReference;

import org.sixgun.ponyexpress.util.PonyLogger;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

/**
 * Stores a reference back to the BitmapWorkerTask which is handling the 
 * bitmap.  A placeholder image is displayed while it works.
 *
 */
class AsyncDrawable extends BitmapDrawable {
	private static final String TAG = "AsyncDrawable";

	private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

	private int mCacheRefCount = 0;
	private int mDisplayRefCount = 0;

	private boolean mHasBeenDisplayed;

	public AsyncDrawable(Resources res, Bitmap bitmap,
			BitmapWorkerTask bitmapWorkerTask) {
		super(res, bitmap);
		bitmapWorkerTaskReference =
				new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
	}

	public BitmapWorkerTask getBitmapWorkerTask() {
		return bitmapWorkerTaskReference.get();
	}

	// Notify the drawable that the displayed state has changed.
	// Keep a count to determine when the drawable is no longer displayed.
	public void setIsDisplayed(boolean isDisplayed) {
		synchronized (this) {
			if (isDisplayed) {
				mDisplayRefCount++;
				mHasBeenDisplayed = true;
				PonyLogger.d(TAG, "IsDisplayed(true) Called");
			} else {
				mDisplayRefCount--;
				PonyLogger.d(TAG, "IsDisplayed(false) Called");
			}
		}
		// Check to see if recycle() can be called.
		checkState();
	}

	// Notify the drawable that the cache state has changed.
	// Keep a count to determine when the drawable is no longer being cached.
	public void setIsCached(boolean isCached) {
		synchronized (this) {
			if (isCached) {
				mCacheRefCount++;
				PonyLogger.d(TAG, "Is cached(true) called");
			} else {
				mCacheRefCount--;
				PonyLogger.d(TAG, "Is cached(false) called");
			}
		}
		// Check to see if recycle() can be called.
		checkState();
	}

	private synchronized void checkState() {
		// If the drawable cache and display ref counts = 0, and this drawable
		// has been displayed, then recycle.
		PonyLogger.d(TAG, "CacheCount = " + mCacheRefCount);
		PonyLogger.d(TAG, "DisplayCount = " + mDisplayRefCount);
		if (mCacheRefCount <= 0 && mDisplayRefCount <= 0 && mHasBeenDisplayed
				&& hasValidBitmap()) {
			getBitmap().recycle();
			PonyLogger.d(TAG, "Bitmap recycle called!!");
		}
	}

	private synchronized boolean hasValidBitmap() {
		Bitmap bitmap = getBitmap();
		return bitmap != null && !bitmap.isRecycled();
	}
}