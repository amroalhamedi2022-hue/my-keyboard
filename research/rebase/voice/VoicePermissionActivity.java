// SPDX-License-Identifier: GPL-3.0-only
package com.zerokeyboard.ime;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import helium314.keyboard.latin.R;

/** Requests permission in a task separate from the settings and the host editor. */
public final class VoicePermissionActivity extends Activity {
    private static final int MICROPHONE_REQUEST = 81;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            finishAndRemoveTask();
        } else if (savedInstanceState == null) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MICROPHONE_REQUEST);
        }
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request != MICROPHONE_REQUEST) return;
        final boolean granted = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
        Toast.makeText(this, granted ? R.string.zero_voice_permission_granted
                : R.string.zero_voice_permission_denied, Toast.LENGTH_LONG).show();
        finishAndRemoveTask();
    }
}
