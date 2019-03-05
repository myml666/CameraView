package com.itfitness.cameraview;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

/**
 * @ProjectName: CameraView
 * @Package: PACKAGE_NAME
 * @ClassName: com.itfitness.cameraview.MainActicity
 * @Description: java类作用描述 ：
 * @Author: 作者名：lml
 * @CreateDate: 2019/3/5 14:03
 * @UpdateUser: 更新者：
 * @UpdateDate: 2019/3/5 14:03
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */

public class MainActicity extends AppCompatActivity {
    private ImageView imgCardocr;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imgCardocr = (ImageView) findViewById(R.id.img_cardocr);
        imgCardocr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActicity.this,CardOcrActivity.class));
            }
        });
    }
}
