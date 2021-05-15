import android.util.Log;

import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;

import java.util.ArrayList;
import java.util.List;


public class Sample {

	public void process(byte[] data, int width, int height) {
		AFT_FSDKEngine engine = new AFT_FSDKEngine();

		// 用来存放检测到的人脸信息列表
		List<AFT_FSDKFace> result = new ArrayList<>();

		//初始化人脸跟踪引擎，使用时请替换申请的APPID和SDKKEY
		AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine("APPID", "SDKKEY", AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
		Log.d("com.arcsoft", "AFT_FSDK_InitialFaceEngine =" + err.getCode());

		//输入的data数据为NV21格式（如Camera里NV21格式的preview数据），其中height不能为奇数，人脸跟踪返回结果保存在result。
		err = engine.AFT_FSDK_FaceFeatureDetect(data, width, height, AFT_FSDKEngine.CP_PAF_NV21, result);
		Log.d("com.arcsoft", "AFT_FSDK_FaceFeatureDetect =" + err.getCode());
		Log.d("com.arcsoft", "Face=" + result.size());
		for (AFT_FSDKFace face : result) {
			Log.d("com.arcsoft", "Face:" + face.toString());
		}

		//销毁人脸跟踪引擎
		err = engine.AFT_FSDK_UninitialFaceEngine();
		Log.d("com.arcsoft", "AFT_FSDK_UninitialFaceEngine =" + err.getCode());
	}
}
