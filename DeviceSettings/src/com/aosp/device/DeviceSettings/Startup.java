/*
* Copyright (C) 2021 Yet Another AOSP Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.aosp.device.DeviceSettings;

import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static android.content.Intent.ACTION_LOCKED_BOOT_COMPLETED;

import static com.aosp.device.DeviceSettings.ModeSwitch.DCModeSwitch.KEY_DC_SWITCH;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import androidx.preference.PreferenceManager;

import com.aosp.device.DeviceSettings.ModeSwitch.*;

import java.util.Map;

public class Startup extends BroadcastReceiver {

    private static final String KEY_MIGRATION_DONE = "migration_done";

    private static final Map<String, String> sKeyFileMap = Map.of(
        // DC Dimming
        KEY_DC_SWITCH, DCModeSwitch.getFile(),
    );

    private void restore(String file, boolean enabled) {
        if (file == null) return;
        if (enabled) Utils.writeValue(file, "1");
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final SharedPreferences dePrefs = Constants.getDESharedPrefs(context);

        if (intent.getAction().equals(ACTION_BOOT_COMPLETED)) {
            if (!dePrefs.getBoolean(KEY_MIGRATION_DONE, false)) {
                // migration of old user encrypted preferences
                final SharedPreferences oldPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                final SharedPreferences.Editor oldPrefsEditor = oldPrefs.edit();
                final SharedPreferences.Editor dePrefsEditor = dePrefs.edit();

                for (String prefKey : sKeyFileMap.keySet()) {
                    if (!oldPrefs.contains(prefKey)) continue;
                    dePrefsEditor.putBoolean(prefKey, oldPrefs.getBoolean(prefKey, false));
                    oldPrefsEditor.remove(prefKey);
                }

                dePrefsEditor.putBoolean(KEY_MIGRATION_DONE, true);
                // must use commit (and not apply) because of what follows!
                dePrefsEditor.commit();
                oldPrefsEditor.commit();
            }

            // Touchscreen gestures
            TouchscreenGestureSettings.MainSettingsFragment.restoreTouchscreenGestureStates(context);
        }

        // restoring state from DE shared preferences
        for (Map.Entry<String, String> set : sKeyFileMap.entrySet()) {
            final String prefKey = set.getKey();
            final String file = set.getValue();
            restore(file, dePrefs.getBoolean(prefKey, false));
        }
    }
}
