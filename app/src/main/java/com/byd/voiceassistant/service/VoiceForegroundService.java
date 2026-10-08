package com.byd.voiceassistant.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import com.byd.voiceassistant.audio.capture.AudioRecordSource;
import com.byd.voiceassistant.audio.wakeword.WakeWordDetector;
import com.byd.voiceassistant.presentation.settings.SettingsActivity;

public class VoiceForegroundService extends Service {

    public static final String CHANNEL_ID = "voice_service_channel";
    public static final int NOTIFICATION_ID = 1001;

    private PowerManager.WakeLock wakeLock;
    private AudioRecordSource audioRecordSource;
    private WakeWordDetector wakeWordDetector;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        acquireWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startInForeground();
        startAudioListening();
        return START_STICKY;
    }

    private void startInForeground() {
        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
            .setContentTitle("BYD Voice Assistant Active")
            .setContentText("Hands-free wake word listening enabled")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "BYD Voice Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background wake word listening and head unit voice assistant service");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void acquireWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BYDVoiceAssistant:WakeLock");
                wakeLock.acquire(10 * 60 * 1000L /* 10 minutes */);
            }
        } catch (Exception ignored) {
        }
    }

    private void startAudioListening() {
        if (audioRecordSource == null) {
            wakeWordDetector = new WakeWordDetector(keyword -> {
                // Wake word detected! Launch or signal overlay
            });
            wakeWordDetector.start();

            audioRecordSource = new AudioRecordSource(chunk -> {
                if (wakeWordDetector != null) {
                    wakeWordDetector.processPcmChunk(chunk);
                }
            });
            audioRecordSource.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (audioRecordSource != null) {
            audioRecordSource.stop();
            audioRecordSource = null;
        }
        if (wakeWordDetector != null) {
            wakeWordDetector.stop();
            wakeWordDetector = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
