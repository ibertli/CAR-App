package com.example.face.Bluetooth;

import com.example.face.Detect.Application;
import com.inuker.bluetooth.library.BluetoothClient;

public class ClientManager {
    private static BluetoothClient mClient;

    public static BluetoothClient getClient() {
        if (mClient == null) {
            synchronized (ClientManager.class) {
                if (mClient == null) {
                    mClient = new BluetoothClient(Application.getInstance());
                }
            }
        }
        return mClient;
    }
}
