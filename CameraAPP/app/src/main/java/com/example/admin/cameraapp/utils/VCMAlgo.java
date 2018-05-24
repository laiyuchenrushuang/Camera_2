package com.example.admin.cameraapp.utils;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import com.example.admin.cameraapp.MainActivity;

import java.util.ArrayList;

import static android.support.constraint.Constraints.TAG;

/**
 * Created by admin on 2018/05/21   .
 */

public class VCMAlgo {
    private static final String TAG = "VCMAlgo";
    private static final float PIX_PERCENT_THRESHOLD=0.2f;
    private static final int MAX_FOCIS_STEP = MainActivity.getPositionMax();

    private static final int SCAN_RETURN_DIS = MainActivity.getBackMaxDistance();//去程圆心距改变量大于此数据后,开始返程   50
    private static final int DIS_STEP_DELTA = MainActivity.getGoDifference();//去程步长差异   10
    private static final int DIS_DELTA = MainActivity.getGoBackDifference();//  //往复差异   5

    public static int[][] dotList = new int[3][1024];
    private static final int STATUS_OK= 0;
    private static final int STATUS_GOBACK = 1;
    private static final int STATUS_FAIL = 2;
    private static final int STATUS_DONE = 3;
    private static final int STATUS_RETEST = 4;
    private static final int STATUS_DISTANCE_FAIL = 5;
    public static float mDistance;
    public static int mTextViewShowDisTance = 0;
    public static int mIndexMax;
    public static int[] mData =new int[3];

    private static int getThreshold(int[] rgb24buf, Rect rect, int nWidth){
        //直方图
        int GrayIndex;
        int[] histogram = new int[256];
        //二值化阈值确定
        for (int row = rect.top; row < rect.bottom; row++) {
            for (int col = rect.left; col < rect.right; col++) {
                GrayIndex = rgb24buf[(row*nWidth+col)]; //Get green channel
                histogram[GrayIndex]++;
            }
        }
        long sum=(rect.bottom-rect.top)*(rect.right-rect.left);
        int sumt=0;
        int threshold=0;
        for(int i=255;i>0;i--) {
            sumt+=histogram[i];
            if(sumt >(float)(sum)*PIX_PERCENT_THRESHOLD){
                threshold=i;
                break;
            }
        }
        threshold = threshold/2;
        return threshold; //二值化，阈值取ROI内20%的最亮像素G值的一半
    }

    private static Point getCenterPoint(int[] rgb24buf, Rect rect, int nWidth, int threshold){
        Log.d(TAG, "getCenterPoint  top ="+rect.top+" left ="+rect.left+ " right ="+rect.right+" bottom ="+rect.bottom);
        for (int row=rect.top; row<rect.bottom; row++)
        {
            for (int col=rect.left; col<rect.right; col++)
            {
                //ROI绿色通道反转二值化
                int idx=(row*nWidth+col);
                if(rgb24buf[idx] >threshold)
                {
                    rgb24buf[idx]=0;
                }
                else
                {
                    rgb24buf[idx]=255;
                }
            }
        }
        //根据二值化图计算ROI中心坐标
        int sum=0;
        long ulX=0;
        long ulY=0;
        Point centerP = new Point(0,0);
        for (int row=rect.top;row<rect.bottom;row++) {
            for (int col=rect.left;col<rect.right;col++) {
                int idx=(row*nWidth+col);
                if(rgb24buf[idx]>threshold) {
                    ulX += col;
                    ulY += row;
                    sum++;
                }
            }
        }
        if(sum > 0) {
            double d = ulX/sum;
            centerP.x = (int)d;
            d = ulY/sum;
            Log.d(TAG, "getCenterPoint: d = "+d +" ulY="+ulY+" sum="+sum);
            centerP.y = (int)d;
        }
        return centerP;
    }

    /*计算2个圆点像素距离*/
    public static Point[] Get2DotDis2(int[] dataBuffer, Rect rectL, Rect rectR, int nWidth, int nHeight){
        int threshold;

        threshold = getThreshold(dataBuffer,rectL,nWidth);
        Point pointL = getCenterPoint(dataBuffer,rectL,nWidth,threshold);
        Log.d(TAG, "Get2DotDis: "+pointL.x + " "+pointL.y);

        threshold = getThreshold(dataBuffer,rectR,nWidth);
        Point pointR = getCenterPoint(dataBuffer,rectR,nWidth,threshold);
        Log.d(TAG, "Get2DotDis: "+pointR.x + " "+pointR.y);
        double distance = Math.sqrt((double) ((pointL.x - pointR.x)*(pointL.x - pointR.x) + (pointL.y - pointR.y)*(pointL.y - pointR.y)));
        Log.d(TAG, "Get2DotDis: distance ="+distance);
        Point[] p = new Point[2];
        p[0] = pointL;
        p[1] = pointR;
        return p;
    }

    private static int meanFilter(int[] dataBuffer,Point point,int nWidth){
        Log.d(TAG, "point.x="+point.x+"point.y="+point.y+ "  nWidth ="+nWidth);
        int ptG=(dataBuffer[((point.y-1)*nWidth+point.x-1)]+
                dataBuffer[((point.y-1)*nWidth+point.x)]+
                dataBuffer[((point.y-1)*nWidth+point.x+1)]+
                dataBuffer[(point.y*nWidth+point.x-1)]+
                dataBuffer[(point.y*nWidth+point.x)]+
                dataBuffer[(point.y*nWidth+point.x+1)]+
                dataBuffer[((point.y+1)*nWidth+point.x-1)]+
                dataBuffer[((point.y+1)*nWidth+point.x)]+
                dataBuffer[((point.y+1)*nWidth+point.x+1)])/9;
        return ptG;
    }
    private static boolean availablePosCheck(int[] dataBuffer,Point pointL,Point pointR,int nWidth,int threshold){
        //3x3 meanfilter
        int ptLG= meanFilter(dataBuffer,pointL,nWidth);
        int ptRG= meanFilter(dataBuffer,pointR,nWidth);

        if(ptLG < threshold){
            //定位左圆点中心异常
            return false;
        }
        if(ptRG<threshold){
            //定位右圆点中心异常
            return false;
        }
        return true;
    }
    private static boolean checkBadPixCountLR(int[] dataBuffer,int x,int y,int z,int nWidth,int threshold){
        int badpixTH=10;
        int badpixcount = 0;
        if (z != 0) {
            z=z-1;
        }
        for(int r=x; r<y; r++) {
            if(dataBuffer[(r*nWidth+z)]>threshold){
                badpixcount++;
            }
        }
        if(badpixcount>badpixTH){
            return false;
        }
        return true;
    }
    private static boolean checkBadPixCountTB(int[] dataBuffer,int x,int y,int z,int nWidth,int threshold){
        int badpixTH=10;
        int badpixcount = 0;
        Log.d(TAG, "checkBadPixCountTB: x="+x+" y="+y+" z="+z+" nWidth="+nWidth);
        if(z != 0){
            z = z -1;
        }
        for(int r=x; r<y; r++) {
            Log.d(TAG, "checkBadPixCountTB: ");
            if(dataBuffer[(z)*nWidth+r]>threshold){
                badpixcount++;
            }
        }
        if(badpixcount>badpixTH){
            return false;
        }
        return true;
    }

    private static boolean ROICheck(int[] dataBuffer, Rect rect,int nWidth,int threshold){
        //左 右 ROI定位框L设定
        boolean resultL = checkBadPixCountLR(dataBuffer,rect.top,rect.bottom,rect.left,nWidth,threshold);
        //左 右 ROI定位框R设定
        boolean resultR = checkBadPixCountLR(dataBuffer,rect.top,rect.bottom,rect.right,nWidth,threshold);
        //左 右 ROI定位框T设定
        boolean resultT = checkBadPixCountTB(dataBuffer,rect.left,rect.right,rect.top,nWidth,threshold);
        //左 右 ROI定位框B设定
        boolean resultB = checkBadPixCountTB(dataBuffer,rect.left,rect.right,rect.bottom,nWidth,threshold);

        boolean result = (resultL && resultR && resultT && resultB);
        return result;
    }

    public static double Get2DotDis(int[] dataBuffer, Rect rectL, Rect rectR, int nWidth, int nHeight){
        int threshold;

        threshold = getThreshold(dataBuffer,rectL,nWidth);
        Point pointL = getCenterPoint(dataBuffer,rectL,nWidth,threshold);
        Log.d(TAG, "Get2DotDis: "+pointL.x + " "+pointL.y);

        threshold = getThreshold(dataBuffer,rectR,nWidth);
        Point pointR = getCenterPoint(dataBuffer,rectR,nWidth,threshold);
        Log.d(TAG, "Get2DotDis: "+pointR.x + " "+pointR.y);

        //黑边框
//        if(!(ROICheck(dataBuffer,rectL,nWidth,threshold)&&
//                ROICheck(dataBuffer,rectR,nWidth,threshold))){
//            return Double.MIN_VALUE;//
//        }

        //圆心
        if(!availablePosCheck(dataBuffer,pointL,pointR,nWidth,threshold)){
            return Double.MIN_VALUE;//
        }
        double distance = Math.sqrt((double) ((pointL.x - pointR.x)*(pointL.x - pointR.x) + (pointL.y - pointR.y)*(pointL.y - pointR.y)));
        Log.d(TAG, "Get2DotDis: distance ="+distance);
        return distance;
    }


    public static int saveDotsDistance(int afPos,boolean front,int index,int[] dataBuffer, Rect rectL, Rect rectR, int nWidth, int nHeight){
        Log.d(TAG, "saveDotsDistance: MAX_FOCIS_STEP ="+MAX_FOCIS_STEP);
        dotList[0][index] = afPos;
        double dis = Get2DotDis(dataBuffer, rectL, rectR, nWidth, nHeight);

        mData[0] = index;
        mData[1] = afPos;
        mData[2] = (int)dis;

        if(dis == Double.MIN_VALUE){
            return STATUS_RETEST;
        }
        if(front){
            dotList[1][index] = (int)(dis + 0.5f);

            if(index >1 && (afPos == MAX_FOCIS_STEP) && (Math.abs(dotList[1][index]-dotList[1][1]) < SCAN_RETURN_DIS)){
                mDistance = Math.abs(dotList[1][index]-dotList[1][0]);
                Log.d(TAG, "saveDotsDistance: dMax -dMin ="+mDistance);
                return STATUS_DISTANCE_FAIL;
            }

            Log.d(TAG, "index = "+index);
            if(index > 1 && Math.abs(dotList[1][index]-dotList[1][index-1])>DIS_STEP_DELTA){ //去程步长差异
                Log.d(TAG, " Math.abs(dotList[1][index]-dotList[1][index-1]) ="+Math.abs(dotList[1][index]-dotList[1][index-1]));
                return STATUS_FAIL;
            }

            if(index >1 && (Math.abs(dotList[1][index]-dotList[1][1])>SCAN_RETURN_DIS)){
                mIndexMax = index;
                return STATUS_GOBACK;
            }
        }else {
            dotList[2][index] = (int)(dis + 0.5f);
        }
        mTextViewShowDisTance = (int) dis;
        return STATUS_OK;
    }

    public static int checkTestResult(int index){

        for(int idx=2; idx<=index; idx++){
            Log.d(TAG, " dotList[1][idx] ="+dotList[1][idx]+" dotList[2][idx]="+dotList[2][idx]+" dotList[2][idx-1]"+dotList[2][idx-1]);
            Log.d(TAG, "Math.abs(dotList[2][idx]-dotList[1][idx]) ="+Math.abs(dotList[2][idx]-dotList[1][idx]));
            Log.d(TAG, "Math.abs(dotList[2][idx]-dotList[2][idx-1] = "+Math.abs(dotList[2][idx]-dotList[2][idx-1]));
            if(dotList[0][idx]==0){
                return STATUS_FAIL;
            }
            if(Math.abs(dotList[2][idx]-dotList[1][idx])>DIS_DELTA){
                return STATUS_FAIL;
            }
            if(Math.abs(dotList[2][idx]-dotList[2][idx-1])>DIS_STEP_DELTA){
                //回程步长差异
                return STATUS_FAIL;
            }
        }
        return STATUS_DONE;
    }
}
