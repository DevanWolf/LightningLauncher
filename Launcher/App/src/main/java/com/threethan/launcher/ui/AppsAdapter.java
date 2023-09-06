package com.threethan.launcher.ui;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.threethan.launcher.MainActivity;
import com.threethan.launcher.R;
import com.threethan.launcher.helpers.CompatHelper;
import com.threethan.launcher.helpers.SettingsManager;
import com.threethan.launcher.platforms.AbstractPlatform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** @noinspection deprecation, rawtypes */
class IconTask extends AsyncTask {
    @SuppressLint("StaticFieldLeak")
    private ImageView imageView;
    private Drawable appIcon;

    @Override
    protected Object doInBackground(Object[] objects) {
        final ApplicationInfo currentApp = (ApplicationInfo) objects[1];
        final MainActivity mainActivityContext = (MainActivity) objects[2];
        final AbstractPlatform appPlatform = AbstractPlatform.getPlatform(currentApp);
        imageView = (ImageView) objects[3];

        ImageView[] imageViews = {imageView};
        appIcon = appPlatform.loadIcon(mainActivityContext, currentApp, imageViews);

        return null;
    }
    @Override
    protected void onPostExecute(Object _n) {
        imageView.setImageDrawable(appIcon);
    }
}
@SuppressLint("UseSwitchCompatOrMaterialCode")
public class AppsAdapter extends BaseAdapter{
    private static Drawable iconDrawable;
    private static File iconFile;
    private static String packageName;
    private final MainActivity mainActivity;
    private final List<ApplicationInfo> appList;
    private final boolean isEditMode;
    private final boolean showTextLabels;

    public AppsAdapter(MainActivity context, boolean editMode, boolean names, List<ApplicationInfo> myApps) {
        mainActivity = context;
        isEditMode = editMode;
        showTextLabels = names;
        SettingsManager settingsManager = SettingsManager.getInstance(mainActivity);

        ArrayList<String> sortedGroups = settingsManager.getAppGroupsSorted(false);
        ArrayList<String> sortedSelectedGroups = settingsManager.getAppGroupsSorted(true);
        boolean isFirstGroupSelected = !sortedSelectedGroups.isEmpty() && !sortedGroups.isEmpty() && sortedSelectedGroups.get(0).compareTo(sortedGroups.get(0)) == 0;
        appList = settingsManager.getInstalledApps(context, sortedSelectedGroups, isFirstGroupSelected, myApps);
    }

    private static class ViewHolder {
        LinearLayout layout;
        ImageView imageView;
        ImageView imageViewBg;
        TextView textView;
        Button moreButton;
    }

    public int getCount() { return appList.size(); }

    public Object getItem(int position) {
        return appList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    /** @noinspection deprecation*/
    @SuppressWarnings("unchecked")
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        final ApplicationInfo currentApp = appList.get(position);
        LayoutInflater layoutInflater = (LayoutInflater) mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            // Create a new ViewHolder and inflate the layout
            int layout = AbstractPlatform.isWideApp(currentApp, mainActivity) ? R.layout.lv_app_wide : R.layout.lv_app_icon;

            convertView = layoutInflater.inflate(layout, parent, false);
            holder = new ViewHolder();
            holder.layout = convertView.findViewById(R.id.layout);
            holder.imageView = convertView.findViewById(R.id.imageLabel);
            holder.textView = convertView.findViewById(R.id.textLabel);
            holder.moreButton = convertView.findViewById(R.id.moreButton);

            // Set clipToOutline to true on imageView
            convertView.findViewById(R.id.clip).setClipToOutline(true);

            convertView.setTag(holder);

            if (position == 0) {
                mainActivity.updateGridViewHeights();
            }

        } else {
            // ViewHolder already exists, reuse it
            holder = (ViewHolder) convertView.getTag();
        }

        // set value into textview
        String name = SettingsManager.getAppLabel(mainActivity, currentApp);
        holder.textView.setVisibility(showTextLabels ? View.VISIBLE : View.GONE);
        if (showTextLabels) {
            holder.textView.setText(name);
            holder.textView.setTextColor(Color.parseColor(mainActivity.darkMode ? "#FFFFFF" : "#000000"));
            holder.textView.setShadowLayer(6, 0, 0, Color.parseColor(mainActivity.darkMode ? "#000000" : "#20FFFFFF"));
        }

        if (isEditMode) {
            holder.layout.setOnClickListener(view -> {
                boolean selected = mainActivity.selectApp(currentApp.packageName);
                holder.layout.setAlpha(selected? 0.5F : 1.0F);
            });

        } else {
            holder.layout.setOnClickListener(view -> {
                AbstractPlatform platform = AbstractPlatform.getPlatform(currentApp);
                if (platform.runApp(mainActivity, currentApp)) animateOpen(holder);
            });
            holder.layout.setOnLongClickListener(view -> {
                mainActivity.setEditMode(true);
                mainActivity.selectApp(currentApp.packageName);
                return true;
            });
        }
        holder.layout.setOnLongClickListener(view -> {
            showAppDetails(currentApp);
            return true;
        });
        Runnable checkHover = new Runnable() {
            @Override
            public void run() {
                holder.moreButton.setVisibility(holder.layout.isHovered() || holder.moreButton.isHovered() ? View.VISIBLE : View.INVISIBLE);
                holder.layout.postDelayed(this, 250);
            }
        };
        checkHover.run();

        holder.moreButton.setOnClickListener(view -> showAppDetails(currentApp));

        new IconTask().execute(this, currentApp, mainActivity, holder.imageView, holder.imageViewBg);

        return convertView;
    }

    public void onImageSelected(String path, ImageView selectedImageView) {
        CompatHelper.clearIcons(mainActivity);
        if (path != null) {
            Bitmap bitmap = ImageUtils.getResizedBitmap(BitmapFactory.decodeFile(path), 450);
            ImageUtils.saveBitmap(bitmap, iconFile);
            selectedImageView.setImageBitmap(bitmap);
        } else {
            selectedImageView.setImageDrawable(iconDrawable);
            AbstractPlatform.updateIcon(iconFile, packageName, null);
            //No longer sets icon here but that should be fine
        }
    }
    private void showAppDetails(ApplicationInfo currentApp) {
        // set layout
        AlertDialog dialog = DialogHelper.build(mainActivity, R.layout.dialog_app_details);
        // package name
        ((TextView) dialog.findViewById(R.id.packageName)).setText(currentApp.packageName);
        // info action
        dialog.findViewById(R.id.info).setOnClickListener(view -> mainActivity.openAppDetails(currentApp));
        dialog.findViewById(R.id.uninstall).setOnClickListener(view -> {mainActivity.uninstallApp(currentApp); dialog.dismiss();});

        // toggle launch mode
        final boolean[] launchOut = {SettingsManager.getAppLaunchOut(currentApp.packageName)};
        final Switch launchModeSwitch = dialog.findViewById(R.id.launch_mode_switch);
        final View launchModeSection = dialog.findViewById(R.id.launch_mode_section);
        final View refreshIconButton = dialog.findViewById(R.id.refresh_icon_button);
        final boolean isVr = AbstractPlatform.isVirtualRealityApp(currentApp, mainActivity);
        final boolean isWeb = AbstractPlatform.isWebsite(currentApp);

        // load icon
        PackageManager packageManager = mainActivity.getPackageManager();

        ImageView iconImage = dialog.findViewById(R.id.app_icon);
        AbstractPlatform appPlatform = AbstractPlatform.getPlatform(currentApp);

        iconImage.setImageDrawable(appPlatform.loadIcon(mainActivity, currentApp, null));

        iconImage.setClipToOutline(true);
        if (AbstractPlatform.isWideApp(currentApp, mainActivity)) iconImage.getLayoutParams().width = mainActivity.dp(150);

        iconImage.setOnClickListener(iconPickerView -> {
            iconDrawable = currentApp.loadIcon(packageManager);
            packageName = currentApp.packageName;

            iconFile = AbstractPlatform.iconFileForPackage(mainActivity, currentApp.packageName);
            if (iconFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                iconFile.delete();
            }
            mainActivity.setSelectedImageView(iconImage);
            ImageUtils.showImagePicker(mainActivity, MainActivity.PICK_ICON_CODE);
        });

        dialog.findViewById(R.id.info).setVisibility(isWeb ? View.GONE : View.VISIBLE);
        if (isVr || isWeb) { //VR apps MUST launch out, so just hide the option
            launchModeSection.setVisibility(View.GONE);
            refreshIconButton.setVisibility(View.VISIBLE);

            refreshIconButton.setOnClickListener(view -> appPlatform.reloadIcon(mainActivity, currentApp, new ImageView[]{iconImage}));
        } else {
            launchModeSection.setVisibility(View.VISIBLE);
            refreshIconButton.setVisibility(View.GONE);
            launchModeSwitch.setChecked(launchOut[0]);

            launchModeSwitch.setOnCheckedChangeListener((sw, value) -> {
                if (!mainActivity.sharedPreferences.getBoolean(SettingsManager.KEY_SEEN_LAUNCH_OUT_POPUP, false) && value) {
                    launchModeSwitch.setChecked(false); // Revert switch
                    AlertDialog subDialog = DialogHelper.build(mainActivity, R.layout.dialog_launch_out_info);
                    subDialog.findViewById(R.id.confirm).setOnClickListener(view -> {
                        mainActivity.sharedPreferences.edit().putBoolean(SettingsManager.KEY_SEEN_LAUNCH_OUT_POPUP, true).apply();
                        subDialog.dismiss();
                        SettingsManager.setAppLaunchOut(currentApp.packageName, true);
                        launchOut[0] = SettingsManager.getAppLaunchOut(currentApp.packageName);
                        launchModeSwitch.setChecked(true);
                    });
                    subDialog.findViewById(R.id.cancel).setOnClickListener(view -> {
                        subDialog.dismiss(); // Dismiss without setting
                    });
                } else {
                    SettingsManager.setAppLaunchOut(currentApp.packageName, !launchOut[0]);
                    launchOut[0] = SettingsManager.getAppLaunchOut(currentApp.packageName);
                }
            });
        }

        // set name
        String name = SettingsManager.getAppLabel(mainActivity, currentApp);
        final EditText appNameEditText = dialog.findViewById(R.id.app_name);
        appNameEditText.setText(name);
        dialog.findViewById(R.id.confirm).setOnClickListener(view -> {
            SettingsManager.setAppLabel(mainActivity, currentApp, appNameEditText.getText().toString());
            dialog.dismiss();
            mainActivity.refreshInterface();
        });
    }

    private void animateOpen(ViewHolder holder) {

        int[] l = new int[2];
        View clip = holder.layout.findViewById(R.id.clip);
        clip.getLocationInWindow(l);
        int w = clip.getWidth();
        int h = clip.getHeight();

        View openAnim = mainActivity.findViewById(R.id.openAnim);
        openAnim.setX(l[0]);
        openAnim.setY(l[1]);
        ViewGroup.LayoutParams layoutParams = new FrameLayout.LayoutParams(w, h);
        openAnim.setLayoutParams(layoutParams);

        openAnim.setVisibility(View.VISIBLE);

        openAnim.setClipToOutline(true);


        ImageView animIcon = openAnim.findViewById(R.id.openIcon);
        ImageView animIconBg = openAnim.findViewById(R.id.openIconBg);
        animIcon.setImageDrawable(holder.imageView.getDrawable());
        animIconBg.setImageDrawable(holder.imageView.getDrawable());

        View openProgress = mainActivity.findViewById(R.id.openProgress);
        openProgress.setVisibility(View.VISIBLE);

        ObjectAnimator aX = ObjectAnimator.ofFloat(openAnim, "ScaleX", 100f);
        ObjectAnimator aY = ObjectAnimator.ofFloat(openAnim, "ScaleY", 100f);
        ObjectAnimator aA = ObjectAnimator.ofFloat(animIcon, "Alpha", 0f);
        ObjectAnimator aP = ObjectAnimator.ofFloat(openProgress, "Alpha", 0.8f);
        aX.setDuration(1000);
        aY.setDuration(1000);
        aA.setDuration(500);
        aP.setDuration(500);
        aP.setStartDelay(1000);
        aX.start();
        aY.start();
        aA.start();
        aP.start();
    }
}
