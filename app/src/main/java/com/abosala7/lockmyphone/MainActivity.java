package com.abosala7.lockmyphone;

import android.app.KeyguardManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String MINUTES_KEY = "minutes_key";
    public static final String SP_NAME = "lockMyPhoneSP";
    private static final int ENABLE_DEVICE_ADMIN_RQ = 10;
    private static final int AUTH_RQ = 16;
    private static final int CANCEL_AUTH_RQ = 14;
    public Button cancelTimer;
    public TextView timeToLockTextView;
    private EditText minutes;
    private Intent startLockServiceIntent;
    private SharedPreferences preferences;
    private String timeToLock;
    private String lockTimeLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        minutes = findViewById(R.id.time);
        cancelTimer = findViewById(R.id.cancel_timer);
        startLockServiceIntent = new Intent(this, LockTimerService.class);
        timeToLockTextView = findViewById(R.id.time_to_lock);
        lockTimeLabel = getResources().getString(R.string.lock_time_tv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        timeToLock = preferences.getString(LockTimerService.LOCK_TIME_SP_KEY, "");
        timeToLockTextView.setText(lockTimeLabel + " " + timeToLock);
        boolean isServiceRunning = preferences.getBoolean(LockTimerService.CANCEL_BUTTON_KEY, false);
        cancelTimer.setEnabled(isServiceRunning);
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    public void lockDevice(View view) {
        DevicePolicyManager manager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName component = new ComponentName(this, DeviceAdmin.class);
        if (!manager.isAdminActive(component)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable device admin to run the app");
            startActivityForResult(intent, ENABLE_DEVICE_ADMIN_RQ);
        } else {
            if (!minutes.getText().toString().isEmpty()) {
                checkUserCredentials();
            } else {
                Snackbar.make(getWindow().getDecorView().getRootView(), getResources().getString(R.string.valid_value), Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void checkUserCredentials() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (km.isKeyguardSecure()) {
                Intent authIntent = km.createConfirmDeviceCredentialIntent(getString(R.string.auth_title), getString(R.string.auth_desc));
                startActivityForResult(authIntent, AUTH_RQ);
            }
        } else {
            startLockService();
        }
    }

    private void startLockService() {
        startLockServiceIntent.putExtra(MINUTES_KEY, Integer.parseInt(minutes.getText().toString()));
        ContextCompat.startForegroundService(this, startLockServiceIntent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ENABLE_DEVICE_ADMIN_RQ && resultCode == RESULT_CANCELED) {
            Snackbar.make(getWindow().getDecorView().getRootView(), getResources().getString(R.string.enable_admin), Snackbar.LENGTH_SHORT).show();
        } else if (requestCode == AUTH_RQ && resultCode == RESULT_OK) {
            startLockService();
        } else if (requestCode == CANCEL_AUTH_RQ && resultCode == RESULT_OK) {
            stopService(startLockServiceIntent);
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(LockTimerService.LOCK_TIME_SP_KEY)) {
            timeToLock = sharedPreferences.getString(LockTimerService.LOCK_TIME_SP_KEY, "");
            timeToLockTextView.setText(lockTimeLabel + " " + timeToLock);
        } else if (key.equals(LockTimerService.CANCEL_BUTTON_KEY)) {
            cancelTimer.setEnabled(sharedPreferences.getBoolean(LockTimerService.CANCEL_BUTTON_KEY, false));
        }
    }

    public void cancelTimer(View view) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (km.isKeyguardSecure()) {
                Intent authIntent = km.createConfirmDeviceCredentialIntent(getString(R.string.auth_title), getString(R.string.auth_desc));
                startActivityForResult(authIntent, CANCEL_AUTH_RQ);
            }
        } else {
            stopService(startLockServiceIntent);
        }

    }

    public static class DeviceAdmin extends DeviceAdminReceiver {
        @Override
        public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
            super.onEnabled(context, intent);
        }

        @Override
        public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
            super.onDisabled(context, intent);
        }
    }
}