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
package org.sixgun.ponyexpress;

import java.util.ArrayList;
import java.util.List;

import org.sixgun.ponyexpress.miroguide.conn.MiroGuideException;
import org.sixgun.ponyexpress.miroguide.conn.MiroGuideService;
import org.sixgun.ponyexpress.miroguide.model.MiroGuideChannel;
import org.sixgun.ponyexpress.util.PonyLogger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

import com.commonsware.cwac.endless.EndlessAdapter;


public class EndlessChannelListAdapter extends EndlessAdapter {

	

	private static final String TAG = "EndlessChannelListAdapter";
	private String mCategoryName;
	private int mOffset;
	List<MiroGuideChannel> mChannels;
	private RotateAnimation mRotate=null;
	private View mPendingView=null;

	public EndlessChannelListAdapter(Context context, String category_name, 
			ArrayList<MiroGuideChannel> channels) {
		super(new ChannelListAdapter(context, 
				R.layout.episode_row, channels));
		
		mCategoryName = category_name;
		mOffset = channels.size();
		
		mRotate=new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF,
				0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		mRotate.setDuration(600);
		mRotate.setRepeatMode(Animation.RESTART);
		mRotate.setRepeatCount(Animation.INFINITE);
		 
	}
	


	@Override
	protected View getPendingView(ViewGroup parent) {
		View row=LayoutInflater.from(parent.getContext()).inflate(R.layout.pending_row, null);
	    
	    mPendingView=row.findViewById(R.id.throbber);
	    startProgressAnimation();
	    
	    return(row); 
	}



	/* (non-Javadoc)
	 * @see com.commonsware.cwac.endless.EndlessAdapter#appendCachedData()
	 */
	@Override
	protected void appendCachedData() {
		ChannelListAdapter a =  (ChannelListAdapter) getWrappedAdapter();
		if (mChannels != null){
			for (MiroGuideChannel channel : mChannels){
				a.add(channel);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.commonsware.cwac.endless.EndlessAdapter#cacheInBackground()
	 */
	@Override
	protected boolean cacheInBackground() throws Exception {
		mChannels = null;
		MiroGuideService miro = new MiroGuideService();
		//Get more channels
		try {
			mChannels = miro.getChannelList("category", mCategoryName, "name", MiroGuideChannel.DEFAULT_LIMIT, mOffset);
		} catch (MiroGuideException e) {
			PonyLogger.e(TAG, "Could not get Miro channels", e);
		} finally {
			miro.close();
		}
		
		if (mChannels!= null && !mChannels.isEmpty()){
			mOffset += mChannels.size();
			return true;
		} else 
			return false;
	}
	
	void startProgressAnimation() {
	    if (mPendingView!=null) {
	      mPendingView.startAnimation(mRotate);
	    }
	  }

}
