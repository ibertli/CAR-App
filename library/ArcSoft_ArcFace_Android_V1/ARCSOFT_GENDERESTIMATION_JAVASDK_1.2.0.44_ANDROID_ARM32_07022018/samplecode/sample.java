import android.util.Log;

import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;

import java.util.ArrayList;
import java.util.List;


public class Sample {

	public void process(byte[] data, int width, int height) {
		ASGE_FSDKEngine engine = new ASGE_FSDKEngine();

		// 用来存放检测到的人脸信息列表
		List<ASGE_FSDKGender> result = new ArrayList<ASGE_FSDKGender>();
		List<ASGE_FSDKFace> input = new ArrayList<ASGE_FSDKFace>();

		//这里人脸框和角度，请根据实际对应图片中的人脸框和角度填写
		input.add(new ASGE_FSDKFace(new Rect(210, 178, 478, 446), ASGE_FSDKEngine.ASGE_FOC_0));

		//初始化人脸检测引擎，使用时请替换申请的APPID和SDKKEY
		ASGE_FSDKError err = engine.ASGE_FSDK_InitgGenderEngine("APPID","SDKKEY");
		Log.d("com.arcsoft", "ASGE_FSDK_InitgGenderEngine = " + err.getCode());

		//输入的data数据为NV21格式（如Camera里NV21格式的preview数据），其中height不能为奇数，人脸检测返回结果保存在result。
		err = engine.ASGE_FSDK_GenderEstimation_Image(data, width, height, ASGE_FSDKEngine.CP_PAF_NV21, input, result);
		Log.d("com.arcsoft", "ASGE_FSDK_GenderEstimation_Image =" + err.getCode());
		Log.d("com.arcsoft", "Face=" + result.size());
		for (ASGE_FSDKGender gender : result) {
			switch(gender.getGender()) {
				case ASGE_FSDKGender.FEMALE : Log.d("com.arcsoft", "gender: FEMALE" ); break;
				case ASGE_FSDKGender.MALE: Log.d("com.arcsoft", "gender: MALE" ); break;
				case ASGE_FSDKGender.UNKNOWN: Log.d("com.arcsoft", "gender: UNKNOWN" ); break;
				default:;
			}
		}

		//销毁人脸检测引擎
		err = engine.ASGE_FSDK_UninitGenderEngine();
		Log.d("com.arcsoft", "ASGE_FSDK_UninitGenderEngine =" + err.getCode());
	}
}
