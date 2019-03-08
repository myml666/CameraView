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
    private TakePictureCallBack takePictureCallBack;//拍照的回调函数
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
        //获取屏幕分辨率
        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        screenWidth = dm.heightPixels;
        screenHeight = dm.widthPixels;
        isSupportAutoFocus = getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_AUTOFOCUS);
        getHolder().addCallback(this);
    }

    public void setTakePictureCallBack(TakePictureCallBack takePictureCallBack) {
        this.takePictureCallBack = takePictureCallBack;
    }

    public boolean isPreviewing() {
        return isPreviewing;
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
                        Bitmap bitmap = cutImage(data);
                        boolean success = false;
                        if (bitmap != null) {
                            success = true;
                        }
                        stopPreview();
                        takePictureCallBack.onPictureTaken(success, bitmap);
                    }
                });
            }
        });
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            if(mCamera!=null&&isPreviewing){
                if(isSupportAutoFocus){
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {

                        }
                    });
                }
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
        int h = bitmap.getWidth();
        int w = bitmap.getHeight();
        int clipw = w/5*3;//这里根据遮罩的比例进行裁剪
        int cliph = (int) (clipw*1.59f);
        int x = (w - clipw) / 2;
        int y = (h - cliph) / 2;
        return Bitmap.createBitmap(bitmap, y, x,cliph, clipw);
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            releaseCamera();
            openCamera();
            initCamera();
        }catch (Exception e){
            mCamera = null;
        }
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
        parameters.setPreviewSize(screenWidth, screenHeight);//设置预览分辨率
        parameters.setPictureSize(screenWidth, screenHeight);//设置拍照图片的分辨率
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
