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
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
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
import com.itfitness.cameraview.utils.CardUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

/**
 * @ProjectName: CameraView
 * @Package: com.itfitness.cameraview.widget
 * @ClassName: CameraView
 * @Description: java类作用描述 ：
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
    public static Camera.Size pictureSize;
    private Camera.Size previewSize;
    private boolean isScanning =false;
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
        isSupportAutoFocus = getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_AUTOFOCUS);
        getHolder().addCallback(this);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    public void setIdentifyCallBack(IdentifyCallBack identifyCallBack) {
        this.identifyCallBack = identifyCallBack;
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
        if(!RegexUtils.isIDCard18(result)){//不是身份证格式
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
        int clipw = w/5*3;
        int cliph = (int) (clipw*1.59f);
        int x = (w - clipw) / 2;
        int y = (h - cliph) / 2;
        return Bitmap.createBitmap(bitmap, y, x,cliph, clipw);
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            screenHeight = getHeight();
            screenWidth = getWidth();
            releaseCamera();
            mCamera = Camera.open();//获取相机
            initCamera();
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
        float percent = calcPreviewPercent();
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        previewSize = getPreviewMaxSize(supportedPreviewSizes, percent);
        // 获取摄像头支持的各种分辨率
        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
        pictureSize = findSizeFromList(supportedPictureSizes, previewSize);
        if (pictureSize == null) {
            pictureSize = getPictureMaxSize(supportedPictureSizes, previewSize);
        }
        screenWidth = previewSize.width;
        screenHeight = previewSize.height;
        // 设置照片分辨率，注意要在摄像头支持的范围内选择
        parameters.setPictureSize(pictureSize.width, pictureSize.height);
        // 设置预浏尺寸，注意要在摄像头支持的范围内选择
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        parameters.setJpegQuality(75);//设置图片质量（科大讯飞需要的图片质量为75以上）
        mCamera.setParameters(parameters);
    }

    /**
     * 获取本控件的长宽比例（结合分辨率等的设置解决图像拉伸问题）
     * @return
     */
    private float calcPreviewPercent() {
        float d = screenHeight;
        return d / screenWidth;
    }

    /**
     * 获取摄像头支持的各种分辨率
     * @param supportedPictureSizes
     * @param size
     * @return
     */
    private Camera.Size findSizeFromList(List<Camera.Size> supportedPictureSizes, Camera.Size size) {
        Camera.Size s = null;
        if (supportedPictureSizes != null && !supportedPictureSizes.isEmpty()) {
            for (Camera.Size su : supportedPictureSizes) {
                if (size.width == su.width && size.height == su.height) {
                    s = su;
                    break;
                }
            }
        }
        return s;
    }

    // 根据摄像头的获取与屏幕分辨率最为接近的一个分辨率
    private Camera.Size getPictureMaxSize(List<Camera.Size> l, Camera.Size size) {
        Camera.Size s = null;
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).width >= size.width && l.get(i).height >= size.width
                    && l.get(i).height != l.get(i).width) {
                if (s == null) {
                    s = l.get(i);
                } else {
                    if (s.height * s.width > l.get(i).width * l.get(i).height) {
                        s = l.get(i);
                    }
                }
            }
        }
        return s;
    }

    // 获取预览的最大分辨率
    private Camera.Size getPreviewMaxSize(List<Camera.Size> l, float j) {
        int idx_best = 0;
        int best_width = 0;
        float best_diff = 100.0f;
        for (int i = 0; i < l.size(); i++) {
            int w = l.get(i).width;
            int h = l.get(i).height;
            if (w * h < screenHeight * screenWidth)
                continue;
            float previewPercent = (float) w / h;
            float diff = Math.abs(previewPercent - j);
            if (diff < best_diff) {
                idx_best = i;
                best_diff = diff;
                best_width = w;
            } else if (diff == best_diff && w > best_width) {
                idx_best = i;
                best_diff = diff;
                best_width = w;
            }
        }
        return l.get(idx_best);
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

    public interface IdentifyCallBack{
        /**
         * 扫描成功回调
         */
        void onIdentifySuccess(String cardNumber);
        void onIdentifyImage(Bitmap bitmap);
    }
}
