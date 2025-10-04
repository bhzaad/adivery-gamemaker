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
    private static volatile boolean awaitingResume = false;
    private static String currentAdPlacement = null;
    private static boolean lifecycleRegistered = false;

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

    private static void uiShowAd(final Activity act, final String placementId) {
        try {
            if (act != null) {
                act.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        try {
                            try {
                                invokeAdivery("showAd", new Class[]{Activity.class, String.class}, act, placementId);
                            } catch (NoSuchMethodException nsme) {
                                invokeAdivery("showAd", new Class[]{String.class}, placementId);
                            }
                            setLastEvent("show_called", placementId, null, null);
                        } catch (Throwable t) {
                            Log.e(TAG, "show(UI) failed", t);
                            setLastEvent("show_error", placementId, String.valueOf(t.getMessage()), null);
                        }
                    }
                });
            } else {
                try {
                    invokeAdivery("showAd", new Class[]{String.class}, placementId);
                    setLastEvent("show_called", placementId, null, null);
                } catch (Throwable t) {
                    Log.e(TAG, "show(no-UI) failed", t);
                    setLastEvent("show_error", placementId, String.valueOf(t.getMessage()), null);
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "uiShowAd dispatch failed", t);
            setLastEvent("show_error", placementId, String.valueOf(t.getMessage()), null);
        }
    }

    private static Object makeListenerProxy(final Class<?> listenerIface) throws Exception {
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
                                if (placement == null) placement = (String) a; else if (message == null) message = (String) a;
                            } else if (a instanceof Boolean) {
                                rewarded = (Boolean) a;
                            } else if (a instanceof Throwable) {
                                if (message == null) message = ((Throwable) a).getMessage();
                            }
                        }
                    }
                    Log.d(TAG, "listener(" + listenerIface.getSimpleName() + "): method=" + m + ", placement=" + placement + ", rewarded=" + rewarded + ", message=" + message);
                    setLastEvent(m, placement, message, rewarded);
                } catch (Throwable ignored) {}
                return null;
            }
        };
        return java.lang.reflect.Proxy.newProxyInstance(getAdiveryClass().getClassLoader(), new Class[]{listenerIface}, handler);
    }

    private static synchronized void ensureGlobalListener(boolean enable) {
        try {
            Class<?> adiveryClass = getAdiveryClass();
            Class<?>[] listenerIfaces = new Class<?>[] {
                    Class.forName("com.adivery.sdk.AdiveryListener"),
                    // Some SDK versions use AdiveryAdListener
                    tryClass("com.adivery.sdk.AdiveryAdListener")
            };

            if (enable) {
                if (globalListenerProxy == null) {
                    // Try to attach any known global-listener method for either iface
                    for (Class<?> li : listenerIfaces) {
                        if (li == null) continue;
                        try {
                            Object proxy = makeListenerProxy(li);
                            // Preferred: addGlobalListener(iface)
                            try {
                                Method add = adiveryClass.getMethod("addGlobalListener", li);
                                add.invoke(null, proxy);
                                globalListenerProxy = proxy;
                                break;
                            } catch (Throwable ignored) {}
                            // Fallback: any static method with single param li and name containing listener/callback
                            for (Method m : adiveryClass.getMethods()) {
                                if ((m.getParameterTypes().length == 1) && m.getParameterTypes()[0] == li) {
                                    String n = m.getName().toLowerCase();
                                    if (n.contains("listener") || n.contains("callback")) {
                                        try { m.invoke(null, proxy); globalListenerProxy = proxy; break; } catch (Throwable ignored2) {}
                                    }
                                }
                            }
                            if (globalListenerProxy != null) break;
                        } catch (Throwable ignored) {}
                    }
                }
            } else {
                if (globalListenerProxy != null) {
                    try { Method rm = adiveryClass.getMethod("removeGlobalListener", Class.forName("com.adivery.sdk.AdiveryListener")); rm.invoke(null, globalListenerProxy); } catch (Throwable ignored) {}
                    globalListenerProxy = null;
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "ensureGlobalListener failed", t);
        }
    }

    private static Class<?> tryClass(String name) {
        try { return Class.forName(name); } catch (Throwable ignored) { return null; }
    }

    private static void attachPerPlacementListener(String placementId) {
        try {
            Class<?> adiveryClass = getAdiveryClass();
            Class<?>[] listenerIfaces = new Class<?>[] {
                    tryClass("com.adivery.sdk.AdiveryListener"),
                    tryClass("com.adivery.sdk.AdiveryAdListener")
            };
            boolean attached = false;
            for (Class<?> li : listenerIfaces) {
                if (li == null) continue;
                try {
                    Object proxy = makeListenerProxy(li);
                    for (Method m : adiveryClass.getMethods()) {
                        Class<?>[] pts = m.getParameterTypes();
                        if (pts.length == 2) {
                            boolean hasString = (pts[0] == String.class) || (pts[1] == String.class);
                            boolean hasListener = (pts[0] == li) || (pts[1] == li);
                            if (hasString && hasListener) {
                                String n = m.getName().toLowerCase();
                                if (n.contains("listener") || n.contains("callback")) {
                                    try {
                                        if (pts[0] == String.class && pts[1] == li) m.invoke(null, placementId, proxy);
                                        else if (pts[0] == li && pts[1] == String.class) m.invoke(null, proxy, placementId);
                                        else continue;
                                        attached = true;
                                        setLastEvent("listener_attached", placementId, m.getName(), null);
                                        break;
                                    } catch (Throwable ignored) {}
                                }
                            }
                        }
                    }
                    if (attached) break;
                } catch (Throwable ignored) {}
            }
            if (!attached) Log.d(TAG, "No per-placement listener method found");
        } catch (Throwable t) {
            Log.e(TAG, "attachPerPlacementListener failed", t);
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
            registerLifecycleCallbacks();
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
            attachPerPlacementListener(placementId);
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
            // Route show to UI thread and try both signatures
            Activity act = null;
            try { act = getActivity(); } catch (Throwable ignored) {}
            currentAdPlacement = placementId;
            awaitingResume = true;
            uiShowAd(act, placementId);
            return 1;
        } catch (Throwable t) {
            Log.e(TAG, "show failed", t);
            return 0;
        }
    }

    public static double adivery_show_app_open(String placementId) {
        try {
            final Activity act = getActivity();
            if (act != null) {
                act.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        try {
                            invokeAdivery("showAppOpenAd", new Class[]{Activity.class, String.class}, act, placementId);
                            setLastEvent("appopen_show_called", placementId, null, null);
                        } catch (Throwable t) {
                            Log.e(TAG, "show_app_open(UI) failed", t);
                            setLastEvent("appopen_show_error", placementId, String.valueOf(t.getMessage()), null);
                        }
                    }
                });
                return 1;
            } else {
                // Fallback without Activity (less reliable)
                invokeAdivery("showAppOpenAd", new Class[]{Activity.class, String.class}, null, placementId);
                setLastEvent("appopen_show_called", placementId, null, null);
                return 1;
            }
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

    private static synchronized void registerLifecycleCallbacks() {
        if (lifecycleRegistered) return;
        try {
            final Application app = getApplication();
            if (app == null) return;
            app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override public void onActivityCreated(Activity activity, android.os.Bundle bundle) {}
                @Override public void onActivityStarted(Activity activity) {}
                @Override public void onActivityResumed(Activity activity) {
                    if (awaitingResume && currentAdPlacement != null) {
                        setLastEvent("onAdClosed", currentAdPlacement, null, null);
                        awaitingResume = false;
                        currentAdPlacement = null;
                    }
                }
                @Override public void onActivityPaused(Activity activity) {}
                @Override public void onActivityStopped(Activity activity) {}
                @Override public void onActivitySaveInstanceState(Activity activity, android.os.Bundle bundle) {}
                @Override public void onActivityDestroyed(Activity activity) {}
            });
            lifecycleRegistered = true;
        } catch (Throwable ignored) {}
    }

    // Simple Android toast message for on-device user feedback
    public static double adivery_toast(final String message) {
        try {
            final Activity act = getActivity();
            if (act != null) {
                act.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        try {
                            android.widget.Toast.makeText(act, (message == null ? "" : message), android.widget.Toast.LENGTH_SHORT).show();
                        } catch (Throwable ignored) {}
                    }
                });
                return 1;
            }
        } catch (Throwable ignored) {}
        return 0;
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
