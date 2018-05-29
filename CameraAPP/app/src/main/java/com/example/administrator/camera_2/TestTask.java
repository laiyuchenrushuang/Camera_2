package com.example.administrator.camera_2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.administrator.camera_2.utils.BitmapUtils;
import com.example.administrator.camera_2.utils.VCMAlgo;

import static android.support.constraint.Constraints.TAG;

/**
 * Created by admin on 2018/05/21   .
 */

public class TestTask extends AsyncTask<Void, Void, Integer> {
    private final byte[] data;
    private int afPos;
    private int index;
    private boolean front;
    private Handler handler;
    private float mDistance;

    public TestTask(byte[] data, int index, int afPos, boolean front, Handler handler) {
        this.data = data;
        this.afPos = afPos;
        this.index = index;
        this.front = front;
        this.handler = handler;
    }

    @Override
    protected void onPostExecute(Integer statusCode) {
        super.onPostExecute(statusCode);
        Message msg = new Message();
        msg.what = statusCode;
        msg.arg1 = (int) mDistance;
        handler.sendMessage(msg);
    }

    @Override
    protected Integer doInBackground(Void[] voids) {
        //Bitmap bitmap= BitmapFactory.decodeFile("/sdcard/"+afPos+"temp.png");
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length).copy(Bitmap.Config.ARGB_8888, true);
        int[] rgbData = BitmapUtils.getGreenByBitmap(bitmap);
        //为了节约时间，截图技巧
        Rect rectL = new Rect(650, bitmap.getHeight() / 3, bitmap.getWidth() / 3, bitmap.getHeight() * 2 / 3);

        Rect rectR = new Rect(bitmap.getWidth() * 2 / 3, bitmap.getHeight() / 3, bitmap.getWidth() - 650, bitmap.getHeight() * 2 / 3);

        Log.d(TAG, " top =" + rectR.top + " left =" + rectR.left + " right =" + rectR.right + " bottom =" + rectR.bottom);
        int statusCode = VCMAlgo.saveDotsDistance(afPos, front, index, rgbData, rectL, rectR, bitmap.getWidth(), bitmap.getHeight());
        mDistance = VCMAlgo.mDistance;
        Log.d(TAG, "doInBackground: front =" + front + "  afPos =" + afPos + "  index =" + index);
        if (front == false && afPos == MainActivity.getPositionMin()) {
            statusCode = VCMAlgo.checkTestResult(VCMAlgo.mIndexMax);
        }
        return statusCode;
    }
}
