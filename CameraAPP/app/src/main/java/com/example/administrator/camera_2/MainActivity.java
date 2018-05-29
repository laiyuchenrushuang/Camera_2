package com.example.administrator.camera_2;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.camera_2.utils.ActionUtils;
import com.example.administrator.camera_2.utils.VCMAlgo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {

    private static final String TAG = "MainActivity";

    private static final int STATUS_OK = 0;
    private static final int STATUS_GOBACK = 1;
    private static final int STATUS_FAIL = 2;
    private static final int STATUS_DONE = 3;
    private static final int STATUS_RETEST = 4;
    private static final int STATUS_DISTANCE_FAIL = 5;
    private static final int RESULT_INFO = 6;

    public static int[] mDataArray = new int[8];
    private static final String CAPTURE_STEP = "afeng-pos";
    private static final String KEY_FOCUS_MODE = "focus-mode";
    private static final String FOCUS_MODE_MANUAL = "manual";

    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private Camera.Parameters mParameters;

    private Button mTakePicture;
    private TextView mTextContextView;
    private EditText mEditMin;
    private EditText mEditMax;
    private EditText mEditStep;
    private EditText mEditPW;
    private EditText mEditPH;
    private EditText mEditBM;
    private EditText mEditGD;
    private EditText mEditGBD;
    private Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplication().getBaseContext();
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        permision();
        init();
        new LPThread().start();
    }

    private void permision() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i("TEST", "Granted");
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);//1 can be another integer
        }

        int permission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, ActionUtils.PERMISSIONS_STORAGE,
                    ActionUtils.REQUEST_EXTERNAL_STORAGE);
        }
    }

    private void init() {
        //第二栏的显示的数据列表
        {
            mEditMin = findViewById(R.id.min);//min
            mEditMax = findViewById(R.id.max);//max
            mEditStep = findViewById(R.id.step);//step
            mEditPW = findViewById(R.id.picture_width);//picture width
            mEditPH = findViewById(R.id.picture_height);//picture height
            mEditBM = findViewById(R.id.back_max);//圆心距离大于50就开始返程
            mEditGD = findViewById(R.id.go_dif);//前后两个相邻圆心距离
            mEditGBD = findViewById(R.id.go_and_back_dif);//来回同一个点的圆心距离相差
        }

        mTextContextView = findViewById(R.id.text_content);
        mSurfaceView = findViewById(R.id.camera_surface);
        mTakePicture = findViewById(R.id.camera_take_picture);
        mHolder = mSurfaceView.getHolder();

        mHolder.addCallback(this);
        mTakePicture.setOnClickListener(this);
        //mEditPW.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null) {
            mCamera = getCamera();
            if (mHolder != null) {
                setStartPreview(mCamera, mHolder);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void setStartPreview(Camera camera, SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
            camera.setDisplayOrientation(90);
            camera.startPreview();
            camera.cancelAutoFocus();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private Camera getCamera() {
        Camera camera;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            camera = null;
        }
        Log.d(TAG, "camera1 " + camera);
        return camera;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }


    private int mCaptureTime = 0;
    private int mPostionValue;
    private boolean front = true;

    private void capture() {
        testTime++;//每次变化 去更新ui（count step distance）
        //相机的参数设置
        mParameters = mCamera.getParameters();
        mParameters.setPictureFormat(ImageFormat.JPEG);
        mParameters.setPictureSize(getPictureWidth(), getPictureHight());
        //mParameters.set(KEY_FOCUS_MODE, FOCUS_MODE_MANUAL);
        mParameters.set(CAPTURE_STEP, mPostionValue);

        mCamera.setParameters(mParameters);
        //连续拍照需要延迟 不然会crash，底层驱动需要耗时
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mCamera.takePicture(null, null, mPictureCallback);
    }

    private int testTime = 0;
    private int testTimeBiaoJi = 0;

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            //储存照片的线程
//            TeskImage tk = new TeskImage(data,mPostionValue);
//            tk.execute();
            Log.d(TAG, "testTimeBiaoJi= " + testTimeBiaoJi + " " + testTime);
            if (testTimeBiaoJi != testTime) {
                new TextThread().start();
            }

            Log.d(TAG, "mCaptureTime = " + mCaptureTime + " front =" + front + " mPostionValue=" + mPostionValue);

            //测量圆心的核心任务
            TestTask testTask = new TestTask(data, mCaptureTime, mPostionValue, front, handler);
            testTask.execute();
            //每次拍照回到预览模式
            mCamera.startPreview();
            //储存10张
//            if (++testTime <10) {
//                capture();
//            }
        }
    };

    private void saveToMobilePhone(byte[] data, int testTime) {
        Log.d(TAG, "onPictureTaken data =  " + data.length);
        File pictureDir = new File("/sdcard/" + testTime + "temp.png");
        if (pictureDir == null) {
            Log.d(TAG,
                    "Error creating media file, check storage permissions!");
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(pictureDir);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        setStartPreview(mCamera, mHolder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        setStartPreview(mCamera, mHolder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        releaseCamera();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.camera_take_picture:
                getTextInfo();
                setViewDisable();
                fixEditViewValue();
                capture();
                break;
            case R.id.picture_width:
                //intentDataChart();
                break;
        }
    }

    private void fixEditViewValue() {
        class MyThread extends Thread {
            @Override
            public void run() {
                super.run();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mEditMin.setText(mDataArray[0]);
                        mEditMax.setText(mDataArray[1]);
                        mEditStep.setText(mDataArray[2]);
                    }
                });
            }
        }
    }

    private void setViewDisable() {
        mTakePicture.setEnabled(false);
        mEditMin.setEnabled(false);
        mEditMax.setEnabled(false);
        mEditStep.setEnabled(false);
        mEditPW.setEnabled(false);
        mEditPH.setEnabled(false);
        mEditBM.setEnabled(false);
        mEditGD.setEnabled(false);
        mEditGBD.setEnabled(false);
    }

    private void setViewEnable() {
        mTakePicture.setEnabled(true);
        mEditMin.setEnabled(true);
        mEditMax.setEnabled(true);
        mEditStep.setEnabled(true);
        mEditPW.setEnabled(true);
        mEditPH.setEnabled(true);
        mEditBM.setEnabled(true);
        mEditGD.setEnabled(true);
        mEditGBD.setEnabled(true);
    }

    private void getTextInfo() {
        //纯粹为了好看，简直没用
        if ("".equals(mEditMin.getText().toString()) || "".equals(mEditMax.getText().toString()) || "".equals(mEditStep.getText().toString())) {
            Log.d(TAG, "getTextInfo: laiyu");
            Toast.makeText(mContext, "USE DEFAULT DATA!", Toast.LENGTH_LONG).show();
            mEditMin.setText("150");
            mEditMax.setText("550");
            mEditStep.setText("10");
        }

        int min = Integer.parseInt(mEditMin.getText().toString());
        int max = Integer.parseInt(mEditMax.getText().toString());
        int step = Integer.parseInt(mEditStep.getText().toString());

        int picture_w = Integer.parseInt(mEditPW.getText().toString());
        int picture_h = Integer.parseInt(mEditPH.getText().toString());
        int back_max = Integer.parseInt(mEditBM.getText().toString());
        int go_diff = Integer.parseInt(mEditGD.getText().toString());
        int go_back_diff = Integer.parseInt(mEditGBD.getText().toString());

        Log.d(TAG, "getTextInfo: min =" + min + "  max =" + max + " Step =" + step
                + " " + picture_w + " " + picture_h + " " + back_max + " " + go_diff + " " + go_back_diff);
        //如果除不尽，那么最大值去取相差step整数倍
        if ((max - min) % step != 0) {
            max = ((max - min) / step) * step + min;
        }

        //储存起来 数据
        mDataArray[0] = min;
        mDataArray[1] = max;
        mDataArray[2] = step;
        mDataArray[3] = picture_w;
        mDataArray[4] = picture_h;
        mDataArray[5] = back_max;
        mDataArray[6] = go_diff;
        mDataArray[7] = go_back_diff;

        mPostionValue = mDataArray[0];

    }

    private float oldDist = 1f;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            handleFocusMetering(event, mCamera);
        } else {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = getFingerSpacing(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float newDist = getFingerSpacing(event);
                    if (newDist > oldDist) {
                        handleZoom(true, mCamera);
                    } else if (newDist < oldDist) {
                        handleZoom(false, mCamera);
                    }
                    oldDist = newDist;
                    break;
            }
        }
        return true;
    }

    private void handleZoom(boolean isZoomIn, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        if (params.isZoomSupported()) {
            int maxZoom = params.getMaxZoom();
            int zoom = params.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                Log.e("Camera", "进入放大方法zoom=" + zoom);
                zoom++;
            } else if (zoom > 0) {
                Log.e("Camera", "进入缩小方法zoom=" + zoom);
                zoom--;
            }
            params.setZoom(zoom);
            camera.setParameters(params);
        } else {
            Log.i(TAG, "zoom not supported");
        }
    }

    private static void handleFocusMetering(MotionEvent event, Camera camera) {
        Log.e("Camera", "进入handleFocusMetering");
        Camera.Parameters params = camera.getParameters();
        Camera.Size previewSize = params.getPreviewSize();
        Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f, previewSize);
        Rect meteringRect = calculateTapArea(event.getX(), event.getY(), 1.5f, previewSize);

        camera.cancelAutoFocus();

        if (params.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(new Camera.Area(focusRect, 800));
            params.setFocusAreas(focusAreas);
        } else {
            Log.i(TAG, "focus areas not supported");
        }
        if (params.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> meteringAreas = new ArrayList<>();
            meteringAreas.add(new Camera.Area(meteringRect, 800));
            params.setMeteringAreas(meteringAreas);
        } else {
            Log.i(TAG, "metering areas not supported");
        }
        final String currentFocusMode = params.getFocusMode();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        camera.setParameters(params);

        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                Camera.Parameters params = camera.getParameters();
                params.setFocusMode(currentFocusMode);
                camera.setParameters(params);
            }
        });
    }

    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        Log.e("Camera", "getFingerSpacing ，计算距离 = " + (float) Math.sqrt(x * x + y * y));
        return (float) Math.sqrt(x * x + y * y);
    }

    private static Rect calculateTapArea(float x, float y, float coefficient, Camera.Size previewSize) {
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) (x / previewSize.width - 1000);
        int centerY = (int) (y / previewSize.height - 1000);

        int left = clamp(centerX - areaSize / 2, -1000, 1000);
        int top = clamp(centerY - areaSize / 2, -1000, 1000);

        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);

        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private int mDistance;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case STATUS_OK:
                    Log.d(TAG, "handleMessage: MAX" + mDataArray[1] + " STEP" + mDataArray[2]);
                    if (front && mPostionValue == mDataArray[1]) {
                        front = false;
                    } else {
                        if (front) {
                            mPostionValue = mPostionValue + mDataArray[2];//
                            mCaptureTime++;
                        } else {
                            mPostionValue = mPostionValue - mDataArray[2];//back
                            mCaptureTime--;
                        }
                    }
                    capture();
                    break;
                case STATUS_FAIL:
                    printDisTanceLog();
                    showDialogInfo(STATUS_FAIL);
                    setViewEnable();
                    openCameraShine();
                    //intentDataChart();
                    break;
                case STATUS_GOBACK:
                    front = false;
                    capture();
                    break;
                case STATUS_DONE:
                    printDisTanceLog();
                    showDialogInfo(STATUS_DONE);
                    setViewEnable();
                    openCameraShine();
                    //intentDataChart();
                    break;
                case STATUS_RETEST:
                    showDialogInfo(STATUS_RETEST);
                    setViewEnable();
                    openCameraShine();
                    break;
                case STATUS_DISTANCE_FAIL:
                    printDisTanceLog();
                    mDistance = msg.arg1;
                    showDialogInfo(STATUS_DISTANCE_FAIL);
                    setViewEnable();
                    openCameraShine();
                    //intentDataChart();
                    break;
                case RESULT_INFO:
                    mTextContextView.setTextColor(Color.BLUE);
                    mTextContextView.setText("Count =" + VCMAlgo.mData[0] + " Positon =" + VCMAlgo.mData[1] + " dis = " + VCMAlgo.mData[2]);
            }
        }
    };

    private void printDisTanceLog() {
        int[][] data = VCMAlgo.dotList;
        for (int index = 0; index < 1024; index++) {
            if (data[0][index] == 0) {
                break;
            }
            Log.d("printDisTanceLog", " " + data[0][index] + " " + data[1][index] + " " + data[2][index]);
        }
    }

    public static int getPositionMin() {
        return mDataArray[0];
    }

    public static int getPositionMax() {
        return mDataArray[1];
    }

    public static int getPositionStep() {
        return mDataArray[2];
    }

    public static int getPictureWidth() {
        return mDataArray[3];
    }

    public static int getPictureHight() {
        return mDataArray[4];
    }

    public static int getBackMaxDistance() {
        return mDataArray[5];
    }

    public static int getGoDifference() {
        return mDataArray[6];
    }

    public static int getGoBackDifference() {
        return mDataArray[7];
    }

    private void showDialogInfo(int statusDone) {

        if (statusDone == STATUS_DONE) {
            String dialogMessage = "测试OK";
            sendDialogInfo(dialogMessage);
            //intentDataChart();
        } else if (statusDone == STATUS_FAIL) {
            String dialogMessage = "测试FAIL";
            sendDialogInfo(dialogMessage);
        } else if (statusDone == STATUS_RETEST) {
            String dialogMessage = "测试位置不对，调整位置再测";
            sendDialogInfo(dialogMessage);
        } else if (statusDone == STATUS_DISTANCE_FAIL) {
            String dialogMessage = "测试FAIL  Dmax - Dmin =" + mDistance;
            sendDialogInfo(dialogMessage);
        }
    }

    //把data数据构建一个折线图
//    private void intentDataChart() {
//        Intent intent = new Intent();
//        intent.setClass(this, ChartActivity.class);
//        startActivity(intent);
//    }

    private void sendDialogInfo(String dialogMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("测试结果");
        builder.setMessage(dialogMessage);
        builder.setNegativeButton("取消", null).show();
    }

//    private class TeskImage extends AsyncTask<Void,Void,Integer>{
//        private byte[] mData;
//        private int mTime;
//        TeskImage (byte[] data,int time){
//            mData =data;
//            mTime= time;
//        }
//        @Override
//        protected Integer doInBackground(Void... voids) {
//            mPostionValue+=10;
//            saveToMobilePhone(mData,mTime);
//            return null;
//        }
//    }

    //refresh Textview UI
    private class TextThread extends Thread {
        @Override
        public void run() {
            try {
                Message msg = new Message();
                msg.what = RESULT_INFO;
                handler.sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class LPThread  extends Thread{
        @Override
        public void run() {
            super.run();
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("-------设定要指定任务--------");
                            getTextInfo();
                            setViewDisable();
                            fixEditViewValue();
                            capture();
                        }
                    });
                }
            }, 5000);// 设定指定的时间time,此处为2000毫秒
        }
    }

    public void openCameraShine(){
        Log.d(TAG, "openCameraShine: ");
        mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);//开启
        mCamera.setParameters(mParameters);

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);//关闭
                mCamera.setParameters(mParameters);
            }
        });


    }
}
