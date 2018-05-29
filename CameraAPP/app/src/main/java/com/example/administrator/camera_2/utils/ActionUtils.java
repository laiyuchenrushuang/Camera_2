package com.example.administrator.camera_2.utils;

import android.Manifest;

/**
 * Created by admin on 2018/5/18.
 */

public class ActionUtils {

    public static final int REQUEST_EXTERNAL_STORAGE = 1;
    public static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
}
