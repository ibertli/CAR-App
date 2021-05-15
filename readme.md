# Hirain V2X Demo（详见GitHub README）
##人脸识别
>SDK设置

1.	参考的demo
https://github.com/asdfqwrasdf/ArcFaceDemo
2.	识别引擎
虹软sdk：ArcFace1.2
3.	前往[官网](https://ai.arcsoft.com.cn)申请ID和SDKKEY
修改 FaceDB.java 下面的对应的值:
```java
public static String appid = "xxxx";
public static String fd_key = "xxxx";
public static String ft_key = "xxxx";
public static String fr_key = "xxxx";
public static String age_key = "xxxx";
public static String gender_key = "xxxx";
```
4.	下载SDK包后，将各包内libs中的*.so拷入src/jniLibs，将*.jar拷入libs。

>人脸注册和识别

1.	点击拍照按钮进行人脸注册，注册界面底部会展示已注册的信息列表，点击列表项，则可以执行删除操作。
2.	点击识别按钮 选择打开前置或者后置的镜头进行检测。

>人脸数据保存

以注册时人名为关键索引，保存在face.txt中。
创建的 name.data 则为实际的数据存储文件，保存了所有特征信息。
同一个名字可以注册多个不同状态角度的人脸，在name.data 中连续保存，占用的数据文件长度为:
N * {4字节(特征数据长度) + 22020字节(特征数据信息)}

##登陆注册
>登陆界面

参考demo：
https://github.com/wenzhihao123/Android-loginsmooth-master

1.  点击左下角注册按钮进行人脸注册界面
2. 点击右下角刷脸登录按钮进行人脸识别登陆

>注册界面

1.  点击恒润LOGO进行人脸注册
2. 用户姓名和密码利用SharedPreference保存在本地
3. 人脸信息保存方式见人脸模式模块部分的描述

##智能驾驶
>高德地图

集成高德地图显示路线
1. 前往[官网](https://lbs.amap.com)申请ID和SDK
2. 在AndroidManifest.xml中配置apikey
```java
<meta-data
            android:name="com.amap.api.v2.apikey"
            android:value="4deb70f361edc04caed17986129cb936"/>
```
3. main\assets目录下保存路线坐标文件
4. 下载SDK包后，将各包内libs中的*.so拷入src/jniLibs，将*.jar拷入libs。
5. 官方技术文档
https://lbs.amap.com/api/android-sdk/gettingstarted

>UDP通信

1. 通过UDPSocket类实现UDP通信
2. UdpActivity中进行连接和数据处理

##语音识别
>百度语音识别

集成百度语音识别模块
1. 前往[官网](http://ai.baidu.com)申请ID和SDK
2. 在AndroidManifest.xml中配置apikey
```java
<meta-data
            android:name="com.baidu.speech.APP_ID"
            android:value="16703078"/>
        <meta-data
            android:name="com.baidu.speech.API_KEY"
            android:value="LOYM3G0KASZWgArQqMqiS8yh"/>
        <meta-data
            android:name="com.baidu.speech.SECRET_KEY"
            android:value="uTLtkVhE4RNB2ozBnggyWALePoGjopi3"/>
        <service 
			android:name="com.baidu.speech.VoiceRecognitionService"android:exported="false"/>
```
3. 下载SDK包后，将各包内libs中的*.so拷入src/jniLibs，将*.jar拷入libs。
4. 官方技术文档
https://ai.baidu.com/docs#/ASR-Android-SDK/top

##二维码
>二维码扫描

使用二维码开源框架-[BGAQRCode-Android](https://github.com/bingoogolapple/BGAQRCode-Android)

QRCode 扫描二维码、扫描条形码、相册获取图片后识别、生成带 Logo 二维码、支持微博微信、QQ 二维码扫描样式。对Zbar和Zxing都做了优化，扫描速度非常快，用户体验很好，用起来也非常方便

##蓝牙
>低功耗蓝牙

1. BLEActivity为低功耗蓝牙
2. 使用第三方库BluetoothKit
3. 参考文章
https://www.jianshu.com/p/6dca236f6ad5


