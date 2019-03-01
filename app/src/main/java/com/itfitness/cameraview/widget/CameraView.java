package com.itfitness.cameraview.widget;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

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

public class CameraView extends SurfaceView implements SurfaceHolder.Callback{
    private Camera mCamera;//相机
    public CameraView(Context context) {
        super(context);
        init();
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    private void init() {
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera = Camera.open();//获取相机
            initCamera();
        }catch (Exception e){
            mCamera = null;
        }
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        if(event.getAction()==MotionEvent.ACTION_DOWN){
//            if(mCamera!=null){
//                mCamera.autoFocus(new Camera.AutoFocusCallback() {
//                    @Override
//                    public void onAutoFocus(boolean success, Camera camera) {
//                        Toast.makeText(getContext().getApplicationContext(), "自动对焦", Toast.LENGTH_SHORT).show();
//                    }
//                });
//            }
//            return true;
//        }
//        return super.onTouchEvent(event);
//    }

    /**
     * 加载相机配置
     */
    private void initCamera() {
        try {
            mCamera.setPreviewDisplay(getHolder());//当前控件显示相机数据
            mCamera.setDisplayOrientation(90);//调整预览角度
            setCameraParameters();
            mCamera.startPreview();//打开相机
        }catch (Exception e){
            releaseCamera();
        }
    }

    /**
     * 配置相机参数
     */
    private void setCameraParameters() {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//自动对焦
        mCamera.setParameters(parameters);
    }

    /**
     * 释放相机
     */
    private void releaseCamera() {
        if(mCamera!=null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera=null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mCamera!=null){
            mCamera.stopPreview();
        }
        initCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }
}
