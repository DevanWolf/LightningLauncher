package com.threethan.launcher.browser.GeckoView.Delegate;

import static android.content.Context.DOWNLOAD_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;

import com.threethan.launcher.R;
import com.threethan.launcher.browser.BrowserActivity;
import com.threethan.launcher.browser.BrowserService;
import com.threethan.launcher.helper.Dialog;
import com.threethan.launcher.support.Updater;

import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.WebResponse;

public class CustomContentDelegate implements GeckoSession.ContentDelegate {
    final BrowserActivity mActivity;

    public CustomContentDelegate(BrowserActivity mActivity) {
        this.mActivity = mActivity;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onExternalResponse(@NonNull GeckoSession session, @NonNull WebResponse response) {

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(response.uri));

        final String filename= URLUtil.guessFileName(response.uri,
                response.headers.get("Content-Disposition"),
                response.headers.get("Content-Type"));

        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!

        if (filename.endsWith(".apk")) request.setDestinationInExternalFilesDir(mActivity, Updater.APK_DIR, filename);
        else try {
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        } catch (IllegalStateException ignored) {
        // If we can't access downloads dir
        request.setDestinationInExternalFilesDir(mActivity, Environment.DIRECTORY_DOWNLOADS, filename);
        }

        DownloadManager manager = (DownloadManager) mActivity.getSystemService(DOWNLOAD_SERVICE);

        mActivity.registerReceiver(mActivity.wService.onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        final long id = manager.enqueue(request);
        BrowserService.downloadFilenameById.put(id, filename);
        BrowserService.downloadActivityById.put(id, mActivity);

        Dialog.toast(mActivity.getString(R.string.web_download_started), filename, true);
        GeckoSession.ContentDelegate.super.onExternalResponse(session, response);
    }

    @Override
    public void onFullScreen(@NonNull GeckoSession session, boolean fullScreen) {
        GeckoSession.ContentDelegate.super.onFullScreen(session, fullScreen);
        if (fullScreen) mActivity.hideTopBar();
        else mActivity.showTopBar();
    }
}
