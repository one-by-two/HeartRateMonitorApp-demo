package guepardoapps.heartratemonitor.views;

/**
 * Created by LBW on 2017/10/20.
 */
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;

import java.io.FileNotFoundException;
import java.util.List;

import guepardoapps.heartratemonitor.Factory;
import guepardoapps.heartratemonitor.HeartRateMonitorView;


public class PathView extends CardiographView {

    public int j = 5;
    public int[] ecgPrint;
    public int benchMark;
    private int x=7,y=1;
    private int max,min;
    private int counter = 0;
    private int zeroDot;
    HeartRateMonitorView heartRateMonitorView= Factory.getHeartView();


    public PathView(Context context) throws FileNotFoundException {
        this(context,null);
    }

    public PathView(Context context, AttributeSet attrs) throws FileNotFoundException {
        this(context, attrs,0);
    }

    public PathView(Context context, AttributeSet attrs, int defStyleAttr) throws FileNotFoundException {
        super(context, attrs, defStyleAttr);
        zeroDot = mHeight*2/3;
        mPaint = new Paint();
        mPath = new Path();

    }
    public int computeY(int i){
        int result;
        if (i>benchMark) {
            result = i - benchMark;
            result = mHeight / 2 + result;
        }
        else {
            result = benchMark - i;
            result = mHeight / 2 - result;
        }

        return result;

    }
    public void drawPath(Canvas canvas) {
//        Tab1 tab1 = new Tab1();
        j = heartRateMonitorView.getReady();
        ecgPrint = heartRateMonitorView.getEcg();

        Log.d("xyz", "drawPath1: "+j);

        mPath.reset();

        if(j>1000){j=0;}
        mPath.moveTo(0,mHeight/2);
        benchMark = ecgPrint[4];
        max = ecgPrint[4];
        min = ecgPrint[4];

        for (int i = 5; i<j; i++){
            if(ecgPrint[i]>max)
                max = ecgPrint[i];
            if(ecgPrint[i]<min)
                min = ecgPrint[i];
            if(max-min>1000)
                benchMark = (max+min)/2;
        }

        for (int i = 5; i<j; i++){

            mPath.lineTo(i*x,computeY(ecgPrint[i]));

        }
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mLineColor);
        mPaint.setStrokeWidth(5);
        canvas.drawPath(mPath,mPaint);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d("xyz", "onDraw: ");
//        Tab1 tab1 = new Tab1();
        drawPath(canvas);
        scrollBy(1,0);
        postInvalidateDelayed(10);


    }

}
