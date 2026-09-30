package com.example.dacawake;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView levelText;
    private int amplitude = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("DAC Awake");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView desc = new TextView(this);
        desc.setText("Nagyon halk, folyamatos jelet küld az audio kimenetre, hogy az USB DAC ne aludjon el. Nem kér audio fókuszt, ezért a Duolingo hangjával együtt működik.");
        desc.setTextSize(17);
        desc.setPadding(0, pad, 0, pad);
        root.addView(desc);

        levelText = new TextView(this);
        levelText.setTextSize(18);
        updateLevelText();
        root.addView(levelText);

        SeekBar seek = new SeekBar(this);
        seek.setMax(127); // 1..128 PCM amplitude
        seek.setProgress(amplitude - 1);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                amplitude = progress + 1;
                updateLevelText();
                Intent i = new Intent(MainActivity.this, KeepAliveService.class);
                i.setAction(KeepAliveService.ACTION_SET_LEVEL);
                i.putExtra(KeepAliveService.EXTRA_AMPLITUDE, amplitude);
                startService(i);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        root.addView(seek, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = new TextView(this);
        hint.setText("Kezdésnek: 4. Ha a DAC még elalszik, emeld lassan. A legkisebb működő értéket használd.");
        hint.setPadding(0, 0, 0, pad);
        root.addView(hint);

        Button start = new Button(this);
        start.setText("INDÍTÁS");
        start.setOnClickListener(v -> {
            Intent i = new Intent(this, KeepAliveService.class);
            i.setAction(KeepAliveService.ACTION_START);
            i.putExtra(KeepAliveService.EXTRA_AMPLITUDE, amplitude);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        });
        root.addView(start, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button stop = new Button(this);
        stop.setText("LEÁLLÍTÁS");
        stop.setOnClickListener(v -> {
            Intent i = new Intent(this, KeepAliveService.class);
            i.setAction(KeepAliveService.ACTION_STOP);
            startService(i);
        });
        root.addView(stop, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView note = new TextView(this);
        note.setText("Tipp Xiaomi/HyperOS alatt: ha a rendszer lelövi a háttérben, állítsd az app akkumulátorhasználatát Korlátozás nélkül értékre.");
        note.setPadding(0, pad, 0, 0);
        root.addView(note);

        setContentView(root);
    }

    private void updateLevelText() {
        double db = 20.0 * Math.log10(amplitude / 32767.0);
        levelText.setText(String.format("Jelszint: %d PCM (kb. %.1f dBFS)", amplitude, db));
    }
}
