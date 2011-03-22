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

/**
 * SubClass of Episode holding information about an episode that is being downloaded.
 * Used by the DownloaderService in a list enables concurrent downloads.
 */
public class DownloadingEpisode extends Episode {
	private String mPodcastName;
	private String mPodcastPath;
	private int mDownloadProgress;
	private int mSize; // This differs to Episode mLength in that mLength is a string 
	// representation of the int mSize.
	private long mRowID;
	private boolean mDownloadFailed = false;
	private boolean mDownloadCancelled = false;
	
	public DownloadingEpisode(){
		mDownloadProgress = 0;
	}
	
//	/**
//     * Copy constructor.  
//     * @param episode to copy.
//     */
//	public DownloadingEpisode(DownloadingEpisode episode){
//		super(episode);
//		mPodcastName = episode.getPodcastName();
//		mPodcastPath = episode.getPodcastPath();
//		mDownloadProgress = episode.getDownloadProgress();
//		mSize = episode.getSize();
//		mRowID = episode.getRowID();
//	}
	
	/**
	 * @param mPodcastName the mPodcastName to set
	 */
	public void setPodcastName(String mPodcastName) {
		this.mPodcastName = mPodcastName;
	}
	/**
	 * @param mPodcastPath the mPodcastPath to set
	 */
	public void setPodcastPath(String mPodcastPath) {
		this.mPodcastPath = mPodcastPath;
	}
	/**
	 * @return the mPodcastPath
	 */
	public String getPodcastPath() {
		return mPodcastPath;
	}
	/**
	 * @return the mPodcastName
	 */
	public String getPodcastName() {
		return mPodcastName;
	}
	/**
	 * @param mDownloadProgress the mDownloadProgress to set
	 */
	public void setDownloadProgress(int mDownloadProgress) {
		this.mDownloadProgress = mDownloadProgress;
	}
	/**
	 * @return the mDownloadProgress
	 */
	public int getDownloadProgress() {
		return mDownloadProgress;
	}
	/**
	 * @param mSize the mSize to set
	 */
	public void setSize(int mSize) {
		this.mSize = mSize;
	}
	/**
	 * @return the mSize
	 */
	public int getSize() {
		return mSize;
	}
	/**
	 * @param mRowID the mRowID to set
	 */
	public void setRowID(long mRowID) {
		this.mRowID = mRowID;
	}
	/**
	 * @return the mRowID
	 */
	public long getRowID() {
		return mRowID;
	}

	public void setDownloadFailed() {
		this.mDownloadFailed  = true;		
	}

	public boolean getDownloadFailed() {
		return this.mDownloadFailed;
		
	}

	public void resetDownloadFailed() {
		this.mDownloadFailed = false;
		
	}
	
	public void setDownloadCancelled(){
		this.mDownloadCancelled = true;
	}

	public boolean downloadCancelled() {
		return mDownloadCancelled;
	}
	
	public void resetDownloadCancelled() {
		this.mDownloadCancelled = false;
		
	}
}
