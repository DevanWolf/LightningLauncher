package com.threethan.launcher.service.explore;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.Timer;
import java.util.TimerTask;

public class ShortcutAccessibilityService extends AccessibilityService {

    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String eventText = event.getText().toString();
            String exploreAccessibilityEventName = getResources().getString(R.string.accessibility_event_name_old);
            String exploreAccessibilityEventNameNew = getResources().getString(R.string.accessibility_event_name_new);
            if ("[Oculus Explore]".compareTo(eventText) == 0 ||
                    exploreAccessibilityEventName.compareTo(eventText) == 0 ||
                    exploreAccessibilityEventNameNew.compareTo(eventText) == 0) {

                Intent launchIntent = new Intent(this, MainActivity.class);
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

                Log.i("LightningLauncherService", "Opening launcher activity from accessibility event");
                startActivity(launchIntent);
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        startActivity(launchIntent);
                    }
                }, 650);
            }
        }
    }
    public void onInterrupt() {}
}