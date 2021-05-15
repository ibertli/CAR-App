package com.example.face.Bluetooth;

import android.widget.Toast;
import com.example.face.Detect.Application;

public class CommonUtils {
    public static void toast(String text) {
        Toast.makeText(Application.getInstance(), text, Toast.LENGTH_SHORT).show();
    }
}
