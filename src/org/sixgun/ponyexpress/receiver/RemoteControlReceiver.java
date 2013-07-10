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
package org.sixgun.ponyexpress.receiver;

import org.sixgun.ponyexpress.service.PodcastPlayer;
import org.sixgun.ponyexpress.util.PonyLogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;


public class RemoteControlReceiver extends BroadcastReceiver {

	public static final String ACTION = "action";
	private static final String TAG = "RemoteControlReceiver";

	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {			
			//Create intent to send to the PodcastPlayer.
			Intent serviceIntent = new Intent(context,PodcastPlayer.class);
			
			KeyEvent button = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			//Each press sends two intents, one for button down, one for up.  
			//So we need to just select one of them.
			if (KeyEvent.ACTION_DOWN == button.getAction()){
				switch (button.getKeyCode()){
				//TODO **BUGWATCH** Different headsets may use different button codes,
				case KeyEvent.KEYCODE_MEDIA_REWIND:
					//Fallthrough
				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					PonyLogger.d(TAG, "Rewind received");
					serviceIntent.putExtra(ACTION, PodcastPlayer.REWIND);
					context.startService(serviceIntent);
					break;
				case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
					//Fallthrough
				case KeyEvent.KEYCODE_HEADSETHOOK:
					PonyLogger.d(TAG, "Play/Pause received");
					serviceIntent.putExtra(ACTION, PodcastPlayer.PLAY_PAUSE);
					context.startService(serviceIntent);
					break;
				case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
					//Fallthrough
					//TODO Should use skip to next properly, can't test, no headset.
				case KeyEvent.KEYCODE_MEDIA_NEXT:
					PonyLogger.d(TAG, "Fast forward received");
					serviceIntent.putExtra(ACTION, PodcastPlayer.FASTFORWARD);
					context.startService(serviceIntent);
					break;
				default:
					PonyLogger.w(TAG, String.valueOf(button.getKeyCode()));
				}
			}
        }
		

	}

}
