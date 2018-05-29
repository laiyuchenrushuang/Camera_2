package com.example.administrator.camera_2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.example.administrator.camera_2.utils.VCMAlgo;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;

public class LineChartView extends View {
    private float mChartWidth;
    private float mChartHight;
    private float mChartLenghtSide;
    private static final int STEPSIZE =500;
    private static final  int  DISTANCE =20;
    private ArrayList<Integer> datalist =new ArrayList<>();

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawChart(canvas);
        drawX(canvas);
        drawY(canvas);
        drawLine(canvas);
    }

    private void drawX(Canvas canvas) {
        Paint paint = new Paint();
        paint.setTextSize(50);
        float textsize = paint.getTextSize();
        paint.setStrokeWidth(5);
        float x= 1.0f,y= 1.0f;
        for (int i = 0;i<STEPSIZE/100;i++) {
            canvas.drawText(""+(i+1)*100,mChartLenghtSide/10+mChartLenghtSide/5*i-textsize/2,mChartLenghtSide*3/4+textsize,paint);
        }

    }

    private void drawY(Canvas canvas) {
        Paint paint = new Paint();
        paint.setTextSize(50);
        float textsize = paint.getTextSize();
        paint.setStrokeWidth(5);
        float x= 1.0f,y= 1.0f;
        for (int i = 0;i<= DISTANCE/5;i++) {
            canvas.drawText(""+i*5,mChartLenghtSide/10-textsize,mChartLenghtSide*3/4-((mChartLenghtSide*3/4-mChartLenghtSide/10)/(DISTANCE/5)*i)+textsize/2,paint);
        }
    }

    private void drawLine(Canvas canvas) {
        Log.d(TAG, "drawLine: ");
        Paint paint = new Paint();
        paint.setStrokeWidth(5);
        paint.setColor(Color.RED);
        if (datalist != null) {
            int length = datalist.size();
            Log.d(TAG, "drawLine: mData.length" +length);
            for (int i = 0;i <length-1;i++) {
                canvas.drawLine(mChartLenghtSide/10+i*MainActivity.getPositionStep(),mChartLenghtSide*3/4-datalist.get(i),mChartLenghtSide/10+(i+1)*MainActivity.getPositionStep(),datalist.get(i+1),paint);
            }
//            canvas.drawLine(
//                    mChartLenghtSide/10+(length-1)*MainActivity.getPositionStep(),
//                    mChartLenghtSide*3/4-datalist.get(length-1),
//                    mChartLenghtSide/10+(length)*MainActivity.getPositionStep(),
//                    datalist.get(length),
//                    paint);

        }

    }

    private void drawChart(Canvas canvas) {
        Paint paint = new Paint();
        paint.setStrokeWidth(5);
        canvas.drawLine(mChartLenghtSide/10,mChartLenghtSide/10,mChartLenghtSide/10,mChartLenghtSide*3/4,paint);//竖线
        canvas.drawLine(mChartLenghtSide/10,mChartLenghtSide*3/4,mChartLenghtSide*9/10,mChartLenghtSide*3/4,paint);//横线
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mChartWidth = MeasureSpec.getSize(widthMeasureSpec);
        mChartHight = MeasureSpec.getSize(heightMeasureSpec);

        if (mChartWidth > mChartHight) {
            mChartLenghtSide =mChartHight;
        }else {
            mChartLenghtSide =mChartWidth;
        }

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }



    public LineChartView(Context context) {
        super(context);
        Log.d(TAG, "LineChartView: 1");
    }

    private void init() {
        int [][] dolist = VCMAlgo.getChartData();

        for (int index = 0;index<1024;index ++) {
            if (dolist[0][index] == 0 || dolist[0][index] == MainActivity.getPositionMax()) {
                Log.d(TAG, "init: ");
                break;
            }
            Log.d(TAG, "init: dolist[1][index]"+dolist[1][index]);
            datalist.add(dolist[1][index]);
        }

    }

    public LineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "LineChartView: 2");
        init();
    }

    public LineChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.d(TAG, "LineChartView: 3");
    }
}
