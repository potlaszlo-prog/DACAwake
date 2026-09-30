package com.example.dacawake;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "dac_awake_settings";
    private static final String PREF_FREQ = "frequency_hz";
    private static final String PREF_DB = "level_dbfs";

    private static final double MIN_FREQ = 10.0;
    private static final double MAX_FREQ = 22000.0;
    private static final int MIN_DB = -90;
    private static final int MAX_DB = -20;

    private SharedPreferences prefs;
    private EditText frequencyInput;
    private TextView levelText;
    private SeekBar levelSeek;

    private double frequencyHz = 18.0;
    private int levelDb = -50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        frequencyHz = prefs.getFloat(PREF_FREQ, 18.0f);
        levelDb = prefs.getInt(PREF_DB, -50);

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        int pad = dp(20);
        int smallGap = dp(8);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("DAC Awake 2");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());

        TextView desc = new TextView(this);
        desc.setText("Folyamatos tesztjelet küld az audio kimenetre, hogy az USB DAC ne aludjon el. A frekvencia és a jelszint állítható. Nem kér audio fókuszt, ezért a Duolingo mellett is futhat.");
        desc.setTextSize(16);
        desc.setPadding(0, pad, 0, pad);
        root.addView(desc, matchWrap());

        TextView freqLabel = new TextView(this);
        freqLabel.setText("Frekvencia (Hz)");
        freqLabel.setTextSize(18);
        root.addView(freqLabel, matchWrap());

        LinearLayout freqRow = new LinearLayout(this);
        freqRow.setOrientation(LinearLayout.HORIZONTAL);
        freqRow.setGravity(Gravity.CENTER_VERTICAL);

        Button minus = new Button(this);
        minus.setText("−");
        minus.setOnClickListener(v -> setFrequency(readFrequency() - frequencyStep(readFrequency()), true));
        freqRow.addView(minus, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        frequencyInput = new EditText(this);
        frequencyInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        frequencyInput.setGravity(Gravity.CENTER);
        frequencyInput.setText(formatFrequency(frequencyHz));
        frequencyInput.setSelectAllOnFocus(true);
        frequencyInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) setFrequency(readFrequency(), true);
        });
        freqRow.addView(frequencyInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));

        Button plus = new Button(this);
        plus.setText("+");
        plus.setOnClickListener(v -> setFrequency(readFrequency() + frequencyStep(readFrequency()), true));
        freqRow.addView(plus, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        root.addView(freqRow, matchWrap());

        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        presets.setPadding(0, smallGap, 0, pad);

        Button lowPreset = new Button(this);
        lowPreset.setText("18 Hz");
        lowPreset.setOnClickListener(v -> setFrequency(18.0, true));
        presets.addView(lowPreset, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button highPreset = new Button(this);
        highPreset.setText("19 kHz");
        highPreset.setOnClickListener(v -> setFrequency(19000.0, true));
        presets.addView(highPreset, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        root.addView(presets, matchWrap());

        levelText = new TextView(this);
        levelText.setTextSize(18);
        root.addView(levelText, matchWrap());

        levelSeek = new SeekBar(this);
        levelSeek.setMax(MAX_DB - MIN_DB);
        levelSeek.setProgress(levelDb - MIN_DB);
        levelSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                levelDb = MIN_DB + progress;
                updateLevelText();
                if (fromUser) {
                    saveSettings();
                    sendLiveSettings();
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        root.addView(levelSeek, matchWrap());
        updateLevelText();

        TextView hint = new TextView(this);
        hint.setText("Kiindulás a te AB13X DAC-odhoz: 18 Hz / −50 dBFS. Először a jelszintet csökkentsd addig, amíg még nem alszik el a DAC. Ha hallható, próbáld a 18 Hz vagy 19 kHz tartományt.");
        hint.setPadding(0, 0, 0, pad);
        root.addView(hint, matchWrap());

        Button start = new Button(this);
        start.setText("INDÍTÁS");
        start.setOnClickListener(v -> {
            setFrequency(readFrequency(), false);
            saveSettings();
            Intent i = new Intent(this, KeepAliveService.class);
            i.setAction(KeepAliveService.ACTION_START);
            putSettings(i);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        });
        root.addView(start, matchWrap());

        Button stop = new Button(this);
        stop.setText("LEÁLLÍTÁS");
        stop.setOnClickListener(v -> {
            Intent i = new Intent(this, KeepAliveService.class);
            i.setAction(KeepAliveService.ACTION_STOP);
            startService(i);
        });
        root.addView(stop, matchWrap());

        TextView note = new TextView(this);
        note.setText("Megjegyzés: a DAC 16 bites PCM-et használ, ezért kb. −90 dBFS alatt már nincs finomabb, valóban kisebb szinuszjel. HyperOS alatt az app akkumulátorhasználatát érdemes Korlátozás nélkül értékre állítani.");
        note.setPadding(0, pad, 0, 0);
        root.addView(note, matchWrap());

        setContentView(root);
    }

    private void setFrequency(double value, boolean sendLive) {
        frequencyHz = Math.max(MIN_FREQ, Math.min(MAX_FREQ, value));
        frequencyInput.setText(formatFrequency(frequencyHz));
        saveSettings();
        if (sendLive) sendLiveSettings();
    }

    private double readFrequency() {
        try {
            String s = frequencyInput.getText().toString().trim().replace(',', '.');
            return Math.max(MIN_FREQ, Math.min(MAX_FREQ, Double.parseDouble(s)));
        } catch (Exception e) {
            return frequencyHz;
        }
    }

    private double frequencyStep(double f) {
        if (f < 100.0) return 1.0;
        if (f < 1000.0) return 10.0;
        return 100.0;
    }

    private void updateLevelText() {
        int pcm = dbToAmplitude(levelDb);
        double actualDb = 20.0 * Math.log10(pcm / 32767.0);
        levelText.setText(String.format(Locale.US,
                "Jelszint: %d dBFS  (PCM amplitúdó: %d, tényleges kb. %.1f dBFS)",
                levelDb, pcm, actualDb));
    }

    private int dbToAmplitude(int db) {
        int a = (int) Math.round(32767.0 * Math.pow(10.0, db / 20.0));
        return Math.max(1, Math.min(32767, a));
    }

    private void saveSettings() {
        prefs.edit()
                .putFloat(PREF_FREQ, (float) frequencyHz)
                .putInt(PREF_DB, levelDb)
                .apply();
    }

    private void sendLiveSettings() {
        Intent i = new Intent(this, KeepAliveService.class);
        i.setAction(KeepAliveService.ACTION_SET_PARAMS);
        putSettings(i);
        startService(i);
    }

    private void putSettings(Intent i) {
        i.putExtra(KeepAliveService.EXTRA_FREQUENCY_HZ, frequencyHz);
        i.putExtra(KeepAliveService.EXTRA_LEVEL_DBFS, levelDb);
    }

    private String formatFrequency(double f) {
        if (Math.abs(f - Math.rint(f)) < 0.001) return String.format(Locale.US, "%.0f", f);
        return String.format(Locale.US, "%.1f", f);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
