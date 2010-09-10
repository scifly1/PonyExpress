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



public class Dent {
	private String mTitle;
	private String mUser;
	private String mUserScreenName;
	private String mAvatarURI;
	
	//This is a key for saving partially written dents should another activity interupt 
	// the IdenticaActivity.
	public final class DentKeys {
		public static final String PARTIALDENT = "partial_dent";
	};
	
	/**
	 * Constructor. Creates and empty Dent.
	 */
	public Dent() {
	}
	
	/** 
	 * Copy constructor.
	 * @param dent to copy
	 */
	public Dent(Dent dent) {
		this(dent.mTitle,dent.mUser,dent.mUserScreenName, dent.mAvatarURI);
	}
	/**
	 * Private constructor used by the copy constructor.
	 * @param mTitle2
	 * @param mAuthor2
	 */
	private Dent(String _title, String _author, String _screen_name, String _avatar) {
		this.mTitle = _title;
		this.mUser = _author;
		this.mUserScreenName = _screen_name;
		this.mAvatarURI = _avatar;
	}

	/**
	 * @param mTitle the mTitle to set
	 */
	public void setTitle(String mTitle) {
		this.mTitle = mTitle;
	}
	/**
	 * @return the mTitle
	 */
	public String getTitle() {
		return mTitle;
	}
	/**
	 * @param mUser the mUser to set
	 */
	public void setUser(String user) {
		this.mUser = user;
	}
	/**
	 * @return the mUser
	 */
	public String getUser() {
		return mUser;
	}
	/**
	 * @return the mUserScreenName
	 */
	public String getUserScreenName() {
		return mUserScreenName;
	}

	/**
	 * @param mUserScreenName the mUserScreenName to set
	 */
	public void setUserScreenName(String mUserScreenName) {
		this.mUserScreenName = mUserScreenName;
	}
	
	/**
	 * @return the mAvatarURI
	 */
	public String getAvatarURI() {
		return mAvatarURI;
	}

	/**
	 * @param AvatarURL the mAvatarURI to set
	 */
	public void setAvatarURI(String AvatarURL) {
		mAvatarURI = AvatarURL;
	}
}
