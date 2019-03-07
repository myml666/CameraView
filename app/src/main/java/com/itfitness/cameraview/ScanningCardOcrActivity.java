package com.itfitness.cameraview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.itfitness.cameraview.utils.FileUtil;
import com.itfitness.cameraview.widget.camera.ScanningCameraView;

import org.opencv.android.OpenCVLoader;

public class ScanningCardOcrActivity extends AppCompatActivity{
    private ScanningCameraView scanningCameraView;
    private ImageView imageView;
    private View viewScanningline;//扫描线
    private RelativeLayout layoutScanning;//扫描容器
    private ImageView imgLight;
    private boolean mLightFlag = false;//灯开关
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanning);
        initDatas();
        initView();
    }

    private void initDatas() {
        initLibrary();
    }

    /**
     * 加载必须的库
     */
    private void initLibrary() {
        boolean b = OpenCVLoader.initDebug();
        if(!b){
            ToastUtils.showShort("识别库加载失败");
        }
        initTessTwo();
    }

    private void initView(){
        scanningCameraView = (ScanningCameraView) findViewById(R.id.camera2);
        imageView = (ImageView) findViewById(R.id.img);
        viewScanningline = (View) findViewById(R.id.view_scanningline);
        layoutScanning = (RelativeLayout) findViewById(R.id.layout_scanning);
        imgLight = (ImageView) findViewById(R.id.img_light);
        scanningCameraView.setIdentifyCallBack(new ScanningCameraView.IdentifyCallBack() {
            @Override
            public void onIdentifySuccess(String cardNumber) {
                LogUtils.eTag("结果",cardNumber);
                Intent intent = new Intent(ScanningCardOcrActivity.this, MainActicity.class);//跳转
                intent.putExtra("idCard",cardNumber);
                startActivity(intent);
                finish();
            }

            @Override
            public void onIdentifyImage(Bitmap bitmap) {
                imageView.setImageBitmap(bitmap);
            }
        });
        imgLight.setSelected(false);
        imgLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mLightFlag){
                    imgLight.setSelected(false);
                    scanningCameraView.closeLight();
                }else {
                    imgLight.setSelected(true);
                    scanningCameraView.openLight();
                }
                mLightFlag = !mLightFlag;
            }
        });
        initOnLayoutListener();
    }
    private void initOnLayoutListener() {
        final ViewTreeObserver viewTreeObserver = this.getWindow().getDecorView().getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onGlobalLayout() {
                int height = layoutScanning.getMeasuredHeight();
                int width = layoutScanning.getMeasuredWidth();

                // 移除GlobalLayoutListener监听
                setAnimation(width);
                ScanningCardOcrActivity.this.getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }
    /**
     * 动画设置
     * @param width
     */
    void setAnimation(int width) {
        LogUtils.eTag("扫描",width+"");
        TranslateAnimation mAnimation = new TranslateAnimation(TranslateAnimation.RELATIVE_TO_PARENT, 0.99f,TranslateAnimation.RELATIVE_TO_PARENT, 0.01f,TranslateAnimation.ABSOLUTE, 0f,TranslateAnimation.ABSOLUTE,0f);
        mAnimation.setDuration(5000);
        mAnimation.setRepeatMode(Animation.REVERSE);// 设置反方向执行
        mAnimation.setRepeatCount(Animation.INFINITE);
        viewScanningline.setAnimation(mAnimation);
        mAnimation.start();
    }
    private void initTessTwo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileUtil.copyRes2SD(ScanningCameraView.TESSDATA,ScanningCardOcrActivity.this);
                }catch (Exception e){

                }
            }
        }).start();
    }
}
