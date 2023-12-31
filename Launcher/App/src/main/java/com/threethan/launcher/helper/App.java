package com.threethan.launcher.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.threethan.launcher.R;
import com.threethan.launcher.launcher.LauncherActivity;
import com.threethan.launcher.support.SettingsManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
    App

    This abstract class is provides info about applications, and helper functions for non-launching
    intents (info, uninstall).

    Functions prefixed with "check" check a property of an app using its metadata
    Functions prefixed with "is" are wrappers around "check" functions which cache values
 */



public abstract class App {
    static Map<Type, Set<String>> categoryIncludedApps = new HashMap<>();
    static Map<Type, Set<String>> categoryExcludedApps = new HashMap<>();

    public enum Type {
        TYPE_PHONE, TYPE_VR, TYPE_TV, TYPE_PANEL, TYPE_WEB, TYPE_SUPPORTED, TYPE_UNSUPPORTED
    }
    private static boolean checkVirtualReality(ApplicationInfo applicationInfo) {
        if (applicationInfo.metaData == null) return false;
        if (applicationInfo.metaData.containsKey("com.oculus.supportedDevices")) return true;
        if (applicationInfo.metaData.containsKey("com.oculus.ossplash")) return true;
        if (applicationInfo.metaData.containsKey("com.samsung.android.vr.application.mode")) return true;
        // Just matches all unity apps, good enough for now
        return applicationInfo.metaData.containsKey("notch.config")
                && applicationInfo.metaData.containsKey("unity.splash-enable");
    }
    private static boolean checkAndroidTv
            (ApplicationInfo applicationInfo, LauncherActivity launcherActivity) {
        PackageManager pm = launcherActivity.getPackageManager();

        // First check for banner
        if (applicationInfo.banner != 0) return true;
        // Then check for intent
        Intent tvIntent = new Intent();
        tvIntent.setAction(Intent.CATEGORY_LEANBACK_LAUNCHER);
        tvIntent.setPackage(applicationInfo.packageName);
        return (tvIntent.resolveActivity(pm) != null);

    }
    private static Set<String> nonNull(Set<String> set) {
        if (set == null) return new HashSet<>();
        else return set;
    }
    protected static boolean isAppOfType
            (ApplicationInfo applicationInfo, App.Type appType) {

            final LauncherActivity launcherActivity = SettingsManager.getAnyLauncherActivity();
            final DataStoreEditor sharedPreferences = launcherActivity.dataStoreEditor;

            if (!categoryIncludedApps.containsKey(appType)) {
                // Create new hashsets for cache
                categoryIncludedApps.put(appType, Collections.synchronizedSet(new HashSet<>()));
                categoryExcludedApps.put(appType, Collections.synchronizedSet(new HashSet<>()));

                // Async load (it's nice to store this data, but it's faster to check initially)
                sharedPreferences.getStringSet(Settings.KEY_INCLUDED_SET + appType, new HashSet<>(),
                        includedSet -> nonNull(categoryIncludedApps.get(appType)).addAll(includedSet));
                sharedPreferences.getStringSet(Settings.KEY_EXCLUDED_SET + appType, new HashSet<>(),
                        includedSet -> nonNull(categoryExcludedApps.get(appType)).addAll(includedSet));
            }

            // Check cache
            if (nonNull(categoryIncludedApps.get(appType))
                    .contains(applicationInfo.packageName)) return true;
            if (nonNull(categoryExcludedApps.get(appType))
                    .contains(applicationInfo.packageName)) return false;

            boolean isType = switch (appType) {
                case TYPE_VR -> checkVirtualReality(applicationInfo);
                case TYPE_TV -> checkAndroidTv(applicationInfo, launcherActivity);
                case TYPE_PANEL -> checkPanelApp(applicationInfo, launcherActivity);
                case TYPE_WEB -> isWebsite(applicationInfo);
                case TYPE_PHONE -> true;
                case TYPE_SUPPORTED -> checkSupported(applicationInfo, launcherActivity);
                default -> false;

                // this function shouldn't be called until checking higher priorities first
            };

        if (isType) {
                nonNull(categoryIncludedApps.get(appType))
                        .add(applicationInfo.packageName);
                sharedPreferences.putStringSet(Settings.KEY_INCLUDED_SET + appType,
                        categoryIncludedApps.get(appType));
            } else {
                nonNull(categoryExcludedApps.get(appType))
                        .add(applicationInfo.packageName);
                sharedPreferences.putStringSet(Settings.KEY_EXCLUDED_SET + appType,
                        categoryIncludedApps.get(appType));
            }

            return isType;
    }


    private static boolean checkPanelApp
            (ApplicationInfo applicationInfo, LauncherActivity launcherActivity) {
        //noinspection SuspiciousMethodCalls
        if (AppData.getFullPanelAppList().contains(applicationInfo)) return true;

        if (AppData.AUTO_DETECT_PANEL_APPS) {
            PackageManager pm = launcherActivity.getPackageManager();
            Intent panelIntent = new Intent("com.oculus.vrshell.SHELL_MAIN");
            panelIntent.setPackage(applicationInfo.packageName);
            return (pm.resolveService(panelIntent, 0) != null);
        } else return false;
    }

    synchronized public static boolean isSupported(ApplicationInfo app) {
        return isAppOfType(app, Type.TYPE_SUPPORTED);
    }
    private static String[] unsupportedPrefixes;
    private static boolean checkSupported(ApplicationInfo app, LauncherActivity launcherActivity) {
        if (isWebsite(app)) return true;

        if (unsupportedPrefixes == null)
            unsupportedPrefixes = launcherActivity.getResources().getStringArray(R.array.unsupported_app_prefixes);
        for (String prefix : unsupportedPrefixes)
            if (app.packageName.startsWith(prefix))
                return false;

        if (app.metaData != null)
            if (app.metaData.keySet().contains("com.oculus.environmentVersion"))
                return isAppOfType(app, Type.TYPE_VR);

        return Launch.checkLaunchable(launcherActivity, app);
    }
    public static boolean isBanner(ApplicationInfo applicationInfo) {
        LauncherActivity launcherActivity = SettingsManager.getAnyLauncherActivity();
        if (launcherActivity == null) return false;
        return typeIsBanner(getType(launcherActivity, applicationInfo));
    }
    /** @noinspection SuspiciousMethodCalls*/
    public static boolean isWebsite(ApplicationInfo applicationInfo) {
        return (isWebsite(applicationInfo.packageName) &&
                !AppData.getFullPanelAppList().contains(applicationInfo));
    }
    public static boolean isWebsite(String packageName) {
        return (packageName.contains("//"));
    }
    public static boolean isShortcut(ApplicationInfo applicationInfo) {
        return isShortcut(applicationInfo.packageName);
    }
    public static boolean isShortcut(String packageName) {
        return packageName.startsWith("{\"mActivity\"") || packageName.startsWith("json://");
    }

        // Invalidate the values caches for isBlank functions
    public static synchronized void invalidateCaches(LauncherActivity launcherActivity) {
        categoryIncludedApps = new HashMap<>();
        categoryExcludedApps = new HashMap<>();

        for (App.Type type : App.Type.values())
            launcherActivity.dataStoreEditor
                    .removeStringSet(Settings.KEY_INCLUDED_SET + type)
                    .removeStringSet(Settings.KEY_EXCLUDED_SET + type);
    }
    // Opens the app info settings pane
    public static void openInfo(Context context, String packageName) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" +
                packageName.replace(PanelApp.packagePrefix, "")));
        context.startActivity(intent);
    }
    // Requests to uninstall the app
    public static void uninstall(LauncherActivity launcher, String packageName) {
        if (App.isWebsite(packageName)) {
            Set<String> webApps = launcher.dataStoreEditor.getStringSet(Settings.KEY_WEBSITE_LIST, Collections.emptySet());
            webApps = new HashSet<>(webApps); // Copy since we're not supposed to modify directly
            if (launcher.browserService != null) launcher.browserService.killWebView(packageName); // Kill web view if running
            webApps.remove(packageName);
            launcher.dataStoreEditor
                    .putString(packageName, null) // set display name
                    .putStringSet(Settings.KEY_WEBSITE_LIST, webApps);
            launcher.refreshAppDisplayListsAll();
        } else {
            Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
            intent.setData(Uri.parse("package:" + packageName));
            launcher.startActivity(intent);
        }
    }

    public static App.Type getType(LauncherActivity launcherActivity, ApplicationInfo app) {
        for (Type type : Platform.getSupportedAppTypes(launcherActivity)) {
            if (isAppOfType(app, type)) return type;
        }
        return Type.TYPE_UNSUPPORTED;
    }

    public static String getTypeString(Activity a, Type type) {
        return switch (type) {
            case TYPE_PHONE -> a.getString(R.string.apps_phone);
            case TYPE_VR -> a.getString(R.string.apps_vr);
            case TYPE_TV -> a.getString(R.string.apps_tv);
            case TYPE_WEB -> a.getString(R.string.apps_web);
            case TYPE_PANEL -> a.getString(R.string.apps_panel);
            default -> "Invalid type";
        };
    }

    public static String getDefaultGroupFor(App.Type type) {
        return SettingsManager.getDefaultGroupFor(type);
    }
    public static boolean typeIsBanner(App.Type type) {
        return SettingsManager.isTypeBanner(type);
    }

    public static boolean isPackageEnabled(Activity activity, String packageName) {
        try {
            ApplicationInfo ai = activity.getPackageManager().getApplicationInfo(packageName,0);
            return ai.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    public static boolean doesPackageExist(Activity activity, String packageName) {
        try {
            ApplicationInfo ignored
                    = activity.getPackageManager().getApplicationInfo(packageName,0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
