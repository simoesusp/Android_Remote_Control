package com.kircherelectronics.accelerationexplorer.control;

public interface DeviceController {
    boolean isConnected();

    void sendControl(int l, int r);
}
