package guepardoapps.heartratemonitor;

import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicBoolean;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import guepardoapps.heartratemonitor.common.Logger;
import guepardoapps.heartratemonitor.common.constants.Enables;
import guepardoapps.heartratemonitor.common.enums.PixelType;
import guepardoapps.heartratemonitor.controller.PermissionController;
import guepardoapps.heartratemonitor.processing.ImageProcessing;

@SuppressWarnings("deprecation")
public class HeartRateMonitorView extends Activity {
    private static final String TAG = HeartRateMonitorView.class.getSimpleName();
    private static Logger _logger;

    private static final int REQUEST_CAMERA_PERMISSION = 219403;

    private static final AtomicBoolean PROCESSING = new AtomicBoolean(false);

    private static final int AVERAGE_ARRAY_SIZE = 4;
    private static final int[] AVERAGE_ARRAY = new int[AVERAGE_ARRAY_SIZE];

    private static final int BEATS_ARRAY_SIZE = 3;
    private static final int[] BEATS_ARRAY = new int[BEATS_ARRAY_SIZE];

    private static SurfaceHolder _previewHolder = null;
    private static Camera _camera = null;
    private static View _image = null;
    private static TextView _textView = null;

    private static WakeLock _wakeLock = null;

    private static int _averageIndex = 0;

    private static PixelType _currentPixelType = PixelType.GREEN;


    public static PixelType getCurrentPixelType() {
        return _currentPixelType;
    }
    private static int imageSize=800;
    private static int _beatsIndex = 0;
    private static double _beats = 0;
    private static long _startTime = 0;

    public static int isReady = 0;
    public static int[] ecg = new int[1000];




    /*
   构造函数
     */
    public HeartRateMonitorView() throws FileNotFoundException {
    }

    public int getReady(){
        return isReady;
    }
    /*
    获取心跳数组
     */
    public int[] getEcg(){
        return ecg;
    }

    private static PreviewCallback _previewCallback = new PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (data == null) {
                _logger.Error("Data is null!");
                return;
            }

            Camera.Size size = camera.getParameters().getPreviewSize();
            if (size == null) {
                _logger.Error("Size is null!");
                return;
            }

            if (!PROCESSING.compareAndSet(false, true)) {
                _logger.Error("Have to return...");
                return;
            }

            isReady++;
            if(isReady > 1000 )
            {
                isReady = 1;
            }



            int width = size.width;
            int height = size.height;
            int imageSum= ImageProcessing.decodeYUV420SPtoRedSum(data.clone(), width, height);
            int imageAverage = ImageProcessing.decodeYUV420SPtoRedAvg(data.clone(), width, height);
            //ecg[isReady-1]=imageAverage;
            ecg[isReady-1] =imageSum/imageSize;


            if (imageAverage == 0 || imageAverage == 255) {
                PROCESSING.set(false);
                return;
            }

            int averageArrayAverage = 0;
            int averageArrayCount = 0;

            for (int averageEntry : AVERAGE_ARRAY) {
                if (averageEntry > 0) {
                    averageArrayAverage += averageEntry;
                    averageArrayCount++;
                }
            }

            int rollingAverage = (averageArrayCount > 0) ? (averageArrayAverage / averageArrayCount) : 0;         //取连续四帧平均值
            PixelType newType = _currentPixelType;

            if (imageAverage < rollingAverage) {                     //比较本帧R分量与前四帧平均值大小 判断是否有心跳
                newType = PixelType.RED;                            // 心跳图片由红变绿
                if (newType != _currentPixelType) {                   //脉搏波下降 心跳加一
                    _beats++;
                }
            } else if (imageAverage > rollingAverage) {
                newType = PixelType.GREEN;
            }

            if (_averageIndex == AVERAGE_ARRAY_SIZE) {               //数组清零  AVERAGE_ARRAY_SIZE=4;
                _averageIndex = 0;
            }

            AVERAGE_ARRAY[_averageIndex] = imageAverage;        //将本帧数据添加到数组中
            _averageIndex++;

            if (newType != _currentPixelType) {
                _currentPixelType = newType;
                _image.postInvalidate();     //界面刷新  在工作者线程中被调用
            }

            long endTime = System.currentTimeMillis();
            double totalTimeInSecs = (endTime - _startTime) / 1000d;             //心跳_beats下所用时间

            if (totalTimeInSecs >= 15) {            //十秒后计算
                double beatsPerSecond = (_beats / totalTimeInSecs);     //每秒心跳数
                int beatsPerMinute = (int) (beatsPerSecond * 60d);     //每分钟心跳数
                if (beatsPerMinute < 30 || beatsPerMinute > 180) {
                    _startTime = System.currentTimeMillis();         //获取系统当前时间
                    _beats = 0;
                    PROCESSING.set(false);
                    return;
                }

                if (_beatsIndex == BEATS_ARRAY_SIZE) {      //beat数组清零   BEATS_ARRAY_SIZE = 3;
                    _beatsIndex = 0;
                }

                BEATS_ARRAY[_beatsIndex] = beatsPerMinute;
                _beatsIndex++;

                int beatsArrayAverage = 0;
                int beatsArrayCount = 0;

                for (int beatsEntry : BEATS_ARRAY) {
                    if (beatsEntry > 0) {
                        beatsArrayAverage += beatsEntry;
                        beatsArrayCount++;
                    }
                }

                int beatsAverage = (beatsArrayAverage / beatsArrayCount);        //心跳取平均值
                _textView.setText(String.valueOf(beatsAverage));

                _startTime = System.currentTimeMillis();
                _beats = 0;
            }

            PROCESSING.set(false);
        }
    };

    private static SurfaceHolder.Callback _surfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                _camera.setPreviewDisplay(_previewHolder);
                _camera.setPreviewCallback(_previewCallback);
            } catch (Throwable throwable) {
                _logger.Error(throwable.toString());
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Camera.Parameters parameters = _camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);                //设置闪光灯常亮

            Camera.Size size = getSmallestPreviewSize(width, height, parameters);      //设置最小预览尺寸
            if (size != null) {
                parameters.setPreviewSize(size.width, size.height);                   // 设置预览尺寸
                _logger.Debug(String.format("Using width %s and height %s", size.width, size.height));
            }

            _camera.setParameters(parameters);
            _camera.startPreview();                                              //开始预览
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    };

    private static Camera.Size getSmallestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea < resultArea) {
                        result = size;
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_main);

        _logger = new Logger(TAG, Enables.LOGGING);
        _logger.Debug("onCreate");

        new PermissionController(this).CheckPermissions(REQUEST_CAMERA_PERMISSION, Manifest.permission.CAMERA);   //获取摄像头权限

        SurfaceView preview = findViewById(R.id.preview);                  //SurfaceView，是Surface的View，通过SurfaceView可以看到Surface的部分或者全部的内容，
        _previewHolder = preview.getHolder();                           //SurfaceHolder显示一个surface的抽象接口，使你可以控制surface的大小和格式， 以及在surface上编辑像素，和监视surace的改变。
        _previewHolder.addCallback(_surfaceCallback);
        _previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        _image = findViewById(R.id.image);
        _textView = findViewById(R.id.text);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);           //PowerManager 控制电源状态 待机时间
        _wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);                       //FULL_WAKE_LOCK: CPU:on , screen:bright , keyboard:bright

    }

    @Override
    public void onResume() {
        super.onResume();
        _logger.Debug("onResume");

        _wakeLock.acquire();     // 屏幕将停留在设定的状态，一般为亮、暗状态
        _camera = Camera.open();
        _camera.setDisplayOrientation(90);
        _startTime = System.currentTimeMillis();               //获取系统当前时间
    }

    @Override
    public void onPause() {
        super.onPause();
        _logger.Debug("onPause");

        _wakeLock.release();                 //释放掉正在运行的cpu或关闭屏幕。

        _camera.setPreviewCallback(null);
        _camera.stopPreview();
        _camera.release();
        _camera = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        _logger.Debug("onDestroy");
    }


    /*
    响应系统设置更改，当系统设置发生更改时，自动回调
     */
    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        _logger.Debug("onConfigurationChanged");
    }
}
