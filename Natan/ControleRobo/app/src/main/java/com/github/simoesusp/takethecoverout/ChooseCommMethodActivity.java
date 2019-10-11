package com.github.simoesusp.takethecoverout;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.net.MalformedURLException;
import java.net.URL;

public class ChooseCommMethodActivity extends Activity {
    private static final String DEFAULT_ESP32_IP = "192.168.4.1";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.communication_selection);

        Button bluetooth = findViewById(R.id.button_bluetooth);
        Button wifi = findViewById(R.id.button_wifi);
        Button settings = findViewById(R.id.button_settings);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            bluetooth.setEnabled(false);
        }

        bluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChooseCommMethodActivity.this, BluetoothActivity.class));
            }
        });

        wifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickIp();
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChooseCommMethodActivity.this, SettingsActivity.class));
            }
        });
    }

    private void pickIp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.choose_ip);

        final EditText input = new EditText(this);
        input.setText(DEFAULT_ESP32_IP);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String ip = input.getText().toString();
                try {
                    new URL("http://" + ip);
                    Intent i = new Intent(ChooseCommMethodActivity.this, WifiDeviceControlActivity.class);
                    i.putExtra(WifiDeviceControlActivity.HOST_EXTRA_KEY, ip);
                    startActivity(i);
                    dialog.dismiss();
                } catch (MalformedURLException e) {
                    Toast.makeText(ChooseCommMethodActivity.this, R.string.malformed_ip, Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
