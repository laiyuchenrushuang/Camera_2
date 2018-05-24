package com.example.admin.cameraapp.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by admin on 2018/05/21   .
 */

public class BitmapUtils {
    private static final String TAG = "BitmapUtils";

    public static int[]  getGreenByBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;
        int pixels[] = new int[size];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int[]data = getGreen(pixels);
        return data;
    }

    private static int[] getGreen(int color[]) {
        if (color == null) {
            return null;
        }

        int[] data = new int[color.length];
        for(int i = 0; i < color.length; i++) {
            data[i] = Color.green(color[i]);
        }
        return data;
    }

    public static int[]  getRGBByBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;
        int pixels[] = new int[size];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int[]data = convertColorToByte(pixels);
        return data;
    }

    private static int[] convertColorToByte(int color[]) {
        if (color == null) {
            return null;
        }

        int[] data = new int[color.length * 3];
        for(int i = 0; i < color.length; i++) {
            data[i * 3] = Color.red(color[i]);
            data[i * 3 + 1] = Color.green(color[i]);
            data[i * 3 + 2] =  Color.blue(color[i]);
        }
        return data;
    }

    public static Bitmap zoomImg(Bitmap bm, int newWidth ,int newHeight){
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleWidth);

        Bitmap zoomBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return zoomBitmap;
    }

    //test
    public static void saveBitmap(Bitmap bm,String fileName) {
        File f = new File(Environment.getExternalStorageDirectory()+"/Android/"+fileName+".jpg");
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            Log.d(TAG, "saveBitmap Done");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
