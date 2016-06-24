## Face Slim [![Build Status](https://travis-ci.org/indywidualny/FaceSlim.svg?branch=master)](https://travis-ci.org/indywidualny/FaceSlim)

With Face Slim you can keep in touch with your friends even if you are not using the official Facebook app! Detailed description and more information below.

[![Face Slim on fdroid.org](https://f-droid.org/wiki/images/0/06/F-Droid-button_get-it-on.png "Download from fdroid.org")](https://f-droid.org/app/org.indywidualni.fblite)

Releases | [pl] wydania: https://github.com/indywidualny/FaceSlim/releases

Face Slim is an unofficial Facebook application for Android devices that resembles the Facebook Mobile Web Application before Facebook started removing features. FaceSlim is free of spyware, unlike what is built into the official Facebook and Facebook Messenger applications.

## Changelog
& what makes the application awesome:

**2.8**
* possibility to change text size (text scale)
* start with the most recent posts
* confirm exiting the app
* better update checking (more info, clickable)

Sometimes to fully activate most recent posts you need to choose recent posts from your Facebook menu either.

**2.7**
* Android N support (the app is targeting upcoming Android release)
* new predefined User Agent which is awesome [#180](../../issues/180)
* blank page after returning to app finally fixed (eg opening external browser)

User Agent **is not** updated automatically. If you upgraded the app to 2.7, change it manually.

If you changed custom User Agent and you want to revert it, the predefined value is  
`Mozilla/5.0 (BB10; Kbd) AppleWebKit/537.10+ (KHTML, like Gecko) Version/10.1.0.4633 Mobile Safari/537.10+`

**2.6**
* notifications reimplemented and fully functional again
* custom User Agent String (to reenable "old" messages tab)
* touch.facebook.com is supported
* app checks for its updates

If you changed custom User Agent and you want to revert it, the predefined value is  
`Mozilla/5.0 (FuckMessenger 1.0; Mobile; rv:41.0) Gecko/41.0 Firefox/41.0`

**2.5**
* mobile Messenger integration. Drawer shortcut to messages opens mobile Messenger now (as well as a launcher shortcut and a message notification) - Facebook decided to disable messages tab soon [#155](../../issues/155)
* geolocation access for checking-in automatically (optional) 
* fix for not blinking LED on some devices 
* minor fixes (thanks to your bug reports) 

**2.4**
* offline mode - When there is no network load pages from a database. It's not perfect and doesn't work for a dynamically loaded content automatically... but it may be useful! [#89](../../issues/89)
* new notifications check interval (2 minutes) [#119](../../issues/119)
* rare bug with keyboard overlaying forms fixed (advanced settings checkbox) [#98](../../issues/98)
* no vibrations when the setting is turned off (fix) [#123](../../issues/123)
* LED for Samsung devices (Marshmallow) should work. I hope ;) [#121](../../issues/121)
* problems with loading a page during app startup fixed
* send button in chat when 'disable images' option is enabled is no longer missing [#114](../../issues/114)
* fixed Facebook blue navigation bar works for all the modes now (normal, basic, zero)
* 'install messenger' notice is hidden by default, it was so annoying
* Bulgarian translation (thanks [pecuna](https://github.com/pecuna))

New way to buy me a beer. Google Play Donation package - see About App page's menu or follow the link:<br />
https://play.google.com/store/apps/details?id=org.indywidualni.faceslim_donation

**2.3**
* some IllegalStateExceptions caught (no more crashes on some devices)
* option to load extra images (thanks [pejakm](https://github.com/pejakm)) - load external images from: googleusercontent.com, tumblr.com, pinimg.com, media.giphy.com
* check is DownloadManager enabled before image saving (if not display a dialog to enable)
* DownloadManager displays some information about a downloaded file now
* Chinese Traditional translation (thanks [dic1911](https://github.com/dic1911))
* French translation (thanks [davidlb](https://github.com/davidlb))
* new messages shortcut icon (thanks [bnbrown](https://github.com/bnbrown))
* translations updated

**2.2**
* message notifications (it's awesome!)
* notifications rebuilt (fully automated, intelligent checker)
* fullscreen video playback support (immersive mode)
* basic mobile version of Facebook
* view / save full size images (with zoom)
* hide news feed (to avoid sidetracking / procrastination)
* hide sponsored posts & ads (beta)
* hide people you may know
* general & minor improvements
* crash reports by e-mail
* save service's logs to file
* code cleanup & refactoring

**2.1**
* camera upload for Lollipop is finally fixed
* black theme is working on every device now [#31](../../issues/31)
* runtime permissions - file access (Marshmallow)
* splash screen is redesigned and shown only during a transition (like Google apps)
* Korean translation (thanks [halcyonest](https://github.com/halcyonest))
* Toolbars instead of deprecated Action Bars (code)
* Tray Preferences instead of deprecated multi-process Shared Preferences (code)
* general refactoring & code cleanup (less lines, more valuable code, no deprecated methods)
* pretty animations for >= Lollipop
* visual improvements for KitKat (statusbar, backgrounds)

App is ready for Android Marshmallow. I said it before but it was premature. Versions 1.8.0 - 2.0.0 have a broken file upload on Marshmallow due to lack of runtime permissions support.

**2.0**
* don't load images (reduce data usage)
* quicker app start
* Spanish translation (thanks [ThecaTTony](https://github.com/ThecaTTony))
* black theme improvements
* new notification icon

**1.9**
* share links from different apps (it also extracts urls from a plain text)
* nice splash screen while starting the app
* links shared by long clicks are perfectly "clean" now
* Portuguese translation (thanks [antun3s](https://github.com/antun3s))
* black theme improvements (thanks [drbeat](https://github.com/drbeat))
* minor improvements

**1.8**
* new name and icon to avoid being recognized as the official Facebook app
* German translation (thanks [de-live-gdev](https://github.com/de-live-gdev))
* Serbian translation (thanks [pejakm](https://github.com/pejakm))
* Czech translation (thanks [panenka](https://github.com/panenka))
* Bengali translation (thanks [G33KS44n](https://github.com/G33KS44n))
* Android Marshmallow support
* new support libraries
* minor improvements

**1.7**
* NOTIFICATIONS SUPPORT (the app will notify you about the latest action on Facebook - not about messages) - visit app settings for more info. It's awesome and works almost perfectly!
* Choose menu position (menu on the left or on the right edge of the screen)
* Links shared by long clicks are now "clean" (removed Facebook redirections)
* UI improvements on Lollipop - Settings page has new, awesome switches now
* possibility to kill the app (not recommended but it removes app from background)
* Black theme improvements
* code security and a lot of optimizations

**1.6**
* Facebook Zero support (free mobile data transfer, GSM dependant)
* Quick Start Guide on app first run (learn the gestures)
* fixed Facebook navigation bar now works great for all devices
* code optimization - the app should be faster and take less memory
* possibility to donate - do you love the app? Buy me a beer! ;)
* shortcut to open messages directly (find it at launcher -> widgets/shortcuts)
* minor layout fixes

**1.5**
* new icon (more Facebook like)
* long click on links and image links to share | copy url
* fixed Facebook navigation bar (refresh after changing it)
* the app will remind you to activate Internet connection if offline
* extra bottom padding for transparent navbar - links are always clickable
* app colors match Facebook site
* possibility to disable hardware acceleration on low end devices (less memory)
* black theme (refresh a page after changing it) - experimental!
* my Google Play link at About & Dev - check out my other apps, one so far :)
* bugfixes (opening links)

**1.4**
* jump to top (no more painful scrolling!)
* activation and deactivation of transparent navbar is applied immediately
* horizontal mode with transparent navbar activated works properly now
* layout fix for KitKat (text in a toast)
* major changes on About & Dev
* the app supports Android 5.1.0
* auto-updates through F-Droid client

**1.3**
* swipe from the left edge for app menu
* progress bar while loading
* useful shortcuts (messages, friends on-line, groups...)
* transparent navigation bar (option to enable)
* clearing app cache
* Settings and About & Dev
* contact me and Notiface (for notifications) recommendation
* fb.me links can be opened in the app

**1.2**
* all Facebook links can now be opened in the app (in mobile view)
* for notification support just install Notiface: http://goo.gl/ZM7m98
* (when new notification is received just click it to open Facebook Lite)
* rare network error during app startup fixed

**1.1**
* photo upload (Camera or File Chooser)
* new permission (WRITE_EXTERNAL_STORAGE)
* (camera photos are saved in Pictures/FacebookLite)
* (photo upload may not work on KitKat)

**1.0**
* modern design (Material)
* chat support
* pull to refresh
* NO ADS at all
* always in your language
* no extra permissions (INTERNET only)

Refresh the page by pulling down with your finger. External links will open in your system browser as well as the full size images so you can zoom in, save, etc. The latest devices (KitKat and Lollipop) - blue status bar background. Minimum permissions needed. The application will always be free and without ads!

[pl] Temat na polskim forum dotyczący aplikacji:
* http://forum.android.com.pl/topic/211657-face-slim/

## Screenshots

* [Lollipop]( https://www.dropbox.com/s/4s0x88sp49hxm4n/en_Lollipop_2015-01-06.png)
* [Lollipop [pl]]( https://www.dropbox.com/s/jgcpnrwqk3gm7qz/pl_Lollipop_2015-01-06.png)
* [KitKat](https://www.dropbox.com/s/qd7qw8zucplob9o/kitkat_adv.png)
* [KitKat [pl]](https://www.dropbox.com/s/stnpvxgk6gb7t1x/Screenshot_20150106_211529.png)
* [KitKat [7-inch tablet]](https://www.dropbox.com/s/ztl1490p2mhbb5f/7_inch_tablet.png)
* [KitKat [10-inch tablet]]( https://www.dropbox.com/s/g9be7kj1bp54pr5/10_inch_tablet.png)

## Support future development!
Buy me a beer or two :)<br />
BTC:&nbsp;  1JUaZytkub2CP5jhRYQDDY6pibqrUUSp2y

<b>Google Play donation package:</b><br />
https://play.google.com/store/apps/details?id=org.indywidualni.faceslim_donation

[![Donate with PayPal](https://www.paypalobjects.com/en_US/GB/i/btn/btn_donateCC_LG.gif "Donate with PayPal")](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=koras%2eevil%40gmail%2ecom&lc=GB&item_name=Krzysztof%20Grabowski&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted)


<br />
___
Copyright notice: Facebook is a trademark of Facebook Inc. This app is NOT connected to Facebook Inc. whatsoever. This app only helps using the official Facebook Mobile website.

Important: The app has some limitations (just like the official Facebook mobile website).<br />
Install using F-Droid client to get automatic updates.

App suspended by Google Play.
Now open source and available to everyone.

Have fun! ;)
