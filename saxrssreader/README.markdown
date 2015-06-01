With this library you can easily parse RSS feeds using the SAX APIs available in Android.

Usage
-----
You can use the API by simply calling `RssReader.read(URL url)`. This will make the request to the url provided and parse it to `RssFeed` and `RssItem` objects. It can't get any easier than this.

Here is an example of how to fetch a RSS feed and iterate through every item:

	URL url = new URL("http://example.com/feed.rss");
	RssFeed feed = RssReader.read(url);

	ArrayList<RssItem> rssItems = feed.getRssItems();
	for(RssItem rssItem : rssItems) {
		Log.i("RSS Reader", rssItem.getTitle());
	}

License
-----
Copyright (c) 2011 Mats Hofman

Licensed under the Apache License, Version 2.0