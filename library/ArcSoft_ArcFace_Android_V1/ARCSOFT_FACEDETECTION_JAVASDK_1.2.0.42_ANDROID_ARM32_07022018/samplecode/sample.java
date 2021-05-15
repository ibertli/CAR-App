import android.util.Log;

import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;

import java.util.ArrayList;
import java.util.List;


public class Sample {

	public void process(byte[] data, int width, int height) {
		AFD_FSDKEngine engine = new AFD_FSDKEngine();

		// 用来存放检测到的人脸信息列表
		List<AFD_FSDKFace> result = new ArrayList<AFD_FSDKFace>();

		//初始化人脸检测引擎，使用时请替换申请的APPID和SDKKEY
		AFD_FSDKError err = engine.AFD_FSDK_InitialFaceEngine("APPID","SDKKEY", AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT, 16, 5);
		Log.d("com.arcsoft", "AFD_FSDK_InitialFaceEngine = " + err.getCode());

		//输入的data数据为NV21格式（如Camera里NV21格式的preview数据），其中height不能为奇数，人脸检测返回结果保存在result。
		err = engine.AFD_FSDK_StillImageFaceDetection(data, width, height, AFD_FSDKEngine.CP_PAF_NV21, result);
		Log.d("com.arcsoft", "AFD_FSDK_StillImageFaceDetection =" + err.getCode());
		Log.d("com.arcsoft", "Face=" + result.size());
		for (AFD_FSDKFace face : result) {
			Log.d("com.arcsoft", "Face:" + face.toString());
		}

		//销毁人脸检测引擎
		err = engine.AFD_FSDK_UninitialFaceEngine();
		Log.d("com.arcsoft", "AFD_FSDK_UninitialFaceEngine =" + err.getCode());
	}
}
