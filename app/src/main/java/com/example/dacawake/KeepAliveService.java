package com.example.dacawake;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.os.IBinder;

public class KeepAliveService extends Service {
    public static final String ACTION_START = "com.example.dacawake.START";
    public static final String ACTION_STOP = "com.example.dacawake.STOP";
    public static final String ACTION_SET_LEVEL = "com.example.dacawake.SET_LEVEL";
    public static final String EXTRA_AMPLITUDE = "amplitude";

    private static final String CHANNEL_ID = "dac_awake";
    private static final int NOTIFICATION_ID = 7;
    private static final int SAMPLE_RATE = 48000;
    private static final double TONE_HZ = 997.0;

    private volatile boolean running = false;
    private volatile int amplitude = 4;
    private Thread audioThread;
    private AudioTrack track;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : ACTION_START;
        if (intent != null && intent.hasExtra(EXTRA_AMPLITUDE)) {
            amplitude = Math.max(1, Math.min(128, intent.getIntExtra(EXTRA_AMPLITUDE, 4)));
        }

        if (ACTION_STOP.equals(action)) {
            stopAudio();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_SET_LEVEL.equals(action)) {
            return START_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification());
        startAudio();
        return START_STICKY;
    }

    private void startAudio() {
        if (running) return;
        running = true;
        audioThread = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
            int min = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);
            int frames = 2048;
            int shorts = frames * 2;
            int bytes = Math.max(min, shorts * 2 * 2);

            try {
                track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setSampleRate(SAMPLE_RATE)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                                .build())
                        .setBufferSizeInBytes(bytes)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build();

                short[] buf = new short[shorts];
                double phase = 0.0;
                double step = 2.0 * Math.PI * TONE_HZ / SAMPLE_RATE;
                track.play();

                while (running) {
                    int a = amplitude;
                    for (int i = 0; i < frames; i++) {
                        short s = (short) Math.round(Math.sin(phase) * a);
                        phase += step;
                        if (phase >= 2.0 * Math.PI) phase -= 2.0 * Math.PI;
                        buf[i * 2] = s;
                        buf[i * 2 + 1] = s;
                    }
                    track.write(buf, 0, buf.length, AudioTrack.WRITE_BLOCKING);
                }
            } catch (Throwable ignored) {
            } finally {
                if (track != null) {
                    try { track.pause(); } catch (Throwable ignored) {}
                    try { track.flush(); } catch (Throwable ignored) {}
                    try { track.release(); } catch (Throwable ignored) {}
                    track = null;
                }
            }
        }, "DAC-Awake-Audio");
        audioThread.start();
    }

    private void stopAudio() {
        running = false;
        if (audioThread != null) {
            audioThread.interrupt();
            audioThread = null;
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "DAC Awake", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("USB DAC ébren tartása");
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(
                this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent stop = new Intent(this, KeepAliveService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(
                this, 1, stop, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return b.setContentTitle("DAC Awake aktív")
                .setContentText("Nagyon halk jel fut az USB DAC ébren tartásához")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(openPi)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause, "Leállítás", stopPi).build())
                .build();
    }

    @Override
    public void onDestroy() {
        stopAudio();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
