package com.abosala7.lockmyphone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class LockTimerService extends Service {
    public static final String LOCK_TIME_SP_KEY = "lock-time-key";
    public static final String CANCEL_BUTTON_KEY = "cancel-button-key";
    public static final int NOTIFICATION_SERVICE_ID = 33;
    private static final String NOTI_CHANNEL_ID = "lock-timer";
    private static final CharSequence NOTI_CHANNEL_NAME = "lock timer";
    private static final int MAIN_ACTIVITY_RQ = 16;
    private static Handler timeCounter = new Handler();
    private SharedPreferences.Editor editor;
    private Runnable timerCallback = new Runnable() {
        @Override
        public void run() {
            DevicePolicyManager manager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            manager.lockNow();
            editor.putBoolean(CANCEL_BUTTON_KEY, false);
            editor.putString(LOCK_TIME_SP_KEY, "");
            editor.commit();
            stopSelf();
        }
    };
    private int minutes;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        editor = getSharedPreferences(MainActivity.SP_NAME, MODE_PRIVATE).edit();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        timeCounter.removeCallbacks(timerCallback);
        minutes = intent != null ? intent.getIntExtra(MainActivity.MINUTES_KEY, 0) : 0;
        startForeground(NOTIFICATION_SERVICE_ID, makeNotification());
        timeCounter.postDelayed(timerCallback, TimeUnit.MINUTES.toMillis(minutes));
        Toast.makeText(this,
                getResources().getString(R.string.lock_time)
                        + " "
                        + minutes + " "
                        + getResources().getString(R.string.minutes)
                , Toast.LENGTH_SHORT).show();
        editor.putString(LOCK_TIME_SP_KEY, getTimeToLock());
        editor.putBoolean(CANCEL_BUTTON_KEY, true);
        editor.commit();
        return START_NOT_STICKY;
    }

    private Notification makeNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTI_CHANNEL_ID, NOTI_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTI_CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.notification_title))
                .setContentIntent(getPendingIntent())
                .setSmallIcon(R.drawable.clock)
                .setContentText(getResources().getString(R.string.lock_time) + " " + getTimeToLock())
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.clock));
        return builder.build();
    }


    public String getTimeToLock() {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, minutes);
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm", Locale.getDefault());
        return formatter.format(new Date(now.getTimeInMillis()));
    }

    private PendingIntent getPendingIntent() {
        Intent i = new Intent(this, MainActivity.class);
        return PendingIntent.getActivity(this, MAIN_ACTIVITY_RQ, i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        editor.putBoolean(CANCEL_BUTTON_KEY, false);
        editor.putString(LOCK_TIME_SP_KEY, "");
        editor.commit();
        timeCounter.removeCallbacks(timerCallback);
    }
}
