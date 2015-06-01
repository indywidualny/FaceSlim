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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class RssHandler extends DefaultHandler {
	
	private RssFeed rssFeed;
	private RssItem rssItem;
	private StringBuilder stringBuilder;

	@Override
	public void startDocument() {
		rssFeed = new RssFeed();
	}
	
	/**
	 * Return the parsed RssFeed with it's RssItems
	 * @return
	 */
	public RssFeed getResult() {
		return rssFeed;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		stringBuilder = new StringBuilder();
		
		if(qName.equals("item") && rssFeed != null) {
			rssItem = new RssItem();
			rssItem.setFeed(rssFeed);
			rssFeed.addRssItem(rssItem);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		stringBuilder.append(ch, start, length);
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) {
		
		if(rssFeed != null && rssItem == null) {
			// Parse feed properties
			
			try {
				if (qName != null && qName.length() > 0) {
				    String methodName = "set" + qName.substring(0, 1).toUpperCase() + qName.substring(1);
				    Method method = rssFeed.getClass().getMethod(methodName, String.class);
				    method.invoke(rssFeed, stringBuilder.toString());
				}
			} catch (SecurityException e) {
			} catch (NoSuchMethodException e) {
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			}
			
		} else if (rssItem != null) {
			// Parse item properties
			
			try {
				if(qName.equals("content:encoded")) 
					qName = "content";
				String methodName = "set" + qName.substring(0, 1).toUpperCase() + qName.substring(1);
				Method method = rssItem.getClass().getMethod(methodName, String.class);
				method.invoke(rssItem, stringBuilder.toString());
			} catch (SecurityException e) {
			} catch (NoSuchMethodException e) {
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			}
		}
		
	}

}
