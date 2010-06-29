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
	private String mAuthor;
	
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
		this(dent.mTitle,dent.mAuthor);
	}
	/**
	 * Private constructor used by the copy constructor.
	 * @param mTitle2
	 * @param mAuthor2
	 */
	private Dent(String _title, String _author) {
		this.mTitle = _title;
		this.mAuthor = _author;
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
	 * @param mAuthor the mAuthor to set
	 */
	public void setAuthor(String mAuthor) {
		this.mAuthor = mAuthor;
	}
	/**
	 * @return the mAuthor
	 */
	public String getAuthor() {
		return mAuthor;
	}
}
