package com.itfitness.cameraview.widget.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

public class TakePictureCameraView extends SurfaceView implements SurfaceHolder.Callback{
    private Camera mCamera;//相机
    private boolean isSupportAutoFocus;//是否支持自动对焦
    private int screenHeight;//屏幕的高度
    private int screenWidth;//屏幕的宽度
    private boolean isPreviewing;//是否在预览
    private int[] mMaskSize;//遮罩的宽高
    private TakePictureCallBack takePictureCallBack;//拍照的回调函数
    public static Camera.Size pictureSize;
    private Camera.Size previewSize;
    public TakePictureCameraView(Context context) {
        super(context);
        init();
    }

    public TakePictureCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TakePictureCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    private void init() {
        isSupportAutoFocus = getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_AUTOFOCUS);
        getHolder().addCallback(this);
    }

    public TakePictureCallBack getTakePictureCallBack() {
        return takePictureCallBack;
    }

    public void setTakePictureCallBack(TakePictureCallBack takePictureCallBack) {
        this.takePictureCallBack = takePictureCallBack;
    }

    public boolean isPreviewing() {
        return isPreviewing;
    }

    public int[] getmMaskSize() {
        return mMaskSize;
    }

    public void setmMaskSize(int[] mMaskSize) {
        this.mMaskSize = mMaskSize;
    }

    public void takePicture(){
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean flag, Camera camera) {
                camera.takePicture(new Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {
                        //提示发出声音
                        if(takePictureCallBack!=null){
                            takePictureCallBack.onShutter();
                        }
                    }
                }, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        Bitmap filepath = savePicture(data);
                        boolean success = false;
                        if (filepath != null) {
                            success = true;
                        }
                        stopPreview();
                        takePictureCallBack.onPictureTaken(success, filepath);
                    }
                });
            }
        });
    }
    /**
     * 裁剪并保存照片
     *
     * @param data
     * @return
     */
    private Bitmap savePicture(byte[] data) {
        Bitmap b = this.cutImage(data);
        return b;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            if(mCamera!=null&&isPreviewing){
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {

                    }
                });
            }
            return true;
        }
        return super.onTouchEvent(event);
    }
    /**
     * 裁剪照片
     *
     * @param data
     * @return
     */
    private Bitmap cutImage(byte[] data) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        //如果没有设置遮罩长宽则布局裁剪
        if(mMaskSize==null){
            return bitmap;
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int x = (w - mMaskSize[1]) / 2;
        int y = (h - mMaskSize[0]) / 2;
        return Bitmap.createBitmap(bitmap, x, y, mMaskSize[1], mMaskSize[0]);
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
            mCamera.startPreview();
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

    public interface TakePictureCallBack{
        /**
         * 用于实现照相声音
         */
        void onShutter();

        /**
         * 拍照的回调
         * @param isSuccess
         * @param filepath
         */
        void onPictureTaken(boolean isSuccess,Bitmap filepath);
    }
}