package com.itfitness.cameraview;

import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.blankj.utilcode.util.ToastUtils;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.itfitness.cameraview.utils.CardUtil;
import com.itfitness.cameraview.utils.FileUtil;
import com.itfitness.cameraview.widget.CameraView;
import com.itfitness.cameraview.widget.MaskView;
import org.opencv.android.OpenCVLoader;
import java.io.File;

public class CardOcrActivity extends AppCompatActivity implements CameraView.TakePictureCallBack{
    private CameraView camera;
    private View viewTakepicture;
    private ImageView img;
    private MaskView mask;
    private ImageView imgOk;
    private ImageView imgCancle;
    private Bitmap mBitmapResult;
    private TextView tvCardnumber;
    private RelativeLayout layoutCardresult;

    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString()
            + "/Tess";
    /**
     * 在DATAPATH中新建这个目录，TessBaseAPI初始化要求必须有这个目录。
     */
    private static final String tessdata = DATA_PATH + File.separator + "tessdata";
    private TessBaseAPI mTessBaseAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cardocr);
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
        camera = (CameraView) findViewById(R.id.camera);
        viewTakepicture = (View) findViewById(R.id.view_takepicture);
        img = (ImageView) findViewById(R.id.img);
        mask = (MaskView) findViewById(R.id.mask);
        camera.setTakePictureCallBack(this);
        imgOk = (ImageView) findViewById(R.id.img_ok);
        imgCancle = (ImageView) findViewById(R.id.img_cancle);
        layoutCardresult = (RelativeLayout) findViewById(R.id.layout_cardresult);
        tvCardnumber = (TextView) findViewById(R.id.tv_cardnumber);
        viewTakepicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.setmMaskSize(new int[]{(int) mask.getmMaskWidth(), (int) mask.getmMaskHeight()});
                camera.takePicture();
            }
        });
        imgOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                identifyCardNumber();
            }
        });
        imgCancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }
    private void initTessTwo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileUtil.copyRes2SD(tessdata,CardOcrActivity.this);
                    if(mTessBaseAPI==null){
                        mTessBaseAPI = new TessBaseAPI();
                        mTessBaseAPI.init(DATA_PATH,"nums");
                    }
                }catch (Exception e){}
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        if(!camera.isPreviewing()){
            layoutCardresult.setVisibility(View.GONE);
            tvCardnumber.setText("身份证号将展示在此");
            camera.startPreview();
        }else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mTessBaseAPI!=null){
            mTessBaseAPI.end();
        }
    }
    @Override
    public void onShutter() {

    }

    @Override
    public void onPictureTaken(boolean isSuccess,Bitmap filepath2) {
        if(isSuccess){
            layoutCardresult.setVisibility(View.VISIBLE);
//            img.setImageBitmap(filepath2);
            mBitmapResult = filepath2;
            final Bitmap bitmap = CardUtil.clipCardNumber(mBitmapResult);
            CardUtil.getBinaryImage(bitmap);
            img.setImageBitmap(bitmap);

        }
    }

    /**
     * 识别身份证号
     */
    private void identifyCardNumber(){
        if(mBitmapResult!=null){
            final Bitmap bitmap = CardUtil.clipCardNumber(mBitmapResult);
            CardUtil.getBinaryImage(bitmap);
            img.setImageBitmap(bitmap);
            if(mTessBaseAPI==null){
                mTessBaseAPI = new TessBaseAPI();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mTessBaseAPI.clear();
                        mTessBaseAPI.setImage(bitmap);
                        final String utf8Text = mTessBaseAPI.getUTF8Text();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvCardnumber.setText("身份证号:"+utf8Text);
                            }
                        });
                    }catch (Exception e){

                    }
                }
            }).start();
        }
    }
}
