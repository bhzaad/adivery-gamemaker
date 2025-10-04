package com.company.game;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import java.lang.reflect.Method;

public class AdiveryGM {

    private static final String TAG = "AdiveryGM";

    private static volatile boolean callbacksEnabled = false;
    private static String lastEvent = "";
    private static Object bannerView = null;
    private static Object globalListenerProxy = null;

    private static void setLastEvent(String type, String placementId, String message, Boolean rewarded) {
        StringBuilder sb = new StringBuilder();
        sb.append("type=").append(type);
        if (placementId != null) sb.append(";placement=").append(placementId);
        if (message != null) sb.append(";message=").append(message.replace(';', ':'));
        if (rewarded != null) sb.append(";rewarded=").append(rewarded ? "1" : "0");
        lastEvent = sb.toString();

        try {
            Class<?> runner = Class.forName("com.yoyogames.runner.RunnerJNILib");
            int map = -1;
            try {
                map = (int) runner.getMethod("dsMapCreate").invoke(null);
            } catch (NoSuchMethodException nsme1) {
                try {
                    map = (int) runner.getMethod("jCreateDsMap", Object.class, Object.class, Object.class, Object.class)
                            .invoke(null, null, null, null, null);
                } catch (NoSuchMethodException nsme2) {
                }
            }
            if (map >= 0) {
                try { runner.getMethod("dsMapAddString", int.class, String.class, String.class).invoke(null, map, "adivery_event", type); } catch (Throwable ignored) {}
                if (placementId != null) {
                    try { runner.getMethod("dsMapAddString", int.class, String.class, String.class).invoke(null, map, "placement", placementId); } catch (Throwable ignored) {}
                }
                if (message != null) {
                    try { runner.getMethod("dsMapAddString", int.class, String.class, String.class).invoke(null, map, "message", message); } catch (Throwable ignored) {}
                }
                if (rewarded != null) {
                    try { runner.getMethod("dsMapAddDouble", int.class, String.class, double.class).invoke(null, map, "rewarded", rewarded ? 1.0 : 0.0); } catch (Throwable ignored) {}
                }
                try {
                    runner.getMethod("CreateAsynEventWithDSMap", int.class, int.class).invoke(null, map, 70);
                } catch (NoSuchMethodException nsme3) {
                    try { runner.getMethod("CreateAsynEventWithDSMap", int.class).invoke(null, map); } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
        }
    }

    private static Activity getActivity() throws Exception {
        // Try common YoYo activity classes
        String[] candidates = new String[] {
                "com.yoyogames.runner.RunnerActivity",
                "com.yoyogames.runner.RunnerNativeActivity",
                "com.yoyogames.runtime.RunnerActivity"
        };
        for (String cn : candidates) {
            try {
                Class<?> cls = Class.forName(cn);
                try {
                    Object act = cls.getField("CurrentActivity").get(null);
                    if (act instanceof Activity) return (Activity) act;
                } catch (Throwable ignored) {}
            } catch (Throwable ignored) {}
        }
        // Fallback: scan ActivityThread for a resumed activity
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            java.lang.reflect.Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Object activities = activitiesField.get(activityThread);
            if (activities instanceof java.util.Map) {
                java.util.Map<?,?> map = (java.util.Map<?,?>) activities;
                for (Object record : map.values()) {
                    Class<?> recClass = record.getClass();
                    java.lang.reflect.Field pausedField;
                    try { pausedField = recClass.getDeclaredField("paused"); pausedField.setAccessible(true); }
                    catch (Throwable t) { pausedField = null; }
                    boolean paused = pausedField != null && Boolean.TRUE.equals(pausedField.get(record));
                    if (!paused) {
                        java.lang.reflect.Field activityField = recClass.getDeclaredField("activity");
                        activityField.setAccessible(true);
                        Object act = activityField.get(record);
                        if (act instanceof Activity) return (Activity) act;
                    }
                }
            }
        } catch (Throwable ignored) {}
        throw new IllegalStateException("Activity not available");
    }

    private static Context getContext() throws Exception {
        try { return getActivity().getApplicationContext(); } catch (Throwable ignored) {}
        Application app = getApplication();
        if (app != null) return app.getApplicationContext();
        throw new IllegalStateException("Context not available");
    }

    private static Application getApplication() throws Exception {
        try { return getActivity().getApplication(); } catch (Throwable ignored) {}
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object app = activityThreadClass.getMethod("currentApplication").invoke(null);
            return (Application) app;
        } catch (Throwable ignored) {}
        return null;
    }

    private static Class<?> getAdiveryClass() throws ClassNotFoundException {
        return Class.forName("com.adivery.sdk.Adivery");
    }

    private static Object invokeAdivery(String method, Class<?>[] types, Object... args) throws Exception {
        Class<?> c = getAdiveryClass();
        Method m = c.getMethod(method, types);
        return m.invoke(null, args);
    }

    private static synchronized void ensureGlobalListener(boolean enable) {
        try {
            Class<?> adiveryClass = getAdiveryClass();
            Class<?> listenerIface = Class.forName("com.adivery.sdk.AdiveryListener");

            if (enable) {
                if (globalListenerProxy == null) {
                    java.lang.reflect.InvocationHandler handler = new java.lang.reflect.InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            try {
                                String m = method.getName();
                                String placement = null;
                                String message = null;
                                Boolean rewarded = null;
                                if (args != null) {
                                    for (Object a : args) {
                                        if (a instanceof String) {
                                            // Prefer first String as placement id unless we already set it
                                            if (placement == null) placement = (String) a;
                                            else if (message == null) message = (String) a;
                                        } else if (a instanceof Boolean) {
                                            rewarded = (Boolean) a;
                                        } else if (a instanceof Throwable) {
                                            if (message == null) message = ((Throwable) a).getMessage();
                                        }
                                    }
                                }
                                Log.d(TAG, "listener: method=" + m
                                        + ", placement=" + placement
                                        + ", rewarded=" + rewarded
                                        + ", message=" + message);
                                setLastEvent(m, placement, message, rewarded);
                            } catch (Throwable ignored) {}
                            return null;
                        }
                    };
                    globalListenerProxy = java.lang.reflect.Proxy.newProxyInstance(
                            adiveryClass.getClassLoader(),
                            new Class[]{listenerIface},
                            handler
                    );
                    Method add = adiveryClass.getMethod("addGlobalListener", listenerIface);
                    add.invoke(null, globalListenerProxy);
                }
            } else {
                if (globalListenerProxy != null) {
                    try {
                        Method rm = adiveryClass.getMethod("removeGlobalListener", listenerIface);
                        rm.invoke(null, globalListenerProxy);
                    } catch (Throwable ignored) {}
                    globalListenerProxy = null;
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "ensureGlobalListener failed", t);
        }
    }

    public static double adivery_set_logging(double enabled) {
        try {
            invokeAdivery("setLoggingEnabled", new Class[]{boolean.class}, enabled != 0);
            return 1;
        } catch (Throwable t) {
            Log.e(TAG, "set_logging failed", t);
            return 0;
        }
    }

    public static double adivery_init(String appId) {
        try {
            invokeAdivery("configure", new Class[]{Application.class, String.class}, getApplication(), appId);
            if (callbacksEnabled) ensureGlobalListener(true);
            return 1;
        } catch (Throwable t) {
            Log.e(TAG, "init failed", t);
            return 0;
        }
    }

    public static double adivery_callbacks_enable(double enable) {
        callbacksEnabled = enable != 0;
        ensureGlobalListener(callbacksEnabled);
        return 1;
    }

    public static double adivery_prepare_interstitial(String placementId) {
        try {
            invokeAdivery("prepareInterstitialAd", new Class[]{Context.class, String.class}, getContext(), placementId);
            return 1;
        } catch (Throwable t) {
            Log.e(TAG, "prepare_interstitial failed", t);
            return 0;
        }
    }

    public static double adivery_prepare_rewarded(String placementId) {
        try {
            Log.d(TAG, "prepare_rewarded: start, placement=" + placementId);
            // Emit GM async event for debug
            try { setLastEvent("prepare_rewarded_start", placementId, null, null); } catch (Throwable ignored) {}
            Context ctx = getContext();
            Log.d(TAG, "prepare_rewarded: got context=" + (ctx == null ? "null" : ctx.getClass().getName()));
            try { setLastEvent("prepare_rewarded_got_context", placementId, (ctx == null ? "null" : ctx.getClass().getName()), null); } catch (Throwable ignored) {}
            // Invoke SDK
            invokeAdivery("prepareRewardedAd", new Class[]{Context.class, String.class}, ctx, placementId);
            Log.d(TAG, "prepare_rewarded: invoked SDK, awaiting callbacks");
            try { setLastEvent("prepare_rewarded_invoked", placementId, null, null); } catch (Throwable ignored) {}
            return 1;
        } catch (Throwable t) {
            Log.e(TAG, "prepare_rewarded: failed for placement=" + placementId, t);
            try { setLastEvent("prepare_rewarded_error", placementId, String.valueOf(t.getMessage()), null); } catch (Throwable ignored) {}
            return 0;
        }
    }

    public static double adivery_prepare_app_open(String placementId) {
        try {
            invokeAdivery("prepareAppOpenAd", new Class[]{Activity.class, String.class}, getActivity(), placementId);
            return 1;
        } catch (Throwable t) {
            Log.e(TAG, "prepare_app_open failed", t);
            return 0;
        }
    }

    public static double adivery_show(String placementId) {
        try {
            invokeAdivery("showAd", new Class[]{String.class}, placementId);
            setLastEvent("show_called", placementId, null, null);
            return 1;
        } catch (Throwable t) {
            Log.e(TAG, "show failed", t);
            return 0;
        }
    }

    public static double adivery_show_app_open(String placementId) {
        try {
            invokeAdivery("showAppOpenAd", new Class[]{Activity.class, String.class}, getActivity(), placementId);
            setLastEvent("appopen_show_called", placementId, null, null);
            return 1;
        } catch (Throwable t) {
            Log.e(TAG, "show_app_open failed", t);
            return 0;
        }
    }

    public static double adivery_is_loaded(String placementId) {
        try {
            Object r = invokeAdivery("isLoaded", new Class[]{String.class}, placementId);
            if (r instanceof Boolean) return ((Boolean) r) ? 1.0 : 0.0;
            return 0.0;
        } catch (Throwable t) {
            Log.e(TAG, "is_loaded failed", t);
            return 0.0;
        }
    }

    public static double adivery_set_user_id(String userId) {
        try {
            invokeAdivery("setUserId", new Class[]{String.class}, userId);
            return 1;
        } catch (Throwable t) {
            Log.e(TAG, "set_user_id failed", t);
            return 0;
        }
    }

    public static String adivery_get_vast_url(String placementId) {
        try {
            Object r = invokeAdivery("getVastUrl", new Class[]{String.class}, placementId);
            return r == null ? "" : String.valueOf(r);
        } catch (Throwable t) {
            Log.e(TAG, "get_vast_url failed", t);
            return "";
        }
    }

    public static double adivery_is_vast_ready() {
        try {
            Object r = invokeAdivery("isVastUrlReady", new Class[]{});
            if (r instanceof Boolean) return ((Boolean) r) ? 1.0 : 0.0;
            return 0.0;
        } catch (Throwable t) {
            Log.e(TAG, "is_vast_ready failed", t);
            return 0.0;
        }
    }

    public static String adivery_get_last_event() {
        String le = lastEvent;
        lastEvent = "";
        return le == null ? "" : le;
    }

    // GDPR dialog setup (opens the SDK GDPR dialog)
    public static double adivery_setup_gdpr() {
        try {
            Object r = invokeAdivery("setUpGDPRDialog", new Class[]{Activity.class}, getActivity());
            return 1;
        } catch (Throwable t) {
            Log.e(TAG, "setup_gdpr failed", t);
            return 0;
        }
    }

    // Banner helpers
    private static Object bannerSizeFromString(String size) throws Exception {
        String s = (size == null ? "" : size).trim().toUpperCase();
        if (s.equals("LARGE_BANNER")) {
            return Class.forName("com.adivery.sdk.BannerSize").getField("LARGE_BANNER").get(null);
        } else if (s.equals("MEDIUM_RECTANGLE") || s.equals("MREC")) {
            return Class.forName("com.adivery.sdk.BannerSize").getField("MEDIUM_RECTANGLE").get(null);
        } else if (s.equals("SMART_BANNER")) {
            return Class.forName("com.adivery.sdk.BannerSize").getField("SMART_BANNER").get(null);
        } else {
            return Class.forName("com.adivery.sdk.BannerSize").getField("BANNER").get(null);
        }
    }

    public static double adivery_banner_show(final String placementId, final String size, final double positionBottom) {
        try {
            final Activity activity = getActivity();
            activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    try {
                        // Remove existing banner if any
                        try {
                            if (bannerView != null) {
                                View view = (View) bannerView;
                                ViewParent vp = view.getParent();
                                if (vp instanceof ViewGroup) {
                                    ((ViewGroup) vp).removeView(view);
                                }
                            }
                        } catch (Throwable ignored) {}

                        Class<?> cls = Class.forName("com.adivery.sdk.AdiveryBannerAdView");
                        Object bv = cls.getConstructor(Context.class).newInstance(activity);
                        // setPlacementId
                        cls.getMethod("setPlacementId", String.class).invoke(bv, placementId);
                        // setBannerSize
                        Object bs = null;
                        try { bs = bannerSizeFromString(size); } catch (Throwable t) {}
                        if (bs != null) {
                            cls.getMethod("setBannerSize", Class.forName("com.adivery.sdk.BannerSize")).invoke(bv, bs);
                        }
                        // loadAd
                        cls.getMethod("loadAd").invoke(bv);

                        int gravity = Gravity.CENTER_HORIZONTAL | ((positionBottom != 0) ? Gravity.BOTTOM : Gravity.TOP);
                        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.WRAP_CONTENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT,
                                gravity
                        );
                        activity.addContentView((View) bv, lp);
                        bannerView = bv;
                        setLastEvent("banner_show_called", placementId, size, null);
                    } catch (Throwable t) {
                        Log.e(TAG, "banner_show failed", t);
                        setLastEvent("banner_error", placementId, String.valueOf(t.getMessage()), null);
                    }
                }
            });
            return 1;
        } catch (Throwable t) {
            Log.e(TAG, "banner_show dispatch failed", t);
            return 0;
        }
    }

    public static double adivery_banner_hide() {
        try {
            final Activity activity = getActivity();
            activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    try {
                        if (bannerView != null) {
                            View view = (View) bannerView;
                            ViewParent vp = view.getParent();
                            if (vp instanceof ViewGroup) {
                                ((ViewGroup) vp).removeView(view);
                            }
                            bannerView = null;
                        }
                        setLastEvent("banner_hidden", null, null, null);
                    } catch (Throwable ignored) {}
                }
            });
            return 1;
        } catch (Throwable t) {
            Log.e(TAG, "banner_hide failed", t);
            return 0;
        }
    }
}
