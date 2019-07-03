/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kircherelectronics.accelerationexplorer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.kircherelectronics.accelerationexplorer.control.DeviceController;
import com.kircherelectronics.accelerationexplorer.control.HttpDeviceController;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity implements Runnable {
    //TODO: scan network for the robot instead of hardcoding,
    //move HTTP controls to a separate class.
    public static final String IP = "192.168.4.1";

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    private Thread thread;
    public BluetoothLeService mBluetoothLeService;
    public boolean mConnected = false;

    private int l = 0, r = 0, vel = 0;
    boolean flagTouchLeft = false;
    boolean flagTouchRight = false;
    boolean buttonClicked = false;
    boolean fAuto = false;

    //TODO remove
    private final DeviceController test = new HttpDeviceController(IP);

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData();
            }
        }
    };
    private String mDeviceAddress;
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controle_robo);

        final Intent intent = getIntent();
        String mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        initButtons();

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @SuppressLint("ClickableViewAccessibility")
    private void initButtons() {

        final Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!buttonClicked) {
                    buttonClicked = true;
                    button.setBackgroundResource(R.drawable.btoff);
                    thread = new Thread(DeviceControlActivity.this);
                    thread.start();
                } else {
                    buttonClicked = false;
                    button.setBackgroundResource(R.drawable.bton);
                    thread.interrupt();
                    thread = null;
                    l = 0;
                    r = 0;
                    vel = 0;
                }
            }
        });

        Button btLeft = findViewById(R.id.left);
        btLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    flagTouchLeft = true;
                }
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    flagTouchLeft = false;
                }
                return true;
            }
        });

        Button btRight = findViewById(R.id.right);
        btRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    flagTouchRight = true;
                }
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    flagTouchRight = false;
                }
                return true;
            }
        });

        Button btFront = findViewById(R.id.front);
        btFront.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (vel >= 0 && vel < 5) {
                        vel++;
                    } else if (vel == 6) {
                        vel = 0;
                    } else if (vel > 6 && vel <= 10) {
                        vel--;
                    }
                }
                return true;
            }
        });

        Button btBack = findViewById(R.id.back);
        btBack.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (vel > 0 && vel <= 5) {
                        vel--;
                    } else if (vel == 0){
                        Toast.makeText(DeviceControlActivity.this, R.string.back_broken, Toast.LENGTH_SHORT).show();
                        //vel = 6;
                    } else if (vel >= 6 && vel < 10) {
                        vel++;
                    }
                }
                return true;
            }
        });

        Button stopButton = findViewById(R.id.stop);
        stopButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    vel = 0;
                }
                return true;
            }
        });
    }



    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            //TODO: Figure out what delay to use and move to a scheduled executor
            try {
                Thread.sleep(25);
            } catch (InterruptedException ignored) {}

            if (flagTouchLeft) {
                if (vel > 0 && vel <= 5) {
                    if (l > 0)
                        l--;
                } else if(vel >= 6 && vel <= 10) {
                    if (l > 6)
                        l--;
                    else if (l == 6)
                        l = 0;
                }
            } else {
                l = vel;
            }

            if (flagTouchRight) {
                if (vel > 0 && vel <= 5) {
                    if (r > 0) {
                        r--;
                    }
                }
                else if(vel >= 6 && vel <= 10) {
                    if (r > 6) {
                        r--;
                    } else if (r == 6) {
                        r = 0;
                    }
                }
            } else {
                r = vel;
            }

            if(vel == 0) {
                if(flagTouchLeft) {
                    r = 1;
                }
                if(flagTouchRight) {
                    l = 1;
                }
            }

            onClickWrite(l, r);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //TODO tirar função
    public void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mConnectionState.setText(resourceId);
            }
        });
    }

    public void displayData() {
        //mDataField.setText("l: "+l+" r: "+r);

    }

    public void onClickWrite(int l, int r) {
        if (mBluetoothLeService != null) {
            if (!fAuto) {
                mBluetoothLeService.writeCustomCharacteristic((l * 16) + r);
            } else {
                mBluetoothLeService.writeCustomCharacteristic(0xFF);
            }
        }
        test.sendControl(l, r);
    }

    public void onClickAuto(View v) {
        Toast.makeText(this, R.string.dont_do_that, Toast.LENGTH_SHORT).show();
        /*if (!fAuto) {
            fAuto = true;
        } else {
            fAuto = false;
            vel = r = l = 0;
        }*/
    }
}



