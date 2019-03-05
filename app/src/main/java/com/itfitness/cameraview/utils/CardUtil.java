package com.itfitness.cameraview.utils;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * @ProjectName: CameraView
 * @Package: com.itfitness.cameraview.utils
 * @ClassName: CardNumberUtils
 * @Description: java类作用描述 ：
 * @Author: 作者名：lml
 * @CreateDate: 2019/3/5 12:21
 * @UpdateUser: 更新者：
 * @UpdateDate: 2019/3/5 12:21
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */

public class CardUtil {
    /**
     * 裁剪身份证区域
     * @param bitmap
     * @return
     */
    public static Bitmap clipCardNumber(Bitmap bitmap){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int clipX = (int) (width*0.25);
        int clipY = height/4*3;
        return Bitmap.createBitmap(bitmap,clipX,clipY,width-clipX-10,height/5);
    }

    /**
     * 对图像进行灰度和二值化处理
     * @param bitmap
     * @return
     */
    public static Bitmap getBinaryImage(Bitmap bitmap){
        Mat src = new Mat();
        Mat dst = new Mat();
        Utils.bitmapToMat(bitmap,src);
        Imgproc.cvtColor(src,dst,Imgproc.COLOR_BGRA2GRAY);//灰度处理
        Imgproc.threshold(dst,src, 50, 255, Imgproc.THRESH_BINARY);//图像二值化
        Utils.matToBitmap(src,bitmap);
        return bitmap;
    }
}
