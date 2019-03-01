package com.itfitness.cameraview.widget;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * @ProjectName: CameraView
 * @Package: com.itfitness.cameraview.widget
 * @ClassName: CameraSurfaceView
 * @Description: java类作用描述 ：
 * @Author: 作者名：lml
 * @CreateDate: 2019/3/1 15:20
 * @UpdateUser: 更新者：
 * @UpdateDate: 2019/3/1 15:20
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */

public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private String TAG=CameraSurfaceView.class.getSimpleName();
    private Context mContext;
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private int screenHeight;//屏幕的高度
    private int screenWidth;//屏幕的宽度

    /***
     * 是否支持自动对焦
     */
    private boolean isSupportAutoFocus;
    public static Camera.Size pictureSize;
    private Camera.Size previewSize;
    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        mHolder=this.getHolder();
        mHolder.addCallback(this);
    }

    private void init(Context context) {
        mContext = context;

        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        isSupportAutoFocus = context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_AUTOFOCUS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            mCamera = Camera.open();
            if (mCamera == null) {
                return;
            }
            mCamera.setDisplayOrientation(90);
            // 设置holder主要是用于surfaceView的图片的实时预览，以及获取图片等功能，可以理解为控制camera的操作..

            mCamera.setPreviewDisplay(surfaceHolder);
            setCameraParms();
            //mCamera.setPreviewCallback(this);
            mCamera.startPreview();
            mCamera.cancelAutoFocus();
            requestLayout();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void setCameraParms() {
        Camera.Parameters myParam = mCamera.getParameters();
        List<String> flashModes = myParam.getSupportedFlashModes();
        String flashMode = myParam.getFlashMode();
        // Check if camera flash exists
        if (flashModes == null) {
            return;
        }
        if (!Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)) {
            // Turn off the flash
            if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                myParam.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            } else {
            }
        }

        float percent = calcPreviewPercent();
        List<Camera.Size> supportedPreviewSizes = myParam.getSupportedPreviewSizes();
        previewSize = getPreviewMaxSize(supportedPreviewSizes, percent);
        // 获取摄像头支持的各种分辨率
        List<Camera.Size> supportedPictureSizes = myParam.getSupportedPictureSizes();
        pictureSize = findSizeFromList(supportedPictureSizes, previewSize);
        if (pictureSize == null) {
            pictureSize = getPictureMaxSize(supportedPictureSizes, previewSize);
        }
        // 设置照片分辨率，注意要在摄像头支持的范围内选择
        myParam.setPictureSize(pictureSize.width, pictureSize.height);
        // 设置预浏尺寸，注意要在摄像头支持的范围内选择
        myParam.setPreviewSize(previewSize.width, previewSize.height);
        myParam.setJpegQuality(70);

        mCamera.setParameters(myParam);
    }

    private float calcPreviewPercent() {
        float d = screenHeight;
        return d / screenWidth;
    }

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

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
}

