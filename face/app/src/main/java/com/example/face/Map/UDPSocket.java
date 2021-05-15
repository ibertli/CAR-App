package com.example.face.Map;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPSocket {
    private static final String TAG = "UDPSocket";

    // 单个CPU线程池大小
    private static final int POOL_SIZE = 5;

    private static final int BUFFER_LENGTH = 1024;
    private byte[] receiveByte = new byte[BUFFER_LENGTH];
    //------------------此处修改UDP网络通信地址----------------------//
    private String BROADCAST_IP = "192.168.188.98"; //服务器IP地址 192.168.0.98(原来的)

    // 端口号，飞鸽协议默认端口2425
    public int CLIENT_PORT = 8002;    //本地监听端口
    public int SERVER_PORT = 8012;    //服务器监听端口

    //------------------UDP网络通信地址修改----------------------//

    private boolean isThreadRunning = false;

    private Context mContext;
    private DatagramSocket client;
    private DatagramPacket receivePacket;
    private String receiveData;
    private boolean receiveFlag = false;

    private long lastReceiveTime = 0;
    private static final long TIME_OUT = 120 * 1000;
    private static final long HEARTBEAT_MESSAGE_DURATION = 10 * 1000;

    private ExecutorService mThreadPool;
    private Thread clientThread;
    private HeartbeatTimer timer;
//    private Users localUser;
//    private Users remoteUser;

    //计算GPS坐标的常量
    private double Loctn_GPS_IniLong = 118.8939929;
    private double Loctn_GPS_IniLat = 31.9496598;
    private double Loctn_GPS_IniHeight = 24.34;
    private double r = 6378137.0;
    private double f = 1/298.257223563;
    private double e2 = f * (2-f);
    private double pi = Math.PI;
    private double temp = Math.sqrt(1 - e2 * Math.pow(Math.sin(Loctn_GPS_IniLat * pi / 180),2));

    public UDPSocket(Context context) {

        this.mContext = context;

        int cpuNumbers = Runtime.getRuntime().availableProcessors();
        // 根据CPU数目初始化线程池
        mThreadPool = Executors.newFixedThreadPool(cpuNumbers * POOL_SIZE);
        // 记录创建对象时的时间
        lastReceiveTime = System.currentTimeMillis();

//        createUser();
    }

    public UDPSocket(Context context, String ip, int client_port, int server_port) {


        this.BROADCAST_IP = ip;
        this.CLIENT_PORT = client_port;
        this.SERVER_PORT = server_port;
        this.mContext = context;

        int cpuNumbers = Runtime.getRuntime().availableProcessors();
        // 根据CPU数目初始化线程池
        mThreadPool = Executors.newFixedThreadPool(cpuNumbers * POOL_SIZE);
        // 记录创建对象时的时间
        lastReceiveTime = System.currentTimeMillis();

//        createUser();
    }

    /**
     * 创建本地用户信息
     */
//    private void createUser() {
//        if (localUser == null) {
//            localUser = new Users();
//        }
//        if (remoteUser == null) {
//            remoteUser = new Users();
//        }
//
//        localUser.setImei(DeviceUtil.getDeviceId(mContext));
//        localUser.setSoftVersion(DeviceUtil.getPackageVersionCode(mContext));
//
//        if (WifiUtil.getInstance(mContext).isWifiApEnabled()) {// 判断当前是否是开启热点方
//            localUser.setIp("192.168.43.1");
//        } else {// 当前是开启 wifi 方
//            localUser.setIp(WifiUtil.getInstance(mContext).getLocalIPAddress());
//            remoteUser.setIp(WifiUtil.getInstance(mContext).getServerIPAddress());
//        }
//    }


    public void startUDPSocket() {
        if (client != null) return;
        try {
            // 表明这个 Socket 在设置的端口上监听数据。
            client = new DatagramSocket(CLIENT_PORT);

            if (receivePacket == null) {
                // 创建接受数据的 packet
                receivePacket = new DatagramPacket(receiveByte, BUFFER_LENGTH);
            }

            startSocketThread();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开启发送数据的线程
     */
    private void startSocketThread() {
        clientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "clientThread is running...");
                receiveMessage();
            }
        });
        isThreadRunning = true;
        clientThread.start();

        //startHeartbeatTimer();
    }

    /**
     * 处理接受到的消息
     */
    private void receiveMessage() {
        while (isThreadRunning) {
            receiveFlag = false;
            try {
                if (client != null) {
                    client.receive(receivePacket);
                }
                lastReceiveTime = System.currentTimeMillis();
                Log.d(TAG, "receive packet success...");
            } catch (IOException e) {
                Log.e(TAG, "UDP数据包接收失败！线程停止");
                stopUDPSocket();
                e.printStackTrace();
                return;
            }

            if (receivePacket == null || receivePacket.getLength() == 0) {
                Log.e(TAG, "无法接收UDP数据或者接收到的UDP数据为空");
                continue;
            }

            String strReceive = new String(receivePacket.getData(), 0, receivePacket.getLength());
            Log.d(TAG, strReceive + " from " + receivePacket.getAddress().getHostAddress() + ":" + receivePacket.getPort());

            receiveData = strReceive;
            receiveFlag = true;
            //解析接收到的 json 信息

            // 每次接收完UDP数据后，重置长度。否则可能会导致下次收到数据包被截断。
            if (receivePacket != null) {
                receivePacket.setLength(BUFFER_LENGTH);
            }
        }
    }

    public void stopUDPSocket() {
        isThreadRunning = false;
        receivePacket = null;
        if (clientThread != null) {
            clientThread.interrupt();
        }
        if (client != null) {
            client.close();
            client = null;
        }
        if (timer != null) {
            timer.exit();
        }
    }

    /**
     * 启动心跳，timer 间隔十秒
     */
    private void startHeartbeatTimer() {
        timer = new HeartbeatTimer();
        timer.setOnScheduleListener(new HeartbeatTimer.OnScheduleListener() {
            @Override
            public void onSchedule() {
                Log.d(TAG, "timer is onSchedule...");
                long duration = System.currentTimeMillis() - lastReceiveTime;
                Log.d(TAG, "duration:" + duration);
                if (duration > TIME_OUT) {//若超过两分钟都没收到我的心跳包，则认为对方不在线。
                    Log.d(TAG, "超时，对方已经下线");
                    // 刷新时间，重新进入下一个心跳周期
                    lastReceiveTime = System.currentTimeMillis();
                } else if (duration > HEARTBEAT_MESSAGE_DURATION) {//若超过十秒他没收到我的心跳包，则重新发一个。
                    String string = "hello,this is a heartbeat message";
                    sendMessage(string);
                }
            }

        });
        timer.startTimer(0, 1000 * 10);
    }

    /**
     * 发送字符串
     *
     * @param message
     */
    public void sendMessage(final String message) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress targetAddress = InetAddress.getByName(BROADCAST_IP);

                    DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), targetAddress, SERVER_PORT);

                    client.send(packet);

                    // 数据发送事件
                    Log.d(TAG, "数据发送成功");

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    /**
     * 发送字节数组
     * @param mes
     */
    public void sendMessage(final byte[] mes) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress targetAddress = InetAddress.getByName(BROADCAST_IP);

                    DatagramPacket packet = new DatagramPacket(mes, mes.length, targetAddress, SERVER_PORT);

                    client.send(packet);

                    // 数据发送事件
                    Log.d(TAG, "数据发送成功");

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public float decodeData(byte[] uint8_msg) {
        int n = uint8_msg.length;
        if (n != 4) {
            Log.d(TAG,"error");
        }
        float res = 0;
        for (int i = 0; i < 4; i++) {
            res += (uint8_msg[i] & 0xff) * Math.pow(256,i);
        }
        return res;
    }

    public byte[] encodeData(float float_msg) {
        int val = (int) (float_msg * 1000);

        return new byte[]{
                (byte)(val & 0xff),
                (byte)((val>>8) & 0xff),
                (byte)((val>>16) & 0xff),
                (byte)((val>>24) & 0xff)
        };
    }

    public double GPSX2GPSLong(double GPSX) {
        double re = r / temp;
        double reh = (re + Loctn_GPS_IniHeight) * Math.cos(Loctn_GPS_IniLat * pi / 180);
        double LoctnFactorX = reh * pi / 180;
        return GPSX / LoctnFactorX + Loctn_GPS_IniLong;
    }

    public double GPSY2GPSLat(double GPSY) {
        double temp3 = Math.pow(temp,3);
        double rn = r * (1 - e2) / temp3;
        double rnh = rn + Loctn_GPS_IniHeight;
        double LoctnFactorY = rnh * pi / 180;
        return GPSY / LoctnFactorY + Loctn_GPS_IniLat;
    }

    public DatagramPacket getReceivePacket() {
        return receivePacket;
    }

    public String getReceiveData() {
        return receiveData;
    }

    public byte[] getReceiveByte() {
        return receiveByte;
    }

    public boolean isReceiveFlag() {
        return receiveFlag;
    }
}
