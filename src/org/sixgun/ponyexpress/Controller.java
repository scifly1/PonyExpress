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
package org.sixgun.ponyexpress;

import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.content.Context;
import android.os.Process;
import android.util.Log;

public class Controller implements Runnable {

	private static final String TAG = "PonyExpress/MessagingController";

	private static Controller inst = null;
	private BlockingQueue<Command> mCommands = new LinkedBlockingQueue<Command>();

	private boolean mBusy;
	private Thread mThread;

	private HashSet<MessagingListener> mListeners = new HashSet<MessagingListener>();

	protected Controller(Context _context) {
		mThread = new Thread(this);
		mThread.start();
	}

	/**
	 * Gets or creates the singleton instance of MessagingController. Application is used to
	 * provide a Context to classes that need it.
	 * @param application
	 * @return
	 */
	public synchronized static Controller getInstance(Context _context) {
		if (inst == null) {
			inst = new Controller(_context);
		}
		return inst;
	}

	public boolean isBusy() {
		return mBusy;
	}

	public void run() {
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		while (true) {
			try {
				Command command = mCommands.take();
				if ( command.listener == null || 
						isActiveListener(command.listener) || 
						(command.listener != null && command.singlenotify == true)) {
					mBusy = true;
					command.runnable.run();
					
				}
			}
			catch (Exception e) {
				Log.d(TAG, "Error running command", e);
			}
			mBusy = false;
		}
	}

	public void addListener(MessagingListener listener) {
		synchronized (mListeners) {
			mListeners.add(listener);
		}
	}

	public void removeListener(MessagingListener listener) {
		synchronized (mListeners) {
			mListeners.remove(listener);
		}
	}

	private boolean isActiveListener(MessagingListener listener) {
		synchronized (mListeners) {
			return mListeners.contains(listener);
		}
	}

	private void put(String description, MessagingListener listener, boolean singlenotify, Runnable runnable) {
		try {
			Command command = new Command();
			command.listener = listener;
			command.runnable = runnable;
			command.description = description;
			command.singlenotify = singlenotify;
			mCommands.add(command);
		}
		catch (IllegalStateException ie) {
			throw new Error(ie);
		}
	}

	class Command {
		public Runnable runnable;

		public MessagingListener listener;

		public String description;

		public boolean singlenotify;
		
		@Override
		public String toString() {
			return description;
		}
	}

	public void loadRemoteImage(final Context context, 
			final String imageUrl,
			final MessagingListener listener) {

		
		put("loadRemoteImage", listener, true, new Runnable() {
			public void run() {
				
				try {
					PonyExpressApp.sImageManager.put(imageUrl);
					put("loadRemoteImageFinished", listener, true, new Runnable() {
						public void run() {
							listener.loadRemoteImageFinished(context);
						}
					});
				} catch (final Exception e) {
					Log.e(TAG, "Exception:", e);
				} 
			}
		});
	

	}

}
