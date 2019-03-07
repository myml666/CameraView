package com.itfitness.cameraview.utils;

import android.content.Context;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.itfitness.cameraview.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * @ProjectName: CameraView
 * @Package: com.itfitness.cameraview.utils
 * @ClassName: FileUtil
 * @Description: java类作用描述 ：
 * @Author: 作者名：lml
 * @CreateDate: 2019/3/5 13:56
 * @UpdateUser: 更新者：
 * @UpdateDate: 2019/3/5 13:56
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */

public class FileUtil {
    /**
     * 复制训练文件到SD卡
     */
    public static void copyRes2SD(String filepath, Context context){
        try {
            File dir = new File(filepath);
            if(!dir.exists()){
                dir.mkdirs();
                InputStream inputStream = context.getResources().openRawResource(R.raw.card);
                LogUtils.eTag("Cehsi",dir.getAbsolutePath());
                ToastUtils.showShort(dir.getAbsolutePath());
                File file = new File(dir,"card.traineddata");
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
}
