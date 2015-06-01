/*
 * Copyright (C) 2011 Mats Hofman <http://matshofman.nl/contact/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.matshofman.saxrssreader;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class RssFeed implements Parcelable {

	private String title;
	private String link;
	private String description;
	private String language;
	private ArrayList<RssItem> rssItems;
	
	public RssFeed() {
		rssItems = new ArrayList<RssItem>();
	}
	
	public RssFeed(Parcel source) {
		
		Bundle data = source.readBundle();
		title = data.getString("title");
		link = data.getString("link");
		description = data.getString("description");
		language = data.getString("language");
		rssItems = data.getParcelableArrayList("rssItems");
		
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		
		Bundle data = new Bundle();
		data.putString("title", title);
		data.putString("link", link);
		data.putString("description", description);
		data.putString("language", language);
		data.putParcelableArrayList("rssItems", rssItems);
		dest.writeBundle(data);
	}
	
	public static final Parcelable.Creator<RssFeed> CREATOR = new Parcelable.Creator<RssFeed>() {
		public RssFeed createFromParcel(Parcel data) {
			return new RssFeed(data);
		}
		public RssFeed[] newArray(int size) {
			return new RssFeed[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}
	
	void addRssItem(RssItem rssItem) {
		rssItems.add(rssItem);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public ArrayList<RssItem> getRssItems() {
		return rssItems;
	}

	public void setRssItems(ArrayList<RssItem> rssItems) {
		this.rssItems = rssItems;
	} 
}
