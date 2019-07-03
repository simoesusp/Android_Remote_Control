package com.github.simoesusp.takethecoverout.control;

public interface DeviceController {
    boolean isConnected();

    void sendControl(int l, int r);
}
