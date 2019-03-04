package com.itfitness.cameraview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.itfitness.cameraview.widget.CameraView;
import com.itfitness.cameraview.widget.MaskView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CameraView.TakePictureCallBack{
    private CameraView camera;
    private View viewTakepicture;
    private ImageView img;
    private MaskView mask;
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
        setContentView(R.layout.activity_main);
        LogUtils.eTag("Cehgsi",tessdata);
        camera = (CameraView) findViewById(R.id.camera);
        viewTakepicture = (View) findViewById(R.id.view_takepicture);
        img = (ImageView) findViewById(R.id.img);
        mask = (MaskView) findViewById(R.id.mask);
        viewTakepicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.setmMaskSize(new int[]{(int) mask.getmMaskWidth(), (int) mask.getmMaskHeight()});
                camera.takePicture(MainActivity.this);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(!camera.isPreviewing()){
            camera.startPreview();
            img.setVisibility(View.GONE);
        }else {
            super.onBackPressed();
        }
    }

    /**
     * 复制训练文件到SD卡
     */
    private void copyRes2SD(){
        try {
            File dir = new File(tessdata);
            if(!dir.exists()){
                dir.mkdirs();
                InputStream inputStream = getResources().openRawResource(R.raw.chi_sim);
                LogUtils.eTag("Cehsi",dir.getAbsolutePath());
                ToastUtils.showShort(dir.getAbsolutePath());
                File file = new File(dir,"chi_sim.traineddata");
                LogUtils.eTag("Cehsi",file.getAbsolutePath());
                FileOutputStream fileOutputStream=new FileOutputStream(file);
                byte[] buff = new byte[1024];
                int len = 0;
                while ((len=inputStream.read(buff))!=-1){
                    fileOutputStream.write(buff,0,len);
                }
                inputStream.close();
                fileOutputStream.close();
            }
        }catch (Exception e){
            LogUtils.eTag("测试",e.getMessage());
        }
    }

    @Override
    public void onShutter() {

    }

    @Override
    public void onPictureTaken(boolean isSuccess,final Bitmap filepath) {
        if(isSuccess){
            img.setVisibility(View.VISIBLE);
            img.setImageBitmap(filepath);
            if(mTessBaseAPI==null){
                mTessBaseAPI = new TessBaseAPI();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    copyRes2SD();
                    mTessBaseAPI = new TessBaseAPI();
                    mTessBaseAPI.init(DATA_PATH,"chi_sim");
                    mTessBaseAPI.setImage(filepath);
                    String utf8Text = mTessBaseAPI.getUTF8Text();
                    LogUtils.eTag("测试识别",utf8Text);
                    mTessBaseAPI.end();
                }
            }).start();

        }
    }
}
