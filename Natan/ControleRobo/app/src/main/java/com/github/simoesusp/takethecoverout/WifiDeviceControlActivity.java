package com.github.simoesusp.takethecoverout;

import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WifiDeviceControlActivity extends BaseDeviceControlActivity {
    private static final int MAX_SPEED_VALUE = 256;

    private static final String TAG = WifiDeviceControlActivity.class.getSimpleName();
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    public static final String HOST_EXTRA_KEY = "HOST";

    private final AtomicInteger requestsExecuting = new AtomicInteger();

    private String host;

    private volatile int lastL, lastR;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        host = getIntent().getStringExtra(HOST_EXTRA_KEY);
    }

    @Override
    protected void sendMotorSpeed(boolean automatic, int l, int r) {
        if(!automatic) {
            if(lastL == l && lastR == r) {
                return;
            }
            lastL = l;
            lastR = r;
        } else {
            synchronized(this) {
                if(requestsExecuting.get() > 0) {
                    return;
                }
            }
        }
        int speed1, speed2, dir1, dir2;
        if(l >= 0 && l < 5) {
            speed1 = MAX_SPEED_VALUE * l / 5;
            dir1 = 0;
        } else {
            speed1 = MAX_SPEED_VALUE * (l - 5) / 5;
            dir1 = 1;
        }
        if(r >= 0 && r < 5) {
            speed2 = MAX_SPEED_VALUE * r / 5;
            dir2 = 0;
        } else {
            speed2 = MAX_SPEED_VALUE * (r - 5) / 5;
            dir2 = 1;
        }

        //and the hardware is yet again fucked
        //swap the two motor speeds so it turns in the right direction
        int temp = speed1;
        speed1 = speed2;
        speed2 = temp;

        synchronized(this) {
            if(requestsExecuting.get() > 0 && automatic) {
                return;
            }
            request(String.format("/motor?speed1=%d&speed2=%d&dir1=%d&dir2=%d", speed1, speed2, dir1, dir2));
        }
    }

    @Override
    protected int sendDelay() {
        return 300;
    }

    private void request(String path) {
        requestsExecuting.incrementAndGet();
        Log.d(TAG, "Executing request: GET " + path);
        HTTP_CLIENT.newCall(new Request.Builder()
                .url("http://" + host + path)
                .build()
        ).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "IO error", e);
                requestsExecuting.decrementAndGet();
            }

            @Override
            public void onResponse(Call call, Response response) {
                Log.i(TAG, "Response: " + response.code() + ", " + response.message());
                response.close();
                requestsExecuting.decrementAndGet();
            }
        });
    }
}
