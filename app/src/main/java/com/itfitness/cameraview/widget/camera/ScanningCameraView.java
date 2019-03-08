package com.itfitness.cameraview.widget.camera;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.RegexUtils;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.itfitness.cameraview.MainActicity;
import com.itfitness.cameraview.R;
import com.itfitness.cameraview.utils.CardUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

/**
 * @ProjectName: CameraView
 * @Package: com.itfitness.cameraview.widget
 * @ClassName: CameraView
 * @Description: java类作用描述 ：扫描式身份证号识别
 * @Author: 作者名：lml
 * @CreateDate: 2019/3/1 14:47
 * @UpdateUser: 更新者：
 * @UpdateDate: 2019/3/1 14:47
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */

public class ScanningCameraView extends SurfaceView implements SurfaceHolder.Callback,Camera.PreviewCallback{
    private Camera mCamera;//相机
    private boolean isSupportAutoFocus;//是否支持自动对焦
    private int screenHeight;//屏幕的高度
    private int screenWidth;//屏幕的宽度
    private boolean isPreviewing;//是否在预览
    private IdentifyCallBack identifyCallBack;//扫描成功的回调函数
    private boolean isScanning =false;
    private MediaPlayer mMediaPlayer;
    public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString()
            + "/Tess";
    /**
     * 在DATAPATH中新建这个目录，TessBaseAPI初始化要求必须有这个目录。
     */
    public static final String TESSDATA = DATA_PATH + File.separator + "tessdata";
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                if(identifyCallBack!=null){
                    openMusic();
                    identifyCallBack.onIdentifySuccess((String) msg.obj);
                }
            }else if (msg.what == 1){
                if(identifyCallBack!=null){
                    identifyCallBack.onIdentifyImage((Bitmap) msg.obj);
                }
            }
        }
    };
    public ScanningCameraView(Context context) {
        super(context);
        init();
    }

    public ScanningCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScanningCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    private void init() {
        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        screenWidth = dm.heightPixels;
        screenHeight = dm.widthPixels;
        isSupportAutoFocus = getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_AUTOFOCUS);
        getHolder().addCallback(this);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    public void setIdentifyCallBack(IdentifyCallBack identifyCallBack) {
        this.identifyCallBack = identifyCallBack;
    }

    /**
     * 发出声音
     */
    public  void  openMusic(){
        mMediaPlayer= MediaPlayer.create(getContext(), R.raw.music_saomiao);
        mMediaPlayer.start();
    }
    /**
     * 开灯
     */
    public void openLight(){
        Camera.Parameters parameters = mCamera.getParameters();
        //打开闪光灯
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);//开启
        mCamera.setParameters(parameters);
    }

    /**
     * 关灯
     */
    public void closeLight(){
        Camera.Parameters parameters = mCamera.getParameters();
        //打开闪光灯
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);//开启
        mCamera.setParameters(parameters);
    }
    /**
     * Camera帧数据回调用
     */
    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        camera.addCallbackBuffer(data);
        new Thread(new Runnable() {
            @Override
            public void run() {
                //识别中不处理其他帧数据
                if (!isScanning) {
                    isScanning = true;
                    try {
                        //获取Camera预览尺寸
                        Camera.Size size = camera.getParameters().getPreviewSize();
                        //将帧数据转为bitmap
                        YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                        if (image != null) {
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
                            Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                            Bitmap bitmap = cutImage(bmp);//获取遮罩处图像
                            final Bitmap cardNumberBitmap = CardUtil.clipCardNumber(bitmap);
                            CardUtil.getBinaryImage(cardNumberBitmap);
                            bmp.recycle();
                            bitmap.recycle();
                            Message message = handler.obtainMessage();
                            message.what = 1;
                            message.obj = cardNumberBitmap;
                            handler.sendMessage(message);
                            TessBaseAPI baseApi = new TessBaseAPI();
                            //初始化OCR的字体数据，DATA_PATH为路径，DEFAULT_LANGUAGE指明要用的字体库（不用加后缀）
                            baseApi.init(DATA_PATH, "card");
                            //设置识别模式
                            baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
                            //设置要识别的图片
                            baseApi.setImage(cardNumberBitmap);
                            //开始识别
                            String result = baseApi.getUTF8Text();
                            baseApi.clear();
                            baseApi.end();
                            isQualified(result);
                        }
                    } catch (Exception ex) {
                        LogUtils.eTag("身份证号出错",ex.getMessage());
                        isScanning = false;
                    }
                }
            }
        }
        ).start();

    }

    /**
     * 检验身份证
     * @param result
     */
    private void isQualified(String result) {
        if (TextUtils.isEmpty(result)) {
            isScanning = false;
            return;
        }
        if(!RegexUtils.isIDCard18Exact(result)){//不是身份证格式
            isScanning = false;
        }else{
            isScanning = true;
            Message message = handler.obtainMessage();
            message.what = 0;
            message.obj = result;
            handler.sendMessage(message);
        }
    }
    /**
     * 摄像头自动聚焦
     */
    Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            postDelayed(doAutoFocus, 500);
        }
    };
    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (mCamera != null) {
                try {
                    mCamera.autoFocus(autoFocusCB);
                } catch (Exception e) {
                }
            }
        }
    };
    /**
     * 裁剪照片
     *
     * @return
     */
    private Bitmap cutImage(Bitmap bitmap) {
        int h = bitmap.getWidth();
        int w = bitmap.getHeight();
        int clipw = w/5*3;//这里根据遮罩的比例进行裁剪
        int cliph = (int) (clipw*1.59f);
        int x = (w - clipw) / 2;
        int y = (h - cliph) / 2;
        return Bitmap.createBitmap(bitmap, y, x,cliph, clipw);
    }
    /**
     * 打开指定摄像头
     */
    public void openCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int cameraId = 0; cameraId < Camera.getNumberOfCameras(); cameraId++) {
            Camera.getCameraInfo(cameraId, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                try {
                    mCamera = Camera.open(cameraId);
                } catch (Exception e) {
                    if (mCamera != null) {
                        mCamera.release();
                        mCamera = null;
                    }
                }
                break;
            }
        }
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            releaseCamera();
            openCamera();
        }catch (Exception e){
            mCamera = null;
        }
    }

    /**
     * 加载相机配置
     */
    private void initCamera() {
        try {
            mCamera.setPreviewDisplay(getHolder());//当前控件显示相机数据
            mCamera.setDisplayOrientation(90);//调整预览角度
            setCameraParameters();
            startPreview();//打开相机
        }catch (Exception e){
            releaseCamera();
        }
    }

    /**
     * 配置相机参数
     */
    private void setCameraParameters() {
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        //确定前面定义的预览宽高是camera支持的，不支持取就更大的
        for (int i = 0; i < sizes.size(); i++) {
            if ((sizes.get(i).width >= screenWidth && sizes.get(i).height >= screenHeight) || i == sizes.size() - 1) {
                screenWidth = sizes.get(i).width;
                screenHeight = sizes.get(i).height;
                break;
            }
        }
        //设置最终确定的预览大小
        parameters.setPreviewSize(screenWidth, screenHeight);
        mCamera.setParameters(parameters);
    }
    /**
     * 释放相机
     */
    private void releaseCamera() {
        if(mCamera!=null){
            stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera=null;
        }
    }
    /**
     * 停止预览
     */
    private void stopPreview() {
        if (mCamera != null && isPreviewing) {
            mCamera.stopPreview();
            isPreviewing = false;
        }
    }

    /**
     * 开始预览
     */
    public void startPreview() {
        if (mCamera != null) {
            mCamera.addCallbackBuffer(new byte[((screenWidth * screenHeight) * ImageFormat.getBitsPerPixel(ImageFormat.NV21)) / 8]);
            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.startPreview();
            if(isSupportAutoFocus) {
                mCamera.autoFocus(autoFocusCB);
            }
            isPreviewing = true;
        }
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        stopPreview();
        initCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    /**
     * 释放MediaPlayer
     */
    private void releaseMediaPlayer() {
        if(mMediaPlayer!=null){
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public interface IdentifyCallBack{
        /**
         * 扫描成功回调
         */
        void onIdentifySuccess(String cardNumber);
        void onIdentifyImage(Bitmap bitmap);
    }
}
