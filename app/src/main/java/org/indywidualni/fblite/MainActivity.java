package org.indywidualni.fblite;

import java.io.File;
import java.io.IOException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity {

    // variables for drawer layout
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private String[] itemList;

    SwipeRefreshLayout swipeRefreshLayout;
    private WebView webView;
    private ProgressBar progressBar;

    // variables for camera and choosing files methods
    private static final int FILECHOOSER_RESULTCODE = 1;
    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI = null;

    // the same for Android 5.0 methods only
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;

    // error handling
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get shared preferences
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // KitKat layout fix
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            // apply top padding to avoid layout being hidden by the status bar
            LinearLayout contentMain = (LinearLayout) findViewById(R.id.content_main);
            contentMain.setPadding(0, getStatusBarHeight(), 0, 0);
            // bug fix for resizing the view while opening soft keyboard
            AndroidBug5497Workaround.assistActivity(this);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // transparent navBar (above KitKat) when it's enabled
            if (preferences.getBoolean("transparent_nav", false)) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                // apply top padding to avoid layout being hidden by the status bar
                LinearLayout contentMain = (LinearLayout) findViewById(R.id.content_main);
                contentMain.setPadding(0, getStatusBarHeight(), 0, 0);
                // bug fix for resizing the view while opening soft keyboard
                AndroidBug5497Workaround.assistActivity(this);

                // bug fix (1.4.1) for launching the app in landscape mode
                if(getResources().getConfiguration().orientation == 0 && Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT)
                    contentMain.setPadding(0, getStatusBarHeight(), getNavigationBarHeight(getApplicationContext(), 0), 0);
                else if (getResources().getConfiguration().orientation == 0 && Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                    contentMain.setPadding(0, 0, 0, getStatusBarHeight());
                }
            }
        }

        // piece of code for drawer layout
        itemList = getResources().getStringArray(R.array.item_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, itemList));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // define url that will open in webView
        String webViewUrl = "http://m.facebook.com";

        // when someone clicks a Facebook link start the app with that link
        if(getIntent().getDataString() != null) {
            webViewUrl = getIntent().getDataString();
            // show information about loading an external link
            Context c = getApplicationContext();
            Toast toast = Toast.makeText(c, R.string.loading_link, Toast.LENGTH_LONG);
            toast.show();
        }

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeRefreshLayout.setOnRefreshListener(onRefreshListener);
        swipeRefreshLayout.setColorSchemeColors(Color.BLUE);

        // webView code without handling external links
        webView = (WebView) findViewById(R.id.webView1);
        webView.getSettings().setJavaScriptEnabled(true);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        //webView.getSettings().setUseWideViewPort(true);
        //webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setAllowFileAccess(true);

        // load url in webView
        webView.loadUrl(webViewUrl);
        webView.setWebViewClient(new MyAppWebViewClient());

        // implement WebChromeClient inner class
        // we will define openFileChooser for select file from camera
        webView.setWebChromeClient(new WebChromeClient() {

            // page loading progress, gone when fully loaded
            public void onProgressChanged(WebView view, int progress) {
                // display it only when it's enabled (default true)
                if (preferences.getBoolean("progress_bar", true)) {
                    if (progress < 100 && progressBar.getVisibility() == ProgressBar.GONE) {
                        progressBar.setVisibility(ProgressBar.VISIBLE);
                    }
                    progressBar.setProgress(progress);
                    if (progress == 100) {
                        progressBar.setVisibility(ProgressBar.GONE);
                    }
                } else {
                    // if progress bar is disabled hide it immediately
                    progressBar.setVisibility(ProgressBar.GONE);
                }
            }

            // for Lollipop, all in one
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    WebChromeClient.FileChooserParams fileChooserParams) {
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

                File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FacebookLite");

                if (!imageStorageDir.exists()) {
                    imageStorageDir.mkdirs();
                }

                // create an image file name
                imageStorageDir  = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
                return imageStorageDir;
            }

            // openFileChooser for Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                mUploadMessage = uploadMsg;

                try {
                    File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FacebookLite");

                    if (!imageStorageDir.exists()) {
                        imageStorageDir.mkdirs();
                    }

                    File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");

                    mCapturedImageURI = Uri.fromFile(file); // save to the private variable

                    final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
                    // captureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("image/*");

                    Intent chooserIntent = Intent.createChooser(i, getString(R.string.image_chooser));
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});

                    startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
                } catch (Exception e) {
                    Toast.makeText(getBaseContext(), "Camera Exception:" + e, Toast.LENGTH_LONG).show();
                }

            }

            // not needed but let's make it overloaded just in case
            // openFileChooser for Android < 3.0
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                openFileChooser(uploadMsg, "");
            }

            // openFileChooser for other Android versions
            /* may not work on KitKat due to lack of implementation of openFileChooser() or onShowFileChooser()
               https://code.google.com/p/android/issues/detail?id=62220
               however newer versions of KitKat fixed it on some devices */
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                openFileChooser(uploadMsg, acceptType);
            }

        });

    }

    // get status bar height (needed for transparent nav bar)
    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    // get navigation bar height
    private int getNavigationBarHeight(Context context, int orientation) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier(orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_height_landscape", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    // return here when file selected from camera or from SD Card
    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {

        // code for all versions except of Lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

            if(requestCode==FILECHOOSER_RESULTCODE) {
                if (null == this.mUploadMessage) {
                    return;
                }

                Uri result=null;

                try{
                    if (resultCode != RESULT_OK) {
                        result = null;
                    } else {
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
                if (data == null) {
                    // if there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;

        } // end of code for Lollipop only

    }

    SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {

        // refreshing pages
        @Override
        public void onRefresh() {
            // reloading page: two ways of doing it
            //webView.loadUrl( "javascript:window.location.reload( true )" );
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
            selectItem(position);
        }
    }

    // when a drawer item is clicked do instructions from below
    private void selectItem(int position) {
        switch(position) {
            case 0:
                webView.loadUrl("javascript:scroll(0,0)");
                break;
            case 1:
                webView.loadUrl("https://m.facebook.com");
                break;
            case 2:
                webView.loadUrl("https://m.facebook.com/messages");
                break;
            case 3:
                webView.loadUrl("https://m.facebook.com/buddylist.php");
                break;
            case 4:
                webView.loadUrl("https://m.facebook.com/groups/?category=membership");
                break;
            case 5:
                webView.loadUrl("https://m.facebook.com/events");
                break;
            case 6:
                Intent settings = new Intent(this, SettingsActivity.class);
                startActivity(settings);
                break;
            case 7:
                Intent about = new Intent(this, AboutActivity.class);
                startActivity(about);
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

        // get shared preferences
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // bug fix (1.4.1) for landscape mode
        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && preferences.getBoolean("transparent_nav", false)) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                LinearLayout contentMain = (LinearLayout) findViewById(R.id.content_main);
                contentMain.setPadding(0, getStatusBarHeight(), getNavigationBarHeight(getApplicationContext(), 0), 0);
            } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                LinearLayout contentMain = (LinearLayout) findViewById(R.id.content_main);
                contentMain.setPadding(0, 0, 0, getStatusBarHeight());
            }
        } else {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                LinearLayout contentMain = (LinearLayout) findViewById(R.id.content_main);
                contentMain.setPadding(0, getStatusBarHeight(), 0, 0);
            } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                LinearLayout contentMain = (LinearLayout) findViewById(R.id.content_main);
                contentMain.setPadding(0, getStatusBarHeight(), 0, 0);
            }
        }
    }

    // app is already running and gets a new intent
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // grab an intent and load instead of the current page
        String webViewUrl = getIntent().getDataString();
        webView.loadUrl(webViewUrl);

        // get shared preferences
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            // transparent navBar (above KitKat) when it's enabled
            if (preferences.getBoolean("transparent_nav", false)) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            // transparent navBar (above KitKat) when it's enabled
            if (preferences.getBoolean("transparent_nav", false)) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                // apply top padding to avoid layout being hidden by the status bar
                LinearLayout contentMain = (LinearLayout) findViewById(R.id.content_main);
                contentMain.setPadding(0, getStatusBarHeight(), 0, 0);
                // bug fix for resizing the view while opening soft keyboard
                AndroidBug5497Workaround.assistActivity(this);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                // reset padding, Lollipop needs zeros
                LinearLayout contentMain = (LinearLayout) findViewById(R.id.content_main);
                contentMain.setPadding(0, 0, 0, 0);
            }
        }
    }

    // handling back button
    @Override
    public void onBackPressed() {
        if(webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

}