package com.example.face.Detect;
/**
 * 二维码activity
 */

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zxing.ZXingView;
import com.example.face.R;

public class QRCodeActivity extends AppCompatActivity {

    private QRCodeView mQRCodeView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        mQRCodeView = (ZXingView) findViewById(R.id.zxingview);
        mQRCodeView.changeToScanQRCodeStyle();
        mQRCodeView.startSpot();
        mQRCodeView.setDelegate(new QRCodeView.Delegate() {
            @Override
            public void onScanQRCodeSuccess(String result) {
                Toast.makeText(QRCodeActivity.this,result,Toast.LENGTH_SHORT).show();
                mQRCodeView.startSpot();
            }

            @Override
            public void onScanQRCodeOpenCameraError() {
                Toast.makeText(QRCodeActivity.this,"fail to open camera",Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.start_spot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mQRCodeView.startSpot();
                Toast.makeText(QRCodeActivity.this, "startSpot", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.stop_spot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mQRCodeView.stopSpot();
                Toast.makeText(QRCodeActivity.this, "stopSpot", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.open_flashlight).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mQRCodeView.openFlashlight();
                Toast.makeText(QRCodeActivity.this, "openFlashlight", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.close_flashlight).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mQRCodeView.closeFlashlight();
                Toast.makeText(QRCodeActivity.this, "closeFlashlight", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mQRCodeView.startCamera();
        mQRCodeView.showScanRect();
    }

    @Override
    protected void onStop() {
        mQRCodeView.stopCamera();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mQRCodeView.onDestroy();
        super.onDestroy();
    }
}
