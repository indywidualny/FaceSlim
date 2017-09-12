/*
 * Copyright (C) 2013 Daniel Velazco
 * Copyright (C) 2016 Jeff Gehlbach
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

package org.indywidualni.fblite.util;

import android.content.Context;
import android.content.Intent;
import android.net.Proxy;
import android.os.Build;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Log;
import android.webkit.WebView;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Taken from
 * https://github.com/velazcod/Tinfoil-Facebook/blob/8850f06/app/src/main/java/com/danvelazco/fbwrapper/util/WebViewProxyUtil.java
 */
public class WebViewProxyUtil {

    // Constants
    private static final String LOG_TAG = "WebViewProxyUtil";

    /**
     * Helper method to set the proxy to a {@link android.webkit.WebView}
     *
     * @param context {@link Context}
     * @param webview {@link android.webkit.WebView}
     * @param host {@link String}
     * @param port {@link int}
     *
     * @return {@link boolean}
     */
    public static boolean setProxy(Context context, WebView webview, String host, int port) {
        // ICS: 4.0.3
        if (Build.VERSION.SDK_INT <= 15) {
            return setProxyICS(webview, host, port);
        }
        // 4.1 or higher (JB)
        else if (Build.VERSION.SDK_INT <= 18) {
            return setProxyJBPlus(webview, host, port);
        }
        // 4.4 or higher (KK)
        else if (Build.VERSION.SDK_INT <= 20) {
            return setKitKatWebViewProxy(context, host, port);
        }
        // 5.0 or higher (LP)
        else {
            return setLollipopWebViewProxy(context, host, port);
        }
    }

    /**
     * Set Proxy for Android 4.0.3 and above.
     */
    @SuppressWarnings("all")
    private static boolean setProxyICS(WebView webview, String host, int port) {
        try {
            Log.d(LOG_TAG, "Setting proxy with 4.0 API.");

            Class jwcjb = Class.forName("android.webkit.JWebCoreJavaBridge");
            Class params[] = new Class[1];
            params[0] = Class.forName("android.net.ProxyProperties");
            Method updateProxyInstance = jwcjb.getDeclaredMethod("updateProxy", params);

            Class wv = Class.forName("android.webkit.WebView");
            Field mWebViewCoreField = wv.getDeclaredField("mWebViewCore");
            Object mWebViewCoreFieldInstance = getFieldValueSafely(mWebViewCoreField, webview);

            Class wvc = Class.forName("android.webkit.WebViewCore");
            Field mBrowserFrameField = wvc.getDeclaredField("mBrowserFrame");
            Object mBrowserFrame = getFieldValueSafely(mBrowserFrameField, mWebViewCoreFieldInstance);

            Class bf = Class.forName("android.webkit.BrowserFrame");
            Field sJavaBridgeField = bf.getDeclaredField("sJavaBridge");
            Object sJavaBridge = getFieldValueSafely(sJavaBridgeField, mBrowserFrame);

            Class ppclass = Class.forName("android.net.ProxyProperties");
            Class pparams[] = new Class[3];
            pparams[0] = String.class;
            pparams[1] = int.class;
            pparams[2] = String.class;
            Constructor ppcont = ppclass.getConstructor(pparams);

            updateProxyInstance.invoke(sJavaBridge, ppcont.newInstance(host, port, null));

            Log.d(LOG_TAG, "Setting proxy with 4.0 API successful!");
            return true;
        } catch (Exception ex) {
            Log.e(LOG_TAG, "failed to set HTTP proxy: ", ex);
            return false;
        }
    }

    /**
     * Set Proxy for Android 4.1 and above.
     */
    @SuppressWarnings("all")
    private static boolean setProxyJBPlus(WebView webview, String host, int port) {
        Log.d(LOG_TAG, "Setting proxy with >= 4.1 API.");

        try {
            Class wvcClass = Class.forName("android.webkit.WebViewClassic");
            Class wvParams[] = new Class[1];
            wvParams[0] = Class.forName("android.webkit.WebView");
            Method fromWebView = wvcClass.getDeclaredMethod("fromWebView", wvParams);
            Object webViewClassic = fromWebView.invoke(null, webview);

            Class wv = Class.forName("android.webkit.WebViewClassic");
            Field mWebViewCoreField = wv.getDeclaredField("mWebViewCore");
            Object mWebViewCoreFieldInstance = getFieldValueSafely(mWebViewCoreField, webViewClassic);

            Class wvc = Class.forName("android.webkit.WebViewCore");
            Field mBrowserFrameField = wvc.getDeclaredField("mBrowserFrame");
            Object mBrowserFrame = getFieldValueSafely(mBrowserFrameField, mWebViewCoreFieldInstance);

            Class bf = Class.forName("android.webkit.BrowserFrame");
            Field sJavaBridgeField = bf.getDeclaredField("sJavaBridge");
            Object sJavaBridge = getFieldValueSafely(sJavaBridgeField, mBrowserFrame);

            Class ppclass = Class.forName("android.net.ProxyProperties");
            Class pparams[] = new Class[3];
            pparams[0] = String.class;
            pparams[1] = int.class;
            pparams[2] = String.class;
            Constructor ppcont = ppclass.getConstructor(pparams);

            Class jwcjb = Class.forName("android.webkit.JWebCoreJavaBridge");
            Class params[] = new Class[1];
            params[0] = Class.forName("android.net.ProxyProperties");
            Method updateProxyInstance = jwcjb.getDeclaredMethod("updateProxy", params);

            updateProxyInstance.invoke(sJavaBridge, ppcont.newInstance(host, port, null));
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Setting proxy with >= 4.1 API failed with error: ", ex);
            return false;
        }

        Log.d(LOG_TAG, "Setting proxy with >= 4.1 API successful!");
        return true;
    }

    /**
     * Set Proxy for Android 4.4 and above.
     */
    @SuppressWarnings("all")
    private static boolean setKitKatWebViewProxy(Context appContext, String host, int port) {
        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxyPort", port + "");
        System.setProperty("https.proxyHost", host);
        System.setProperty("https.proxyPort", port + "");
        try {
            Class applictionCls = Class.forName("android.app.Application");
            Field loadedApkField = applictionCls.getDeclaredField("mLoadedApk");
            loadedApkField.setAccessible(true);
            Object loadedApk = loadedApkField.get(appContext);
            Class loadedApkCls = Class.forName("android.app.LoadedApk");
            Field receiversField = loadedApkCls.getDeclaredField("mReceivers");
            receiversField.setAccessible(true);
            ArrayMap receivers = (ArrayMap) receiversField.get(loadedApk);
            for (Object receiverMap : receivers.values()) {
                for (Object rec : ((ArrayMap) receiverMap).keySet()) {
                    Class clazz = rec.getClass();
                    if (clazz.getName().contains("ProxyChangeListener")) {
                        Method onReceiveMethod = clazz.getDeclaredMethod("onReceive", Context.class, Intent.class);
                        Intent intent = new Intent(Proxy.PROXY_CHANGE_ACTION);

                        /*********** optional, may be need in future *************/
                        final String CLASS_NAME = "android.net.ProxyProperties";
                        Class cls = Class.forName(CLASS_NAME);
                        Constructor constructor = cls.getConstructor(String.class, Integer.TYPE, String.class);
                        constructor.setAccessible(true);
                        Object proxyProperties = constructor.newInstance(host, port, null);
                        intent.putExtra("proxy", (Parcelable) proxyProperties);
                        /*********** optional, may be need in future *************/

                        onReceiveMethod.invoke(rec, appContext, intent);
                    }
                }
            }
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException |
                IllegalArgumentException | NoSuchMethodException | InvocationTargetException |
                InstantiationException e) {
            Log.e(LOG_TAG, "Setting proxy with >= 4.4 API failed with error: ", e);
            return false;
        }

        Log.d(LOG_TAG, "Setting proxy with >= 4.4 API successful!");
        return true;
    }
    /**
     * Set Proxy for Android 5.0 and above.
     */
    @SuppressWarnings("all")
    private static boolean setLollipopWebViewProxy(Context appContext, String host, int port) {
        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxyPort", port + "");
        System.setProperty("https.proxyHost", host);
        System.setProperty("https.proxyPort", port + "");
        try {
            Class applictionCls = Class.forName("android.app.Application");
            Field loadedApkField = applictionCls.getDeclaredField("mLoadedApk");
            loadedApkField.setAccessible(true);
            Object loadedApk = loadedApkField.get(appContext);
            Class loadedApkCls = Class.forName("android.app.LoadedApk");
            Field receiversField = loadedApkCls.getDeclaredField("mReceivers");
            receiversField.setAccessible(true);
            ArrayMap receivers = (ArrayMap) receiversField.get(loadedApk);
            for (Object receiverMap : receivers.values()) {
                for (Object rec : ((ArrayMap) receiverMap).keySet()) {
                    Class clazz = rec.getClass();
                    if (clazz.getName().contains("ProxyChangeListener")) {
                        Method onReceiveMethod = clazz.getDeclaredMethod("onReceive", Context.class, Intent.class);
                        Intent intent = new Intent(Proxy.PROXY_CHANGE_ACTION);
                        /***** In Lollipop, ProxyProperties went public as ProxyInfo *****/
                        final String CLASS_NAME = "android.net.ProxyInfo";
                        Class cls = Class.forName(CLASS_NAME);
                        /***** ProxyInfo lacks constructors, use the static buildDirectProxy method instead *****/
                        Method buildDirectProxyMethod = cls.getMethod("buildDirectProxy", String.class, Integer.TYPE);
                        Object proxyInfo = buildDirectProxyMethod.invoke(cls, host, port);
                        intent.putExtra("proxy", (Parcelable) proxyInfo);
                        onReceiveMethod.invoke(rec, appContext, intent);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Setting proxy with >= 5.0 API failed with", e);
            return false;
        }
        Log.d(LOG_TAG, "Setting proxy with >= 5.0 API successful!");
        return true;
    }

    private static Object getFieldValueSafely(Field field, Object classInstance) throws IllegalArgumentException,
            IllegalAccessException {
        boolean oldAccessibleValue = field.isAccessible();
        field.setAccessible(true);
        Object result = field.get(classInstance);
        field.setAccessible(oldAccessibleValue);
        return result;
    }

}