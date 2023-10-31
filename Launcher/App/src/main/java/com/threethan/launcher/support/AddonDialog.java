package com.threethan.launcher.support;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.threethan.launcher.R;
import com.threethan.launcher.helper.App;
import com.threethan.launcher.helper.AppData;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.launcher.LauncherActivity;

import java.lang.ref.WeakReference;
import java.util.Objects;

/*
    AddonDialog

    This class handles the addon manager, AKA shortcut settings
    It handles related popups, and updates buttons according to the state returned by Addon
 */
public abstract class AddonDialog {
    private static WeakReference<Updater> updaterRef;
    private static WeakReference<LauncherActivity> activityRef;
    public static void showAddons(LauncherActivity a) {
        AlertDialog dialog = Dialog.build(a, Platform.isVr(a) ? R.layout.dialog_addons_vr : R.layout.dialog_addons_tv);
        if (dialog == null) return;
        activityRef = new WeakReference<>(a);

        View addonFacebook = dialog.findViewById(R.id.addonFacebook);
        if (addonFacebook!=null) updateAddonButton(a, addonFacebook, Updater.TAG_FACEBOOK_SHORTCUT);

        View addonExplore = dialog.findViewById(R.id.addonExplore);
        if (addonExplore!=null) {
            updateAddonButton(a, addonExplore, Updater.TAG_HORIZON_FEED_SHORTCUT);
            dialog.findViewById(R.id.disableExplore).setOnClickListener(v -> App.openInfo(a, AppData.EXPLORE_PACKAGE));
            ((TextView) dialog.findViewById(R.id.disableExploreWhy)).setText(
                    App.isPackageEnabled(a, AppData.EXPLORE_PACKAGE) ?
                            R.string.addons_explore_disable_why : R.string.addons_explore_enable_why);
            ((TextView) dialog.findViewById(R.id.disableExploreText)).setText(
                    App.isPackageEnabled(a, AppData.EXPLORE_PACKAGE) ?
                    R.string.addons_explore_disable : R.string.addons_explore_enable);
        }

        View addonLibrary = dialog.findViewById(R.id.addonLibrary);
        if (addonLibrary!=null) updateAddonButton(a, addonLibrary, Updater.TAG_APP_LIBRARY_SHORTCUT);

        View addonPeople = dialog.findViewById(R.id.addonPeople);
        if (addonLibrary!=null) updateAddonButton(a, addonPeople, Updater.TAG_PEOPLE_SHORTCUT);

        View addonAndroidTv = dialog.findViewById(R.id.addonAndroidTv);
        if (addonAndroidTv!=null) updateAddonButton(a, addonAndroidTv, Updater.TAG_ANDROID_TV_SHORTCUT);

        dialog.findViewById(R.id.exitButton).setOnClickListener(v -> dialog.dismiss());
    }
    public static void updateAddonButton(final Activity a, final View outerView, final String tag) {
        final View uninstallButton = outerView.findViewById(R.id.addonUninstall);
        final View installButton = outerView.findViewById(R.id.addonInstall);
        final View updateButton = outerView.findViewById(R.id.addonUpdate);
        final View activateButton = outerView.findViewById(R.id.addonActivate);

        View icon = outerView.findViewById(R.id.icon);
        if (icon != null) icon.setClipToOutline(true);
        Runnable updateButtonRunnable = new Runnable() {
            @Override
            public void run() {
                uninstallButton.setVisibility(View.GONE);
                installButton.setVisibility(View.GONE);
                updateButton.setVisibility(View.GONE);
                activateButton.setVisibility(View.GONE);

                Updater updater = getUpdater();
                if (updater == null) return;

                switch (updater.getAddonState(tag)) {
                    case Updater.STATE_ACTIVE -> uninstallButton.setVisibility(View.VISIBLE);
                    case Updater.STATE_NOT_INSTALLED -> installButton.setVisibility(View.VISIBLE);
                    case Updater.STATE_HAS_UPDATE -> updateButton.setVisibility(View.VISIBLE);
                    case Updater.STATE_INACTIVE -> activateButton.setVisibility(View.VISIBLE);
                }
                outerView.postDelayed(this, 100);
            }
        };
        outerView.post(updateButtonRunnable);

        uninstallButton.setOnClickListener((v -> Objects.requireNonNull(getUpdater()).uninstallAddon(a, tag)));
        installButton.setOnClickListener((v -> Objects.requireNonNull(getUpdater()).installAddon(tag)));
        updateButton.setOnClickListener((v -> Objects.requireNonNull(getUpdater()).installAddon(tag)));
        activateButton.setOnClickListener((v -> showAccessibilityDialog()));
    }
    @Nullable
    protected static Updater getUpdater() {
        Updater updater = null;
        if (updaterRef != null) updater = updaterRef.get();
        if (updater != null) return updater;
        LauncherActivity activity = activityRef.get();
        if (activity != null) {
            updater = new Updater(activity);
            updaterRef = new WeakReference<>(updater);
            return updater;
        }
        return null;
    }

    protected static void showAccessibilityDialog() {
        Activity a = activityRef.get();
        if (a==null) return;
        AlertDialog subDialog = Dialog.build(a, R.layout.dialog_service_info);
        if (subDialog == null) return;
        subDialog.findViewById(R.id.confirm).setOnClickListener(view1 -> {
            // Navigate to accessibility settings
            Intent localIntent = new Intent("android.settings.ACCESSIBILITY_SETTINGS");
            localIntent.setPackage("com.android.settings");
            a.startActivity(localIntent);
        });
    }
}
