package com.github.simoesusp.takethecoverout;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_fragment, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            Context ctx = getPreferenceManager().getContext();
            PreferenceScreen p = getPreferenceManager().createPreferenceScreen(ctx);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

            SwitchPreferenceCompat cb = new SwitchPreferenceCompat(ctx);
            cb.setDefaultValue(false);
            cb.setKey("enable-back");
            cb.setChecked(prefs.getBoolean("enable-back", false));
            cb.setTitle(R.string.enable_back);
            cb.setIconSpaceReserved(false);

            p.addPreference(cb);

            SeekBarPreference sb = new SeekBarPreference(ctx);
            sb.setMin(10);
            sb.setMax(100);
            sb.setDefaultValue(25);
            sb.setAdjustable(true);
            sb.setKey("max-speed-percent");
            sb.setValue(prefs.getInt("max-speed-percent", 25));
            sb.setShowSeekBarValue(true);
            sb.setTitle(R.string.max_speed);
            sb.setIconSpaceReserved(false);

            p.addPreference(sb);

            setPreferenceScreen(p);
        }
    }
}
