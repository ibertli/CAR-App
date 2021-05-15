package com.example.face.Detect;
/**
 * 登陆注册界面
 */

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;
import com.arcsoft.facedetection.AFD_FSDKVersion;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.example.face.login.DBHelper;
import com.example.face.R;
import com.example.face.login.KeyboardWatcher;
import com.guo.android_extend.image.ImageConverter;
import com.guo.android_extend.widget.ExtImageView;

import java.util.ArrayList;
import java.util.List;

import static com.example.face.Detect.MainActivity.*;

public class LoginRegisterActivity extends AppCompatActivity implements View.OnClickListener, KeyboardWatcher.SoftKeyboardStateListener {
    private final String TAG = this.getClass().toString();
    private final static int MSG_CODE = 0x1000;
    private final static int MSG_EVENT_REG = 0x1001;
    private final static int MSG_EVENT_NO_FACE = 0x1002;
    private final static int MSG_EVENT_NO_FEATURE = 0x1003;
    private final static int MSG_EVENT_FD_ERROR = 0x1004;
    private final static int MSG_EVENT_FR_ERROR = 0x1005;
    private final static int MSG_EVENT_IMG_ERROR = 0x1006;

    private static final int REQUEST_CODE_IMAGE_CAMERA = 1;
    private static final int REQUEST_CODE_IMAGE_OP = 2;
    private static final int REQUEST_CODE_OP = 3;
    private static final int RESULT_CODE_DETECT = 3;

    private LoginUIHandler mUIHandler;
    private String 		mFilePath = null;
    private Bitmap mBitmap;
    private Rect src = new Rect();
    private Rect dst = new Rect();
    private Thread view;
    private AFR_FSDKFace mAFR_FSDKFace;
    private Bitmap face;

    private EditText mName;
    private EditText mPasswd;
    private Button mRegister;
    private ExtImageView mExtImageView;
    private ImageView mImageView;

    private int screenHeight = 0;//屏幕高度
    private float scale = 0.8f; //logo缩放比例
    private View service, body;
    private KeyboardWatcher keyboardWatcher;

    private DBHelper mDBHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_register);

        mExtImageView = (ExtImageView) findViewById(R.id.regist_imageview);
        mName = (EditText) findViewById(R.id.regist_name);
        mPasswd = (EditText) findViewById(R.id.regist_password);
        mRegister = (Button) findViewById(R.id.btn_register);
        mImageView = (ImageView) findViewById(R.id.back);
        service = findViewById(R.id.service);
        body = findViewById(R.id.body);

        screenHeight = this.getResources().getDisplayMetrics().heightPixels; //获取屏幕高度

        mExtImageView.setOnClickListener(this);
        mRegister.setOnClickListener(this);
        mImageView.setOnClickListener(this);

        mDBHelper = new DBHelper(this,"Data.db",null,1);

        keyboardWatcher = new KeyboardWatcher(findViewById(Window.ID_ANDROID_CONTENT));
        keyboardWatcher.addSoftKeyboardStateListener(this);

        faceHandler();
    }

    private void faceHandler() {
        mUIHandler = new LoginUIHandler();
        view = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mFilePath == null) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mBitmap = Application.decodeImage(mFilePath);
                src.set(0,0,mBitmap.getWidth(),mBitmap.getHeight());

                byte[] data = new byte[mBitmap.getWidth() * mBitmap.getHeight() * 3 / 2];
                try {
                    ImageConverter convert = new ImageConverter();
                    convert.initial(mBitmap.getWidth(), mBitmap.getHeight(), ImageConverter.CP_PAF_NV21);
                    if (convert.convert(mBitmap, data)) {
                        Log.d(TAG, "convert ok!");
                    }
                    convert.destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                    Message reg = Message.obtain();
                    reg.what = MSG_CODE;
                    reg.arg1 = MSG_EVENT_IMG_ERROR;
                    reg.obj = e.getMessage();
                    mUIHandler.sendMessage(reg);
                }

                AFD_FSDKEngine engine = new AFD_FSDKEngine();
                AFD_FSDKVersion version = new AFD_FSDKVersion();
                List<AFD_FSDKFace> result = new ArrayList<AFD_FSDKFace>();
                AFD_FSDKError err = engine.AFD_FSDK_InitialFaceEngine(FaceDB.appid, FaceDB.fd_key, AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT, 16, 5);
                Log.d(TAG, "AFD_FSDK_InitialFaceEngine = " + err.getCode());
                if (err.getCode() != AFD_FSDKError.MOK) {
                    Message reg = Message.obtain();
                    reg.what = MSG_CODE;
                    reg.arg1 = MSG_EVENT_FD_ERROR;
                    reg.arg2 = err.getCode();
                    mUIHandler.sendMessage(reg);
                }
                err = engine.AFD_FSDK_GetVersion(version);
                Log.d(TAG, "AFD_FSDK_GetVersion =" + version.toString() + ", " + err.getCode());
                err  = engine.AFD_FSDK_StillImageFaceDetection(data, mBitmap.getWidth(), mBitmap.getHeight(), AFD_FSDKEngine.CP_PAF_NV21, result);
                Log.d(TAG, "AFD_FSDK_StillImageFaceDetection =" + err.getCode() + "<" + result.size());

                if (!result.isEmpty()) {
                    AFR_FSDKVersion version1 = new AFR_FSDKVersion();
                    AFR_FSDKEngine engine1 = new AFR_FSDKEngine();
                    AFR_FSDKFace result1 = new AFR_FSDKFace();
                    AFR_FSDKError error1 = engine1.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
                    Log.d("com.arcsoft", "AFR_FSDK_InitialEngine = " + error1.getCode());
                    if (error1.getCode() != AFD_FSDKError.MOK) {
                        Message reg = Message.obtain();
                        reg.what = MSG_CODE;
                        reg.arg1 = MSG_EVENT_FR_ERROR;
                        reg.arg2 = error1.getCode();
                        mUIHandler.sendMessage(reg);
                    }
                    error1 = engine1.AFR_FSDK_GetVersion(version1);
                    Log.d("com.arcsoft", "FR=" + version.toString() + "," + error1.getCode()); //(210, 178 - 478, 446), degree = 1　780, 2208 - 1942, 3370
                    error1 = engine1.AFR_FSDK_ExtractFRFeature(data, mBitmap.getWidth(), mBitmap.getHeight(), AFR_FSDKEngine.CP_PAF_NV21, new Rect(result.get(0).getRect()), result.get(0).getDegree(), result1);
                    Log.d("com.arcsoft", "Face=" + result1.getFeatureData()[0] + "," + result1.getFeatureData()[1] + "," + result1.getFeatureData()[2] + "," + error1.getCode());
                    if(error1.getCode() == error1.MOK) {
                        mAFR_FSDKFace = result1.clone();
                        int width = result.get(0).getRect().width();
                        int height = result.get(0).getRect().height();
                        Bitmap face_bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                        Canvas face_canvas = new Canvas(face_bitmap);
                        face_canvas.drawBitmap(mBitmap, result.get(0).getRect(), new Rect(0, 0, width, height), null);
                        Message reg = Message.obtain();
                        reg.what = MSG_CODE;
                        reg.arg1 = MSG_EVENT_REG;
                        reg.obj = face_bitmap;
                        mUIHandler.sendMessage(reg);
                    } else {
                        Message reg = Message.obtain();
                        reg.what = MSG_CODE;
                        reg.arg1 = MSG_EVENT_NO_FEATURE;
                        mUIHandler.sendMessage(reg);
                    }
                    error1 = engine1.AFR_FSDK_UninitialEngine();
                    Log.d("com.arcsoft", "AFR_FSDK_UninitialEngine : " + error1.getCode());
                } else {
                    Message reg = Message.obtain();
                    reg.what = MSG_CODE;
                    reg.arg1 = MSG_EVENT_NO_FACE;
                    mUIHandler.sendMessage(reg);
                }
                err = engine.AFD_FSDK_UninitialFaceEngine();
                Log.d(TAG, "AFD_FSDK_UninitialFaceEngine =" + err.getCode());
            }
        });
        view.start();
    }

    /**
     * 缩小
     *
     * @param view
     */
    public void zoomIn(final View view, float dist) {
        view.setPivotY(view.getHeight());
        view.setPivotX(view.getWidth() / 2);
        AnimatorSet mAnimatorSet = new AnimatorSet();
        ObjectAnimator mAnimatorScaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, scale);
        ObjectAnimator mAnimatorScaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, scale);
        ObjectAnimator mAnimatorTranslateY = ObjectAnimator.ofFloat(view, "translationY", 0.0f, -dist);

        mAnimatorSet.play(mAnimatorTranslateY).with(mAnimatorScaleX).with(mAnimatorScaleY);

        mAnimatorSet.setDuration(300);
        mAnimatorSet.start();

    }

    /**
     * f放大
     *
     * @param view
     */
    public void zoomOut(final View view) {
        if (view.getTranslationY()==0){
            return;
        }
        view.setPivotY(view.getHeight());
        view.setPivotX(view.getWidth() / 2);
        AnimatorSet mAnimatorSet = new AnimatorSet();

        ObjectAnimator mAnimatorScaleX = ObjectAnimator.ofFloat(view, "scaleX", scale, 1.0f);
        ObjectAnimator mAnimatorScaleY = ObjectAnimator.ofFloat(view, "scaleY", scale, 1.0f);
        ObjectAnimator mAnimatorTranslateY = ObjectAnimator.ofFloat(view, "translationY", view.getTranslationY(), 0);

        mAnimatorSet.play(mAnimatorTranslateY).with(mAnimatorScaleX).with(mAnimatorScaleY);
        mAnimatorSet.setDuration(300);
        mAnimatorSet.start();

    }

    class LoginUIHandler extends android.os.Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_CODE) {
                if (msg.arg1 == MSG_EVENT_REG) {
//                    LayoutInflater inflater = LayoutInflater.from(RegisterActivity.this);
//                    View layout = inflater.inflate(R.layout.dialog_register, null);
//                    mEditText = (EditText) layout.findViewById(R.id.editview);
//                    mEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
//                    mExtImageView = (ExtImageView) layout.findViewById(R.id.extimageview);
//                    mExtImageView.setImageBitmap((Bitmap) msg.obj);
//                    final Bitmap face = (Bitmap) msg.obj;
//                    new AlertDialog.Builder(RegisterActivity.this)
//                            .setTitle("请输入注册名字")
//                            .setIcon(android.R.drawable.ic_dialog_info)
//                            .setView(layout)
//                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    ((Application)RegisterActivity.this.getApplicationContext()).mFaceDB.addFace(mEditText.getText().toString(), mAFR_FSDKFace, face);
//                                    mRegisterViewAdapter.notifyDataSetChanged();
//                                    dialog.dismiss();
//                                }
//                            })
//                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    dialog.dismiss();
//                                }
//                            })
//                            .show();
                    mExtImageView.setImageBitmap((Bitmap) msg.obj);
                    face = (Bitmap) msg.obj;

                } else if(msg.arg1 == MSG_EVENT_NO_FEATURE ){
                    Toast.makeText(LoginRegisterActivity.this, "人脸特征无法检测，请换一张图片", Toast.LENGTH_SHORT).show();
                } else if(msg.arg1 == MSG_EVENT_NO_FACE ){
                    Toast.makeText(LoginRegisterActivity.this, "没有检测到人脸，请换一张图片", Toast.LENGTH_SHORT).show();
                } else if(msg.arg1 == MSG_EVENT_FD_ERROR ){
                    Toast.makeText(LoginRegisterActivity.this, "FD初始化失败，错误码：" + msg.arg2, Toast.LENGTH_SHORT).show();
                } else if(msg.arg1 == MSG_EVENT_FR_ERROR){
                    Toast.makeText(LoginRegisterActivity.this, "FR初始化失败，错误码：" + msg.arg2, Toast.LENGTH_SHORT).show();
                } else if(msg.arg1 == MSG_EVENT_IMG_ERROR){
                    Toast.makeText(LoginRegisterActivity.this, "图像格式错误，：" + msg.obj, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.regist_imageview://开始注册人脸
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                ContentValues values = new ContentValues(1);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                ((Application)(this.getApplicationContext())).setCaptureImage(uri);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(intent, REQUEST_CODE_IMAGE_CAMERA);
                break;
            case R.id.btn_register:
                if (mRegister.getText().toString().equals("点击图像拍照")) {
                    Toast.makeText(this, "请点击图像拍摄照片", Toast.LENGTH_SHORT).show();
                }
                else {
                    if (checkIsAlreadyRegister(mName.getText().toString())) {
                        Toast.makeText(this,"用户名已被注册",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        ((Application) LoginRegisterActivity.this.getApplicationContext()).mFaceDB.addFace(mName.getText().toString(), mAFR_FSDKFace, face);
                        registerUserInfo(mName.getText().toString(), mPasswd.getText().toString());
                        Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show();
                        this.finish();
                    }
                }
                break;
            case R.id.back:
                finish();
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_IMAGE_OP && resultCode == RESULT_OK) {
            Uri mPath = data.getData();
            String file = getPath(mPath);
            Bitmap bmp = Application.decodeImage(file);
            if (bmp == null || bmp.getWidth() <= 0 || bmp.getHeight() <= 0 ) {
                Log.e(TAG, "error");
            } else {
                Log.i(TAG, "bmp [" + bmp.getWidth() + "," + bmp.getHeight());
            }
            mFilePath = file;
        } else if (requestCode == REQUEST_CODE_OP) {
            Log.i(TAG, "RESULT =" + resultCode);
            if (data == null) {
                return;
            }
            Bundle bundle = data.getExtras();
            String path = bundle.getString("imagePath");
            Log.i(TAG, "path="+path);
        } else if (requestCode == REQUEST_CODE_IMAGE_CAMERA && resultCode == RESULT_OK) {
            Uri mPath = ((Application)(this.getApplicationContext())).getCaptureImage();
            String file = getPath(mPath);
            Bitmap bmp = Application.decodeImage(file);
            mFilePath = file;
            mRegister.setText("立即注册");
        }

        if (resultCode == RESULT_CODE_DETECT) {
            Toast.makeText(this,"欢迎你："+data.getStringExtra("name"),Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this,MainActivity.class));
        }
    }

    /**
     * @param uri
     * @return
     */
    private String getPath(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(this, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }

                    // TODO handle non-primary volumes
                } else if (isDownloadsDocument(uri)) {

                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    return getDataColumn(this, contentUri, null, null);
                } else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[] {
                            split[1]
                    };

                    return getDataColumn(this, contentUri, selection, selectionArgs);
                }
            }
        }
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor actualimagecursor = this.getContentResolver().query(uri, proj, null, null, null);
        int actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        actualimagecursor.moveToFirst();
        String img_path = actualimagecursor.getString(actual_image_column_index);
        String end = img_path.substring(img_path.length() - 4);
        if (0 != end.compareToIgnoreCase(".jpg") && 0 != end.compareToIgnoreCase(".png")) {
            return null;
        }
        return img_path;
    }

    /**
     * 利用SharedPreferences进行默认登陆设置
     */
    private void saveUserInfo() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserInfo",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username",mName.getText().toString());
        editor.putString("password",mPasswd.getText().toString());
        editor.commit();
    }

    /**
     * 利用sql创建嵌入式数据库进行注册访问
     */
    private void registerUserInfo(String username, String password) {
        SQLiteDatabase sqLiteDatabase = mDBHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username",username);
        values.put("password",password);
        sqLiteDatabase.insert("usertable",null,values);
        sqLiteDatabase.close();
    }

    /**
     * 检验用户名是否已经注册
     */
    private boolean checkIsAlreadyRegister(String value) {
        SQLiteDatabase sqLiteDatabase = mDBHelper.getWritableDatabase();
        String query = "Select * from usertable where username = ?";
        Cursor cursor = sqLiteDatabase.rawQuery(query,new String[]{value});
        if (cursor.getCount() > 0) {
            cursor.close();
            return true;
        }
        cursor.close();
        return false;
    }

    @Override
    public void onSoftKeyboardOpened(int keyboardSize) {
        Log.e("face", "----->show" + keyboardSize);
        int[] location = new int[2];
        body.getLocationOnScreen(location); //获取body在屏幕中的坐标,控件左上角
        int x = location[0];
        int y = location[1];
        Log.e("face","y = "+y+",x="+x);
        int bottom = screenHeight - (y+body.getHeight()) ;
        Log.e("face","bottom = "+bottom);
        Log.e("face","con-h = "+body.getHeight());
        if (keyboardSize > bottom){
            ObjectAnimator mAnimatorTranslateY = ObjectAnimator.ofFloat(body, "translationY", 0.0f, -(keyboardSize - bottom));
            mAnimatorTranslateY.setDuration(300);
            mAnimatorTranslateY.setInterpolator(new AccelerateDecelerateInterpolator());
            mAnimatorTranslateY.start();
            zoomIn(mExtImageView, keyboardSize - bottom);

        }
    }

    @Override
    public void onSoftKeyboardClosed() {
        Log.e("face", "----->hide");
        ObjectAnimator mAnimatorTranslateY = ObjectAnimator.ofFloat(body, "translationY", body.getTranslationY(), 0);
        mAnimatorTranslateY.setDuration(300);
        mAnimatorTranslateY.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimatorTranslateY.start();
        zoomOut(mExtImageView);
    }
}
