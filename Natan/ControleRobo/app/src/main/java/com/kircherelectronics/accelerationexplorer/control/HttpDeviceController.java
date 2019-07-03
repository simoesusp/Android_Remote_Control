package com.kircherelectronics.accelerationexplorer.control;

import android.util.Log;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpDeviceController implements DeviceController {
    private static final String TAG = HttpDeviceController.class.getSimpleName();
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    private final String host;
    private volatile boolean lastRequestSuccessful;

    public HttpDeviceController(String host) {
        this.host = host;
    }

    @Override
    public boolean isConnected() {
        return lastRequestSuccessful;
    }

    @Override
    public void sendControl(int l, int r) {
        int speed1, speed2, dir1, dir2;
        if(l >= 0 && l < 5) {
            speed1 = 1024 * l / 5;
            dir1 = 0;
        } else {
            speed1 = 1024 * (l - 5) / 5;
            dir1 = 1;
        }
        if(r >= 0 && r < 5) {
            speed2 = 1024 * r / 5;
            dir2 = 0;
        } else {
            speed2 = 1024 * (r - 5) / 5;
            dir2 = 1;
        }
        request(String.format("/motor?speed1=%d&speed2=%d&dir1=%d&dir2=%d", speed1, speed2, dir1, dir2));
    }

    private void request(String path) {
        Log.d(TAG, "Executing request: GET " + path);
        HTTP_CLIENT.newCall(new Request.Builder()
                .url("http://" + host + path)
                .build()
        ).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "IO error", e);
                lastRequestSuccessful = false;
            }

            @Override
            public void onResponse(Call call, Response response) {
                Log.i(TAG, "Response: " + response.code() + ", " + response.message());
                lastRequestSuccessful = response.code() / 100 == 2; // 2xx
            }
        });
    }
}
