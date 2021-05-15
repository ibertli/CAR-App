package com.example.face.Map;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.*;
import com.amap.api.maps.model.*;
import com.example.face.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements LocationSource,AMapLocationListener, View.OnClickListener {
    private static final String TAG = "UDPSocket";

    private MapView mMapView;
    private AMap aMap;
    private UiSettings mUiSettings;
    private AMapLocationClient mAMapLocationClient;
    private AMapLocationClientOption mAMapLocationClientOption;
    private AMapLocationListener mAMapLocationListener;
    private Polyline mPolyline;

    private double[] GPSLat = new double[120];
    private double[] GPSLng = new double[120];
    private String route;
    private List<LatLng> points = new ArrayList<LatLng>();//经纬度数组

    private final Handler mHandler = new Handler();
    private Runnable udpRunnable;
    //以下是“智能驾驶”界面按钮
    private Button btnConfirm;
    private Button btnSlow;
    private Button btnStop;
    private Button btnStart;
    private Button btnRoute1;
    private Button btnRoute2;
    private Button btnRoute3;
    private Button btnCity;         //新增城市按钮
    private TextView tvSpeed;
    private TextView tvBarrier;
    private TextView numDriveState;
    private TextView numAvoid;
    private TextView numLocation;
    private TextView numGPS;
    private TextView numActuator;
    private TextView numSensor;

    private Thread threadData;
    private UDPSocket mUDPSocket;
    private Boolean isThreadRunning = false;

    //发送和接收的数据格式
    private String state_dic = "[{'self_route':0,'self_lat':39,'self_lon':116,'self_yaw':-1,'self_speed':4,'self_adu':0,'self_location':0,'self_confirm':0}]";
    private String msg_dic = "[{'route_id': 0,'confirm': 0,'adu_confirm': 0}]";
    private List<Double> state = new ArrayList<Double>();
    private Integer[] send_state = new Integer[]{0,0,0,0,0,0}; //数组长度加1，添加城市按钮
    private String sendMessage;
    private Marker mMarker;
    private LatLng mLatLng;

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
        setContentView(R.layout.activity1_map);

        bindView();

        //开启UDPSocket
        if (mUDPSocket == null) {
            mUDPSocket = new UDPSocket(this);
            mUDPSocket.startUDPSocket();
            isThreadRunning = true;
            startDataThread();
            Toast.makeText(this, "UDP启动", Toast.LENGTH_SHORT).show();
        }

        //UI更新数据显示
        udpRunnable = new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        };

        //地图初始化
        mMapView = (MapView) findViewById(R.id.map_view);
        mMapView.onCreate(savedInstanceState);
        aMap = mMapView.getMap();//获得地图
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(31.944389236472063,118.90007793796346),16));


    }

    private void bindView() {
        btnRoute1 = this.findViewById(R.id.buttonRoute1);
        btnRoute1.setOnClickListener(this);
        btnRoute2 = this.findViewById(R.id.buttonRoute2);
        btnRoute2.setOnClickListener(this);
        btnRoute3 = this.findViewById(R.id.buttonRoute3);
        btnRoute3.setOnClickListener(this);

        btnConfirm = this.findViewById(R.id.buttonConfirm);
        btnSlow = this.findViewById(R.id.buttonSlowStop);
        btnStop = this.findViewById(R.id.buttonEmergencyStop);
        btnStart = this.findViewById(R.id.buttonStart);
        btnCity = this.findViewById(R.id.buttonCity);           //
        tvSpeed = this.findViewById(R.id.tvSpeed);
        tvBarrier = this.findViewById(R.id.tvBarrier);
        numDriveState = this.findViewById(R.id.numDriveState);
        numAvoid = this.findViewById(R.id.numAvoid);
        numLocation = this.findViewById(R.id.numLocation);
        numGPS = this.findViewById(R.id.numGPS);
        numActuator = this.findViewById(R.id.numActuator);
        numSensor = this.findViewById(R.id.numSensor);

        btnConfirm.setOnClickListener(this);
        btnSlow.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnStart.setOnClickListener(this);
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
//                        etReceive.setText(mUDPSocket.getReceiveData());
//                        if (TextUtils.isDigitsOnly(mUDPSocket.getReceiveData())) {
//                            etReceive.setText(encodeData(Float.parseFloat(mUDPSocket.getReceiveData())).toString());
//                        }
                        Log.d(TAG,mUDPSocket.getReceiveData());
                        mHandler.post(udpRunnable);
                    }
                }
            }
        });
        threadData.start();
    }

    //更新UI显示接收到的数据
    private void updateUI() {
        dataProcess(mUDPSocket.getReceiveByte());
        if (!state.isEmpty())
        {
            //更新状态
            DecimalFormat df = new DecimalFormat("#.00");
            tvSpeed.setText(String.valueOf(state.get(4)));
            numDriveState.setText(String.valueOf((state.get(5))));
            numLocation.setText(String.valueOf((state.get(9).intValue())));
            numGPS.setText(String.valueOf((state.get(10).intValue())));
            numActuator.setText(String.valueOf((state.get(11).intValue())));
            numSensor.setText(String.valueOf((state.get(12).intValue())));
            numAvoid.setText(String.valueOf((state.get(13)-state.get(14))*2));
            tvBarrier.setText(String.valueOf(df.format(state.get(15))));

            //绘制实时坐标
            if (mMarker != null)
                mMarker.remove();
            mLatLng = new LatLng(GPSY2GPSLat(state.get(2) - 240), GPSX2GPSLong(state.get(1) + 480));
            mMarker = aMap.addMarker(new MarkerOptions().position(mLatLng));
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mLatLng,16));
        }
    }

    private void mapInit() {
        //        locationPoint();

//        //初始化定位
//        mAMapLocationClient = new AMapLocationClient(getApplicationContext());
//        //初始化AMapLocationClientOption对象
//        mAMapLocationClientOption = new AMapLocationClientOption();
//        //监听回调
//        mAMapLocationListener = new AMapLocationListener() {
//            @Override
//            public void onLocationChanged(AMapLocation aMapLocation) {
//
//            }
//        };
//        //设置定位模式为高精度模式。
//        mAMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
//        //设置定位回调监听
//        mAMapLocationClient.setLocationListener(mAMapLocationListener);
//        //获取一次定位结果
//        mAMapLocationClientOption.setOnceLocation(true);
//        //设置是否返回地址信息（默认返回地址信息）
//        mAMapLocationClientOption.setNeedAddress(true);
//        //给定位客户端对象设置定位参数
//        mAMapLocationClient.setLocationOption(mAMapLocationClientOption);
//        //启动定位
//        mAMapLocationClient.startLocation();
    }

    private void locationPoint() {
        MyLocationStyle myLocationStyle = new MyLocationStyle();//初始化定位
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//设定定位风格
        myLocationStyle.interval(2000);//连续定位时间
        aMap.setMyLocationStyle(myLocationStyle);//设置定位类型
        aMap.setMyLocationEnabled(true);//开启定位

        //设置地图控件
        mUiSettings = aMap.getUiSettings();
        mUiSettings.setCompassEnabled(true);//显示指南针
        mUiSettings.setMyLocationButtonEnabled(true);//显示定位按钮
    }

    //读取Json格式数据 读地图坐标
    private void loadJson(String fileName) throws JSONException {
        StringBuilder stringBuilder = new StringBuilder();
        AssetManager assetManager = this.getAssets();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open(fileName)));
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = new JSONObject(stringBuilder.toString());
        JSONArray jsonArray = jsonObject.getJSONArray("points");
        route = jsonObject.getString("route_id");
        points.clear();
        for (int i=1; i< jsonArray.length(); i++) {
            JSONObject jsonObjectPoint = jsonArray.getJSONObject(i);
            GPSLat[i] = Double.parseDouble(jsonObjectPoint.getString("latitude"));
            GPSLng[i] = Double.parseDouble(jsonObjectPoint.getString("longitude"));
            points.add(new LatLng(GPSLat[i],GPSLng[i]));
        }
    }

    //路线绘制
    private void setUpMap(List<LatLng> list) {
        if (mPolyline != null) {
            mPolyline.remove();
        }
        if (list.size()>1) {
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(31.944389236472063,118.90007793796346),16));
            aMap.setMapTextZIndex(2);
            mPolyline = aMap.addPolyline(new PolylineOptions()
                    .addAll(list)
                    .width(5)
                    .color(Color.GREEN));
            Toast.makeText(this,"添加路线"+route,Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(this,"no path",Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        isThreadRunning = false;
        if (mUDPSocket != null) {
            mUDPSocket.stopUDPSocket();
        }
        if (threadData != null) {
            threadData.interrupt();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {

    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {

    }

    @Override
    public void deactivate() {

    }

    @Override
    public void onClick(View view) {
        //判断点击的是哪条路线按钮
        switch (view.getId()) {
            case R.id.buttonRoute1:
                send_state[0] = 1;
                if (!btnRoute1.isActivated()){
                    try {
                        loadJson("route1.json");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    setUpMap(points);
                    btnRoute1.setActivated(true);
                    btnRoute2.setActivated(false);
                    btnRoute3.setActivated(false);
                }else {
                    btnRoute1.setActivated(false);
                    mPolyline.remove();
                }
                break;
            case R.id.buttonRoute2:

                send_state[0] = 2;
                if (!btnRoute2.isActivated()){
                    try {
                        loadJson("route2.json");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    setUpMap(points);
                    btnRoute1.setActivated(false);
                    btnRoute2.setActivated(true);
                    btnRoute3.setActivated(false);
                }else {
                    btnRoute2.setActivated(false);
                    mPolyline.remove();
                }
                break;
            case R.id.buttonRoute3:
                send_state[0] = 3;
                if (!btnRoute3.isActivated()){
                    try {
                        loadJson("route3.json");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    setUpMap(points);
                    btnRoute1.setActivated(false);
                    btnRoute2.setActivated(false);
                    btnRoute3.setActivated(true);
                }else {
                    btnRoute3.setActivated(false);
                    mPolyline.remove();
                }
                break;
            case R.id.buttonConfirm:
                send_state[1] += 1;
                if (!btnConfirm.isActivated()){
                    btnConfirm.setActivated(true);
                }else {
                    btnConfirm.setActivated(false);
                }
                sendData();
                break;
            case R.id.buttonSlowStop:
                if (send_state[3] == 0)
                {
                    send_state[3] = 1;
                    btnSlow.setActivated(true);
                }else {
                    send_state[3] = 0;
                    btnSlow.setActivated(false);
                }
                sendData();
                break;
            case R.id.buttonEmergencyStop:
                if (send_state[4] == 0)
                {
                    send_state[4] = 1;
                    btnStop.setActivated(true);
                }else {
                    send_state[4] = 0;
                    btnStop.setActivated(false);
                }
                sendData();
                break;
            //城市按钮
            case R.id.buttonCity:
                if (send_state[5] == 0)
                {
                    send_state[5] = 1;
                    btnCity.setActivated(true);
                }else {
                    send_state[5] = 0;
                    btnCity.setActivated(false);
                }
                //sendData();
                break;
            //
            case R.id.buttonStart:
                if (send_state[2] == 0)
                {
                    send_state[2] = 1;
                    btnStart.setActivated(true);
                }else {
                    send_state[2] = 0;
                    btnStart.setActivated(false);
                }
                sendData();
                break;

        }
    }

    //数字转IP地址
    private String intToIP(int i) {
        return  (i & 0xFF ) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF) + "." +
                ( i >> 24 & 0xFF) ;
    }

    /**
     * 接收byte数据并依次解析加入链表state
     * @param data
     */
    private void dataProcess(byte[] data) {
        state.clear();
        int n = data.length;//接收到的数据长度
        StringBuilder stringBuilder = new StringBuilder();
        if (n >= 32) {
            for (int i = 0; i < 16; i++ ) {
                double temp = decodeData(new byte[]{data[i * 4], data[i * 4 + 1], data[i * 4 + 2], data[i * 4 + 3]});

                state.add(temp);

                stringBuilder.append(temp);
                stringBuilder.append(',');

            }
        }
        Log.d(TAG,stringBuilder.toString());
        Log.d(TAG,Integer.toString(n));

    }

    /**
     * 获取byte的实际长度
     * @param bytes
     * @return
     */
    public int getValidLength(byte[] bytes){
        int i = 0;
        if (null == bytes || 0 == bytes.length)
            return i ;
        for (; i < bytes.length; i++) {
            if (bytes[i] == '\0')
                break;
        }
        return i;
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
            mUDPSocket.sendMessage(byteMergerAll(encodeData(send_state[0]), encodeData(send_state[1]),
                    encodeData(send_state[2]), encodeData(send_state[3]), encodeData(send_state[4])));
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
