/*
 *  Copyright (C) 2019-2020 The Evolution X Project
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

package com.android.internal.util.evolution;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import android.widget.Toast;

import com.android.internal.R;

import com.android.internal.statusbar.IStatusBarService;

public class ActionUtils {

    public static final String INTENT_SCREENSHOT = "action_handler_screenshot";
    public static final String INTENT_REGION_SCREENSHOT = "action_handler_region_screenshot";

    // Cycle ringer modes
    public static void toggleRingerModes (Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        Vibrator mVibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);

        switch (am.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:
                if (mVibrator.hasVibrator()) {
                    am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                }
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                notificationManager.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                break;
            case AudioManager.RINGER_MODE_SILENT:
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                break;
        }
    }

    // Trigger Hush Mute mode
    public static void triggerHushMute(Context context) {
        // We can't call AudioService#silenceRingerModeInternal from here, so this is a partial copy of it
        int silenceRingerSetting = Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.VOLUME_HUSH_GESTURE, Settings.Secure.VOLUME_HUSH_OFF,
                UserHandle.USER_CURRENT);

        int ringerMode;
        int toastText;
        if (silenceRingerSetting == Settings.Secure.VOLUME_HUSH_VIBRATE) {
            ringerMode = AudioManager.RINGER_MODE_VIBRATE;
            toastText = com.android.internal.R.string.volume_dialog_ringer_guidance_vibrate;
        } else {
            // VOLUME_HUSH_MUTE and VOLUME_HUSH_OFF
            ringerMode = AudioManager.RINGER_MODE_SILENT;
            toastText = com.android.internal.R.string.volume_dialog_ringer_guidance_silent;
        }
        AudioManager audioMan = (AudioManager)
                context.getSystemService(Context.AUDIO_SERVICE);
        audioMan.setRingerModeInternal(ringerMode);
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show();
    }

    // Screenshots
    public static void takeScreenshot(boolean full) {
        IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        try {
            wm.sendCustomAction(new Intent(full? INTENT_SCREENSHOT : INTENT_REGION_SCREENSHOT));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // Turn off the screen
    public static void switchScreenOff(Context ctx) {
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        if (pm!= null && pm.isScreenOn()) {
            pm.goToSleep(SystemClock.uptimeMillis());
        }
    }

    // Turn screen on
    public static void switchScreenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm == null) return;
        pm.wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:CAMERA_GESTURE_PREVENT_LOCK");
    }

    // Force stop packages
    public static void killForegroundApp() {
        FireActions.killForegroundApp();
    }

    // Volume Panel
    public static void toggleVolumePanel(Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
    }

    // Toggle flashlight on/off
    public static void toggleCameraFlash() {
        FireActions.toggleCameraFlash();
    }

    // Launch a custom app/activity
    public static void launchApp(Context context, boolean leftEdgeApp, boolean isVerticalSwipe) {
        Intent intent = null;
        String packageName = Settings.System.getStringForUser(context.getContentResolver(),
                leftEdgeApp ? (isVerticalSwipe ? Settings.System.LEFT_VERTICAL_BACK_SWIPE_APP_ACTION : Settings.System.LEFT_LONG_BACK_SWIPE_APP_ACTION)
                : (isVerticalSwipe ? Settings.System.RIGHT_VERTICAL_BACK_SWIPE_APP_ACTION : Settings.System.RIGHT_LONG_BACK_SWIPE_APP_ACTION),
                UserHandle.USER_CURRENT);
        String activity = Settings.System.getStringForUser(context.getContentResolver(),
                leftEdgeApp ? (isVerticalSwipe ? Settings.System.LEFT_VERTICAL_BACK_SWIPE_APP_ACTIVITY_ACTION : Settings.System.LEFT_LONG_BACK_SWIPE_APP_ACTIVITY_ACTION)
                : (isVerticalSwipe ? Settings.System.RIGHT_VERTICAL_BACK_SWIPE_APP_ACTIVITY_ACTION : Settings.System.RIGHT_LONG_BACK_SWIPE_APP_ACTIVITY_ACTION),
                UserHandle.USER_CURRENT);
        boolean launchActivity = activity != null && !TextUtils.equals("NONE", activity);
        try {
            if (launchActivity) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName(packageName, activity);
            } else {
                intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        } catch (Exception e) {}
    }

    // Toggle notifications panel
    public static void toggleNotifications() {
        FireActions.toggleNotifications();
    }

    // Toggle qs panel
    public static void toggleQsPanel() {
        FireActions.toggleQsPanel();
    }
    // Clear-all notifications
    public static void clearAllNotifications() {
        FireActions.clearAllNotifications();
    }

    // Start Assistant
    public static void startAssist() {
        FireActions.startAssist();
    }

    // Launch default camera activity
    public static void launchCamera(Context context) {
        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    // Launch Voice Search activity
    public static void launchVoiceSearch(Context context) {
        Intent intent = new Intent(Intent.ACTION_SEARCH_LONG_PRESS);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    // Launch Power Menu dialog
    public static void showPowerMenu() {
        final IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        try {
            wm.showGlobalActions();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private static final class FireActions {
        private static IStatusBarService mStatusBarService = null;
        private static IStatusBarService getStatusBarService() {
            synchronized (FireActions.class) {
                if (mStatusBarService == null) {
                    mStatusBarService = IStatusBarService.Stub.asInterface(
                            ServiceManager.getService("statusbar"));
                }
                return mStatusBarService;
            }
        }

        public static void killForegroundApp() {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.killForegroundApp();
                } catch (RemoteException e) {
                    // do nothing.
                }
            }
        }

        public static void toggleCameraFlash() {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.toggleCameraFlash();
                } catch (RemoteException e) {
                    // do nothing.
                }
            }
        }

        public static void startAssist() {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.startAssist(new Bundle());
                } catch (RemoteException e) {}
            }
        }

        public static void toggleNotifications() {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.togglePanel();
                } catch (RemoteException e) {}
            }
        }

        public static void toggleQsPanel() {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.toggleSettingsPanel();
                } catch (RemoteException e) {}
            }
        }

        public static void clearAllNotifications() {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.onClearAllNotifications(ActivityManager.getCurrentUser());
                } catch (RemoteException e) {}
            }
        }
    }
}
