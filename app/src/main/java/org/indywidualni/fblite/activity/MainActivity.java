package org.indywidualni.fblite.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import net.grandcentrix.tray.TrayAppPreferences;

import org.indywidualni.fblite.MyApplication;
import org.indywidualni.fblite.R;
import org.indywidualni.fblite.service.NotificationsService;
import org.indywidualni.fblite.util.AndroidBug5497Workaround;
import org.indywidualni.fblite.util.Connectivity;
import org.indywidualni.fblite.util.Dimension;
import org.indywidualni.fblite.util.DownloadManagerResolver;
import org.indywidualni.fblite.util.Miscellany;
import org.indywidualni.fblite.webview.MyWebViewClient;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    // variables for drawer layout
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    // main layout, pull to refresh, webview
    private LinearLayout contentMain;
    private SwipeRefreshLayout swipeRefreshLayout;
    private WebView webView;
    private ProgressBar progressBar;

    // fullscreen videos
    private MyWebChromeClient mWebChromeClient;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private FrameLayout customViewContainer;
    private View mCustomView;
    private int previousUiVisibility;

    // variables for camera and choosing files methods
    private static final int FILECHOOSER_RESULTCODE = 1;
    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI;

    // the same for Android 5.0 methods only
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;

    // log tag, preferences, runtime permissions
    private static final String TAG = MainActivity.class.getSimpleName();
    private SharedPreferences preferences;
    private TrayAppPreferences trayPreferences;
    private static final int REQUEST_STORAGE = 1;

    // create link handler (long clicked links)
    private final MyHandler linkHandler = new MyHandler(this);

    // save images
    private static final int ID_CONTEXT_MENU_SAVE_IMAGE = 2562617;
    private String mPendingImageUrlToSave;
    private static String appDirectoryName;

    // user agents
    private static String USER_AGENT_DEFAULT;
    private static final String USER_AGENT_BASIC = "Mozilla/5.0 (Linux; U; Android 2.3.3; en-gb; " +
            "Nexus S Build/GRI20) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";

    @Override
    @SuppressLint("setJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get shared preferences and TrayPreferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        trayPreferences = new TrayAppPreferences(getApplicationContext());
        appDirectoryName = getString(R.string.app_name).replace(" ", "");

        // set the main content view (for drawer position)
        if ("0".equals(preferences.getString("drawer_pos", "0")))
            setContentView(R.layout.activity_main);
        else
            setContentView(R.layout.activity_main_drawer_right);

        // the main layout, everything is inside
        contentMain = (LinearLayout) findViewById(R.id.content_main);

        // if the app is being launched for the first time
        if (preferences.getBoolean("first_run", true)) {
            // show quick start guide
            onCoachMark();
            // save the fact that the app has been started at least once
            preferences.edit().putBoolean("first_run", false).apply();
        }

        // start the service when it's activated but somehow it's not running
        // when it's already running nothing happens so it's ok
        if (preferences.getBoolean("notifications_activated", false) || preferences.getBoolean("message_notifications", false)) {
            final Intent intent = new Intent(MyApplication.getContextOfApplication(), NotificationsService.class);
            MyApplication.getContextOfApplication().startService(intent);
        }

        // KitKat layout fix
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            // apply top padding to avoid layout being hidden by the status bar
            contentMain.setPadding(0, Dimension.getStatusBarHeight(getApplicationContext()), 0, 0);
            // bug fix for resizing the view while opening soft keyboard
            AndroidBug5497Workaround.assistActivity(this);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // transparent navBar (above KitKat) when it's enabled
            if (preferences.getBoolean("transparent_nav", false)) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                // apply top padding to avoid layout being hidden by the status bar
                contentMain.setPadding(0, Dimension.getStatusBarHeight(getApplicationContext()), 0, 0);
                // bug fix for resizing the view while opening soft keyboard
                AndroidBug5497Workaround.assistActivity(this);

                // bug fix (1.4.1) for launching the app in landscape mode
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT)
                    contentMain.setPadding(0, Dimension.getStatusBarHeight(getApplicationContext()), Dimension.getNavigationBarHeight(getApplicationContext(), 0), 0);
                else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                    contentMain.setPadding(0, 0, 0, Dimension.getStatusBarHeight(getApplicationContext()));
                }
            }
        }

        // piece of code for drawer layout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.drawer_slider);
        // set up the drawer's list view with items and onClick listener
        String[] itemList = getResources().getStringArray(R.array.item_array);
        mDrawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, itemList));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // disable hardware acceleration
        if (!preferences.getBoolean("hardware_acceleration", true)) {
            View root = mDrawerLayout.getRootView();
            root.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            Log.v("Hardware Acceleration", "disabled for this view");
        }

        // define url that will open in webView
        String webViewUrl = "https://m.facebook.com";

        // use basic mode
        if (preferences.getBoolean("basic_mode", false))
            webViewUrl = "https://mbasic.facebook.com";

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setOnRefreshListener(onRefreshListener);
        swipeRefreshLayout.setColorSchemeColors(Color.BLUE);

        // fullscreen videos display here
        customViewContainer = (FrameLayout) findViewById(R.id.customViewContainer);

        // bind progress bar
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        // webView code without handling external links
        webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);

        // show full images
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        // get user agent
        USER_AGENT_DEFAULT = webView.getSettings().getUserAgentString();
        trayPreferences.put("webview_user_agent", USER_AGENT_DEFAULT);

        // set user agent for basic mode
        if (preferences.getBoolean("basic_mode", false))
            webView.getSettings().setUserAgentString(USER_AGENT_BASIC);

        // disable images to reduce data usage
        if (preferences.getBoolean("no_images", false))
            webView.getSettings().setLoadsImagesAutomatically(false);

        // code optimization
        boolean isConnectedMobile = Connectivity.isConnectedMobile(getApplicationContext());
        boolean isFacebookZero = preferences.getBoolean("facebook_zero", false);

        /** get a subject and text and check if this is a link trying to be shared */
        String sharedSubject = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
        String sharedUrl = getIntent().getStringExtra(Intent.EXTRA_TEXT);

        // if we have a valid URL that was shared by us, open the sharer
        if (sharedUrl != null) {
            if (!sharedUrl.equals("")) {
                // check if the URL being shared is a proper web URL
                if (!sharedUrl.startsWith("http://") || !sharedUrl.startsWith("https://")) {
                    // if it's not, let's see if it includes an URL in it (prefixed with a message)
                    int startUrlIndex = sharedUrl.indexOf("http:");
                    if (startUrlIndex > 0) {
                        // seems like it's prefixed with a message, let's trim the start and get the URL only
                        sharedUrl = sharedUrl.substring(startUrlIndex);
                    }
                }
                // final step, set the proper Sharer...
                webViewUrl = String.format("https://m.facebook.com/sharer.php?u=%s&t=%s", sharedUrl, sharedSubject);
                if (preferences.getBoolean("basic_mode", false))
                    webViewUrl = String.format("https://mbasic.facebook.com/sharer.php?u=%s&t=%s", sharedUrl, sharedSubject);
                // ... and parse it just in case
                webViewUrl = Uri.parse(webViewUrl).toString();
            }
        }

        /** when someone clicks a Facebook link start the app with this link */
        if ((getIntent() != null && getIntent().getDataString() != null) && (!isFacebookZero || !isConnectedMobile)) {
            webViewUrl = getIntent().getDataString();
            // show information about loading an external link
            Toast.makeText(getApplicationContext(), getString(R.string.loading_link), Toast.LENGTH_SHORT).show();
        } else if (isFacebookZero && isConnectedMobile) {
            // facebook zero if activated and connected to a mobile network
            webViewUrl = "https://0.facebook.com";
            Toast.makeText(getApplicationContext(), getString(R.string.facebook_zero_active), Toast.LENGTH_SHORT).show();
        }

        /** if opened by a notification or a shortcut */
        try {
            //noinspection ConstantConditions
            if (getIntent().getExtras().getString("start_url") != null) {
                String temp = getIntent().getExtras().getString("start_url");
                if (!isFacebookZero || !isConnectedMobile)
                    webViewUrl = temp;
                // cancel all notifications if 'All notifications' button was clicked
                if ("https://m.facebook.com/notifications".equals(temp))
                    NotificationsService.cancelAllNotifications();
            }
        } catch (Exception ignored) {}

        // notify when there is no internet connection
        if (!Connectivity.isConnected(getApplicationContext()))
            Toast.makeText(getApplicationContext(), getString(R.string.no_network), Toast.LENGTH_SHORT).show();

        mWebChromeClient = new MyWebChromeClient();
        webView.setWebViewClient(new MyWebViewClient());
        webView.setWebChromeClient(mWebChromeClient);

        // load url in webView
        webView.loadUrl(webViewUrl);

        // OnLongClickListener for detecting long clicks on links and images
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // activate long clicks on links and image links according to settings
                if (preferences.getBoolean("long_clicks", true)) {
                    WebView.HitTestResult result = webView.getHitTestResult();
                    if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE || result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                        Message msg = linkHandler.obtainMessage();
                        webView.requestFocusNodeHref(msg);
                        return true;
                    }
                }
                return false;
            }
        });

    }

    private class MyWebChromeClient extends WebChromeClient {

        // page loading progress, gone when fully loaded
        public void onProgressChanged(WebView view, int progress) {
            // display it only when it's enabled (default true)
            if (preferences.getBoolean("progress_bar", true)) {
                if (progress < 100 && progressBar.getVisibility() == ProgressBar.GONE)
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                // set progress, it changes
                progressBar.setProgress(progress);
                if (progress == 100)
                    progressBar.setVisibility(ProgressBar.GONE);
            } else {
                // if progress bar is disabled hide it immediately
                progressBar.setVisibility(ProgressBar.GONE);
            }
        }

        // for >= Lollipop, all in one
        public boolean onShowFileChooser(
                WebView webView, ValueCallback<Uri[]> filePathCallback,
                WebChromeClient.FileChooserParams fileChooserParams) {

            /** Request permission for external storage access.
             *  If granted it's awesome and go on,
             *  otherwise just stop here and leave the method.
             */
            requestStoragePermission();
            if (!hasStoragePermission())
                return false;

            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePathCallback;

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

                // create the file where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e(TAG, "Unable to create Image File", ex);
                }

                // continue only if the file was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }

            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("image/*");

            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.image_chooser));
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

            startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);

            return true;
        }

        // creating image files (Lollipop only)
        private File createImageFile() throws IOException {
            File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), appDirectoryName);

            if (!imageStorageDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                imageStorageDir.mkdirs();
            }

            // create an image file name
            imageStorageDir = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
            return imageStorageDir;
        }

        // openFileChooser for Android 3.0+
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
            mUploadMessage = uploadMsg;

            try {
                File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), appDirectoryName);

                if (!imageStorageDir.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    imageStorageDir.mkdirs();
                }

                File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");

                mCapturedImageURI = Uri.fromFile(file); // save to the private variable

                final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
                //captureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");

                Intent chooserIntent = Intent.createChooser(i, getString(R.string.image_chooser));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});

                startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), getString(R.string.camera_exception), Toast.LENGTH_LONG).show();
            }

        }

        // not needed but let's make it overloaded just in case
        // openFileChooser for Android < 3.0
        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            openFileChooser(uploadMsg, "");
        }

        // openFileChooser for other Android versions
        /** may not work on KitKat due to lack of implementation of openFileChooser() or onShowFileChooser()
         *  https://code.google.com/p/android/issues/detail?id=62220
         *  however newer versions of KitKat fixed it on some devices */
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            openFileChooser(uploadMsg, acceptType);
        }

        /** This method was deprecated in API level 18.
         *  This method supports the obsolete plugin mechanism,
         *  and will not be invoked in future
         */
        @SuppressWarnings("deprecation")
        @Override
        public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
            onShowCustomView(view, callback);
        }

        @Override
        public void onShowCustomView(View view,CustomViewCallback callback) {
            // if a view already exists then immediately terminate the new one
            if (mCustomView != null) {
                callback.onCustomViewHidden();
                return;
            }
            mCustomView = view;

            // hide webView and swipeRefreshLayout
            webView.setVisibility(View.GONE);
            swipeRefreshLayout.setVisibility(View.GONE);

            // show customViewContainer
            customViewContainer.setVisibility(View.VISIBLE);
            customViewContainer.addView(view);
            customViewCallback = callback;

            // activate immersive mode
            if (Build.VERSION.SDK_INT >= 19)
                hideSystemUI();
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();
            if (mCustomView == null)
                return;

            // hide and remove customViewContainer
            mCustomView.setVisibility(View.GONE);
            customViewContainer.setVisibility(View.GONE);
            customViewContainer.removeView(mCustomView);
            customViewCallback.onCustomViewHidden();

            // show swipeRefreshLayout and webView
            swipeRefreshLayout.setVisibility(View.VISIBLE);
            webView.setVisibility(View.VISIBLE);

            mCustomView = null;

            // deactivate immersive mode
            if (Build.VERSION.SDK_INT >= 19)
                showSystemUI();
        }

    }

    // handle long clicks on links, an awesome way to avoid memory leaks
    private static class MyHandler extends Handler {

        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {

                // get url to share
                String url = (String) msg.getData().get("url");

                if (url != null) {
                    /* "clean" an url to remove Facebook tracking redirection while sharing
                    and recreate all the special characters */
                    url = Miscellany.cleanAndDecodeUrl(url);

                    Log.v("Link long clicked", url);
                    // create share intent for long clicked url
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, url);
                    activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share_link)));
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void hideSystemUI() {
        previousUiVisibility = contentMain.getSystemUiVisibility();
        contentMain.setPadding(0, 0, 0, 0);

        contentMain.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void showSystemUI() {
        contentMain.setSystemUiVisibility(previousUiVisibility);
        // fake a configuration change to set the right padding
        onConfigurationChanged(getResources().getConfiguration());
    }

    // request storage permission
    private void requestStoragePermission() {
        String[] permissions = new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE };
        if (!hasStoragePermission()) {
            Log.e(TAG, "No storage permission at the moment. Requesting...");
            ActivityCompat.requestPermissions(this, permissions, REQUEST_STORAGE);
        } else {
            Log.e(TAG, "We already have storage permission. Yay!");
            // new image is about to be saved
            if (mPendingImageUrlToSave != null)
                saveImageToDisk(mPendingImageUrlToSave);
        }
    }

    // check is storage permission granted
    private boolean hasStoragePermission() {
        String storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        int hasPermission = ContextCompat.checkSelfPermission(this, storagePermission);
        return (hasPermission == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Storage permission granted");
                    // new image is about to be saved
                    if (mPendingImageUrlToSave != null)
                        saveImageToDisk(mPendingImageUrlToSave);
                } else {
                    Log.e(TAG, "Storage permission denied");
                    Toast.makeText(getApplicationContext(), getString(R.string.no_storage_permission), Toast.LENGTH_SHORT).show();
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // return here when file selected from camera or from SD Card
    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        // code for all versions except of Lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

            if (requestCode == FILECHOOSER_RESULTCODE) {
                if (null == this.mUploadMessage)
                    return;

                Uri result = null;

                try {
                    if (resultCode != RESULT_OK)
                        result = null;
                    else {
                        // retrieve from the private variable if the intent is null
                        result = data == null ? mCapturedImageURI : data.getData();
                    }
                }
                catch(Exception e) {
                    Toast.makeText(getApplicationContext(), "activity :"+e, Toast.LENGTH_LONG).show();
                }

                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }

        } // end of code for all versions except of Lollipop

        // start of code for Lollipop only
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            if (requestCode != FILECHOOSER_RESULTCODE || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }

            Uri[] results = null;

            // check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (data == null || data.getData() == null) {
                    // if there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = new Uri[] {Uri.parse(mCameraPhotoPath)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[] {Uri.parse(dataString)};
                    }
                }
            }

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;

        } // end of code for Lollipop only
    }

    private SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {

        // refreshing pages
        @Override
        public void onRefresh() {
            // notify when there is no internet connection
            if (!Connectivity.isConnected(getApplicationContext()))
                Toast.makeText(getApplicationContext(), getString(R.string.no_network), Toast.LENGTH_SHORT).show();

            // reloading page
            webView.reload();

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    swipeRefreshLayout.setRefreshing(false);
                    // done!
                }

            }, 2000);
        }};

    // the click listener for ListView in the navigation drawer
    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String baseAddress = "https://m.facebook.com/";

            if (preferences.getBoolean("basic_mode", false))
                baseAddress = "https://mbasic.facebook.com/";
            if (preferences.getBoolean("facebook_zero", false) && Connectivity.isConnectedMobile(getApplicationContext()))
                baseAddress = "https://0.facebook.com/";

            selectItem(position, baseAddress);
        }
    }

    // when a drawer item is clicked do instructions from below
    private void selectItem(int position, String baseAddress) {
        switch (position) {
            case 0:
                webView.loadUrl(baseAddress);
                break;
            case 1:
                webView.loadUrl(baseAddress + "messages");
                break;
            case 2:
                webView.loadUrl(baseAddress + "buddylist.php");
                break;
            case 3:
                webView.loadUrl(baseAddress + "groups/?category=membership");
                break;
            case 4:
                webView.loadUrl(baseAddress + "events");
                break;
            case 5:
                Intent settings = new Intent(this, SettingsActivity.class);
                startActivity(settings);
                break;
            case 6:
                Intent about = new Intent(this, AboutActivity.class);
                startActivity(about);
                break;
            case 7:
                trayPreferences.put("activity_visible", false);
                //finish();
                System.exit(0); // ugly, ugly, ugly! They wanted it :(
                break;
            case 8:
                webView.loadUrl("javascript:scroll(0,0)");
                break;
            default:
                // silence is golden
                break;
        }
        // update selected item, then close the drawer
        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    // survive screen orientation change
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // don't change padding during fullscreen video playback
        if (mCustomView == null) {
            // bug fix (1.4.1) for landscape mode
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && preferences.getBoolean("transparent_nav", false)) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                    contentMain.setPadding(0, Dimension.getStatusBarHeight(getApplicationContext()), Dimension.getNavigationBarHeight(getApplicationContext(), 0), 0);
                } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                    contentMain.setPadding(0, 0, 0, Dimension.getStatusBarHeight(getApplicationContext()));
                }
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && preferences.getBoolean("transparent_nav", false)) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                    contentMain.setPadding(0, Dimension.getStatusBarHeight(getApplicationContext()), 0, 0);
                } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                    contentMain.setPadding(0, Dimension.getStatusBarHeight(getApplicationContext()), 0, 0);
                }
            }
        }
    }

    // app is already running and gets a new intent
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // grab an url if opened by clicking a link
        String webViewUrl = getIntent().getDataString();

        // code optimization
        boolean isConnectedMobile = Connectivity.isConnectedMobile(getApplicationContext());
        boolean isFacebookZero = preferences.getBoolean("facebook_zero", false);

        // set the right user agent
        if (preferences.getBoolean("basic_mode", false))
            webView.getSettings().setUserAgentString(USER_AGENT_BASIC);
        else
            webView.getSettings().setUserAgentString(USER_AGENT_DEFAULT);

        /** get a subject and text and check if this is a link trying to be shared */
        String sharedSubject = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
        String sharedUrl = getIntent().getStringExtra(Intent.EXTRA_TEXT);

        // if we have a valid URL that was shared by us, open the sharer
        if (sharedUrl != null) {
            if (!sharedUrl.equals("")) {
                // check if the URL being shared is a proper web URL
                if (!sharedUrl.startsWith("http://") || !sharedUrl.startsWith("https://")) {
                    // if it's not, let's see if it includes an URL in it (prefixed with a message)
                    int startUrlIndex = sharedUrl.indexOf("http:");
                    if (startUrlIndex > 0) {
                        // seems like it's prefixed with a message, let's trim the start and get the URL only
                        sharedUrl = sharedUrl.substring(startUrlIndex);
                    }
                }
                // final step, set the proper Sharer...
                webViewUrl = String.format("https://m.facebook.com/sharer.php?u=%s&t=%s", sharedUrl, sharedSubject);
                if (preferences.getBoolean("basic_mode", false))
                    webViewUrl = String.format("https://mbasic.facebook.com/sharer.php?u=%s&t=%s", sharedUrl, sharedSubject);
                // ... and parse it just in case
                webViewUrl = Uri.parse(webViewUrl).toString();
            }
        }

        /** if opened by a notification or a shortcut */
        try {
            if (getIntent().getExtras().getString("start_url") != null)
                webViewUrl = getIntent().getExtras().getString("start_url");
                // cancel all notifications if 'All notifications' button was clicked
                if ("https://m.facebook.com/notifications".equals(webViewUrl))
                    NotificationsService.cancelAllNotifications();
        } catch (Exception ignored) {}

        /** load a grabbed url instead of the current page */
        if (isFacebookZero && isConnectedMobile)
            Toast.makeText(getApplicationContext(), getString(R.string.facebook_zero_active), Toast.LENGTH_SHORT).show();
        else
            webView.loadUrl(webViewUrl);

        // notify when there is no internet connection
        if (!Connectivity.isConnected(getApplicationContext()))
            Toast.makeText(getApplicationContext(), getString(R.string.no_network), Toast.LENGTH_SHORT).show();

        // recreate activity when something important was just changed
        if (getIntent().getBooleanExtra("core_settings_changed", false)) {
            finish(); // finish and create a new Instance
            Intent restart = new Intent(MainActivity.this, MainActivity.class);
            startActivity(restart);
        }
    }

    // handling back button
    @Override
    public void onBackPressed() {
        if (inCustomView())
            hideCustomView();
        else if (mCustomView == null && webView.canGoBack())
            webView.goBack();
        else
            super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        webView.resumeTimers();
        registerForContextMenu(webView);
        trayPreferences.put("activity_visible", true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            unregisterForContextMenu(webView);
            webView.onPause();
            webView.pauseTimers();
        }
        trayPreferences.put("activity_visible", false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (inCustomView()) {
            hideCustomView();
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: Destroying...");
        super.onDestroy();
        if (webView != null) {
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        // just in case, it should be GCed anyway
        if (mWebChromeClient != null)
            mWebChromeClient = null;
    }

    // is a video played in fullscreen mode
    private boolean inCustomView() {
        return (mCustomView != null);
    }

    // deactivate fullscreen for video playback
    private void hideCustomView() {
        mWebChromeClient.onHideCustomView();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        WebView.HitTestResult result = webView.getHitTestResult();
        if (result != null) {
            int type = result.getType();

            if (type == WebView.HitTestResult.IMAGE_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                showLongPressedImageMenu(menu, result.getExtra());
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case ID_CONTEXT_MENU_SAVE_IMAGE:
                /** In order to save anything we need storage permission.
                 *  onRequestPermissionsResult will save an image.
                 */
                requestStoragePermission();
                break;
        }
        return super.onContextItemSelected(item);
    }


    private void showLongPressedImageMenu(ContextMenu menu, String imageUrl) {
        mPendingImageUrlToSave = imageUrl;
        menu.add(0, ID_CONTEXT_MENU_SAVE_IMAGE, 0, getString(R.string.save_img));
    }

    private void saveImageToDisk(String imageUrl) {
        if (!DownloadManagerResolver.resolve(this)) {
            mPendingImageUrlToSave = null;
            return;
        }

        try {
            File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), appDirectoryName);

            if (!imageStorageDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                imageStorageDir.mkdirs();
            }

            // default image extension
            String imgExtension = ".jpg";

            if (imageUrl.contains(".gif"))
                imgExtension = ".gif";
            else if (imageUrl.contains(".png"))
                imgExtension = ".png";

            String date = DateFormat.getDateTimeInstance().format(new Date());
            String file = "IMG_" + date.replace(" ", "-") + imgExtension;

            DownloadManager dm = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri downloadUri = Uri.parse(imageUrl);
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);

            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES + File.separator + appDirectoryName, file)
                    .setTitle(file).setDescription(getString(R.string.save_img))
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            dm.enqueue(request);

            Toast.makeText(this, getString(R.string.downloading_img), Toast.LENGTH_LONG).show();
        } catch (IllegalStateException ex) {
            Toast.makeText(this, getString(R.string.cannot_access_storage), Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            // just in case, it should never be called anyway
            Toast.makeText(this, getString(R.string.file_cannot_be_saved), Toast.LENGTH_LONG).show();
        } finally {
            mPendingImageUrlToSave = null;
        }
    }

    // first run dialog with introduction
    private void onCoachMark() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.coach_mark);
        dialog.setCanceledOnTouchOutside(true);
        //for dismissing anywhere you touch
        View masterView = dialog.findViewById(R.id.coach_mark_master_view);
        masterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                // notifications setup notice
                Toast.makeText(getApplicationContext(), getString(R.string.setup_notifications), Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

}
