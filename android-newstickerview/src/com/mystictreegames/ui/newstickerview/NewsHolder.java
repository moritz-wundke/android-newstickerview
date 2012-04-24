package com.mystictreegames.ui.newstickerview;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Container class for our news. Implements the {@link Parcelable} interface and can be added
 * or retrieved from any {@link Parcel}.
 * @author Moritz 'Moss' Wundke (b.thax.dcg@gmail.com)
 *
 */
public class NewsHolder implements Parcelable {
	/** The news text */
	public String	mNews;
	
	/** The news link */
	public String	mLink;
	
	/** The time the news will be shown */
	public Integer	mTime;
	
	/**
	 * Create a news holder with all requited data
	 */
	public NewsHolder( String news, String link, Integer time ) {
		mNews = news;
		mLink = link;
		mTime = time;
	}
	
	/** Create from parcel */
    private NewsHolder(final Parcel in) {
    	mNews = in.readString();
    	mLink = in.readString();
    	mTime = in.readInt();
    }
    
    @Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(mNews);
        dest.writeString(mLink);
        dest.writeInt(mTime);
    }
	
	public static final Parcelable.Creator<NewsHolder> CREATOR = new Parcelable.Creator<NewsHolder>() {
        public NewsHolder createFromParcel(final Parcel in) {
            return new NewsHolder(in);
        }

        public NewsHolder[] newArray(final int size) {
            return new NewsHolder[size];
        }
    };
}