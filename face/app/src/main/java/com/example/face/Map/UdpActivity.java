package com.example.face.Map;
/**
 * UDP通信activity
 */

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.face.R;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UdpActivity extends AppCompatActivity {

    private static final String TAG = "UDPSocket";

    private EditText etSend;
    private EditText etIp;
    private EditText etPort;
    private EditText etReceive;
    private Button btnSend;
    private Button btnConnect;
    private Button btnDisconnect;
    private TextView tvstate;

    // 单个CPU线程池大小
    private static final int POOL_SIZE = 5;
    private ExecutorService mThreadPool;

    private int CLIENT_PORT = 8002;             //原来是8086
    private int SERVER_PORT = 8012;             //原来是8085
    private String SERVER_IP = "192.168.188.98";//原来是"172.20.10.11"
    private byte bufClient[] = new byte[1024];
    private int BUF_LENGTH = 1024;

    private byte[] receiveInfo;
    private byte[] buf;
    private boolean isThreadRunning = false;

    private InetAddress clientIP;
    private InetAddress serverAddr;
    private int clientPort;

    private DatagramSocket client;
    private DatagramPacket dpClientSend;
    private DatagramPacket dpClientReceive;

    private Thread threadClient;
    private Thread threadData;
    private UDPSocket mUDPSocket;

    private long lastReceiveTime = 0;
    private static final long TIME_OUT = 120 * 1000;
    private static final long HEARTBEAT_MESSAGE_DURATION = 10 * 1000;

    //时间控制
    private int RECEIVE_INTERVAl = 1;
    private int SEND_INTERVAL = 5;
    private long RECEIVE_TIME;
    private long SEND_TIME;

    //发送和接收的数据格式
    private String state_dic = "[{'self_route':0,'self_lat':39,'self_lon':116,'self_yaw':-1,'self_speed':4,'self_adu':0,'self_location':0,'self_confirm':0}]";
    private String msg_dic = "[{'route_id': 0,'confirm': 0,'adu_confirm': 0}]";
    private List<Double> state = new ArrayList<Double>();

    //计算GPS坐标的常量
    private double Loctn_GPS_IniLong = 118.8939929;
    private double Loctn_GPS_IniLat = 31.9496598;
    private double Loctn_GPS_IniHeight = 24.34;
    private double r = 6378137.0;
    private double f = 1/298.257223563;
    private double e2 = f * (2-f);
    private double pi = Math.PI;
    private double temp = Math.sqrt(1 - e2 * Math.pow(Math.sin(Loctn_GPS_IniLat * pi / 180),2));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udp);
        bindView();

        WifiManager manager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = manager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        etReceive.setText(intToIP(ipAddress));

        int cpuNumbers = Runtime.getRuntime().availableProcessors();
        // 根据CPU数目初始化线程池
        mThreadPool = Executors.newFixedThreadPool(cpuNumbers * POOL_SIZE);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                String ip = etIp.getText().toString();
//                int port = Integer.parseInt(etPort.getText().toString());
//                mUDPSocket = new UDPSocket(UdpActivity.this, ip, 8002, port);

                //默认使用本地端口8002，服务器端口8012，服务器IP：172.20.10.2 前往UDPSocket类修改
                mUDPSocket = new UDPSocket(UdpActivity.this);

                mUDPSocket.startUDPSocket();
                isThreadRunning = true;
                startDataThread();
                tvstate.setText("已连接");

            }
        });

        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mUDPSocket != null) {
                    mUDPSocket.stopUDPSocket();
                    tvstate.setText("未连接");
                }
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (mUDPSocket != null) {
//                    mUDPSocket.sendMessage(etSend.getText().toString());
//                }else {
//                    Log.d(TAG,"no udp connect");
//                }
                sendData();
            }
        });

    }

    /**
     * 启动数据处理线程
     */
    private void startDataThread() {
        threadData = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isThreadRunning) {
                    if (mUDPSocket != null && mUDPSocket.isReceiveFlag()) {
                        // TODO: 2019-08-15 在这里处理接收到的数据 
//                        etReceive.setText(mUDPSocket.getReceiveData());
//                        if (TextUtils.isDigitsOnly(mUDPSocket.getReceiveData())) {
//                            etReceive.setText(encodeData(Float.parseFloat(mUDPSocket.getReceiveData())).toString());
//                        }
                        Log.d(TAG,mUDPSocket.getReceiveData());
                        //etReceive.setText(mUDPSocket.getReceiveData());
                        dataProcess(mUDPSocket.getReceiveByte());
                    }
                }
            }
        });
        threadData.start();
    }

    //数字转IP地址
    private String intToIP(int i) {
        return  (i & 0xFF ) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF) + "." +
                ( i >> 24 & 0xFF) ;
    }

    //绑定组件
    private void bindView() {
        etIp = this.findViewById(R.id.et_ip);
        etPort = this.findViewById(R.id.et_port);
        etSend = this.findViewById(R.id.et_send);
        etReceive = this.findViewById(R.id.et_receive);
        btnSend = this.findViewById(R.id.btn_send);
        btnConnect = this.findViewById(R.id.btn_connect);
        btnDisconnect = this.findViewById(R.id.btn_disconnect);
        tvstate = this.findViewById(R.id.tv_state);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isThreadRunning = false;
        if (mUDPSocket != null) {
            mUDPSocket.stopUDPSocket();
        }
        if (threadData != null) {
            threadData.interrupt();
        }
    }

    private void dataProcess(byte[] data) {
        state.clear();
        int n = data.length;
        StringBuilder stringBuilder = new StringBuilder();
        if (n >= 64) {
            for (int i = 0; i < 16; i++ ) {
                double temp = decodeData(new byte[]{data[i * 4], data[i * 4 + 1], data[i * 4 + 2], data[i * 4 + 3]});
                state.add(temp);

                stringBuilder.append(temp);
                stringBuilder.append(',');
            }
        }
        Log.d(TAG,stringBuilder.toString());

    }

    //byte数组相加
    private static byte[] byteMergerAll(byte[]... values) {
        int length_byte = 0;
        for (int i = 0; i < values.length; i++) {
            length_byte += values[i].length;
        }
        byte[] all_byte = new byte[length_byte];
        int countLength = 0;
        for (int i = 0; i < values.length; i++) {
            byte[] b = values[i];
            System.arraycopy(b, 0, all_byte, countLength, b.length);
            countLength += b.length;
        }
        return all_byte;
    }

    //发送数据
    private void sendData()
    {
        if (mUDPSocket != null) {
            mUDPSocket.sendMessage(encodeData(1));
        }else {
            Log.d(TAG,"no udp connect");
        }
    }

    /**
     * 数据解码
     * @param uint8_msg
     * @return
     */
    private double decodeData(byte[] uint8_msg) {
        int n = uint8_msg.length;
        if (n != 4) {
            Log.d(TAG,"error");
        }
        float res = 0;
        for (int i = 0; i < 4; i++) {
            res += (uint8_msg[i] & 0xff) * Math.pow(256,i);
        }
        return res/10000.0 -10000;
    }

    /**
     * 数据编码
     * @param float_msg
     * @return
     */
    private byte[] encodeData(float float_msg) {
        int val = (int) (float_msg * 1000);

        return new byte[]{
                (byte)(val & 0xff),
                (byte)((val>>8) & 0xff),
                (byte)((val>>16) & 0xff),
                (byte)((val>>24) & 0xff)
        };

    }

    /**
     * GPSX坐标转换为经度
     * @param GPSX
     * @return
     */
    private double GPSX2GPSLong(double GPSX) {
        double re = r / temp;
        double reh = (re + Loctn_GPS_IniHeight) * Math.cos(Loctn_GPS_IniLat * pi / 180);
        double LoctnFactorX = reh * pi / 180;
        return GPSX / LoctnFactorX + Loctn_GPS_IniLong;
    }

    /**
     * GPSY坐标转换为纬度
     * @param GPSY
     * @return
     */
    private double GPSY2GPSLat(double GPSY) {
        double temp3 = Math.pow(temp,3);
        double rn = r * (1 - e2) / temp3;
        double rnh = rn + Loctn_GPS_IniHeight;
        double LoctnFactorY = rnh * pi / 180;
        return GPSY / LoctnFactorY + Loctn_GPS_IniLat;
    }

}
