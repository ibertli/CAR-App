import android.util.Log;

import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;

import java.util.ArrayList;
import java.util.List;


public class Sample {

	public void process(byte[] data, int width, int height) {
		ASAE_FSDKEngine engine = new ASAE_FSDKEngine();

		// 用来存放检测到的人脸信息列表
		List<ASAE_FSDKAge> result = new ArrayList<ASAE_FSDKAge>();
		List<ASAE_FSDKFace> input = new ArrayList<ASAE_FSDKFace>();
		
		//这里人脸框和角度，请根据实际对应图片中的人脸框和角度填写
		input.add(new ASAE_FSDKFace(new Rect(210, 178, 478, 446), ASAE_FSDKEngine.ASAE_FOC_0));

		//初始化人脸检测引擎，使用时请替换申请的APPID和SDKKEY
		ASAE_FSDKError err = engine.ASAE_FSDK_InitAgeEngine("APPID","SDKKEY");
		Log.d("com.arcsoft", "ASAE_FSDK_InitAgeEngine = " + err.getCode());

		//输入的data数据为NV21格式（如Camera里NV21格式的preview数据），其中height不能为奇数，人脸检测返回结果保存在result。
		err = engine.ASAE_FSDK_AgeEstimation_Image(data, width, height, ASAE_FSDKEngine.CP_PAF_NV21, input, result);
		Log.d("com.arcsoft", "ASAE_FSDK_AgeEstimation_Image =" + err.getCode());
		Log.d("com.arcsoft", "Face=" + result.size());
		for (ASAE_FSDKAge age : result) {
			Log.d("com.arcsoft", "Age:" + age.getAge());
		}

		//销毁人脸检测引擎
		err = engine.ASAE_FSDK_UninitAgeEngine();
		Log.d("com.arcsoft", "ASAE_FSDK_UninitAgeEngine =" + err.getCode());
	}
}
