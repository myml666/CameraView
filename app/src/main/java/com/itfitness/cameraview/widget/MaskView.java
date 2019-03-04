package com.itfitness.cameraview.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.itfitness.cameraview.R;

/**
 * @ProjectName: CameraView
 * @Package: com.itfitness.cameraview.widget
 * @ClassName: MaskView
 * @Description: java类作用描述 ：遮罩控件
 * @Author: 作者名：lml
 * @CreateDate: 2019/3/1 16:46
 * @UpdateUser: 更新者：
 * @UpdateDate: 2019/3/1 16:46
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */

public class MaskView extends View {
    private float mMaskWidth;//中间透明部分的宽度
    private float mMaskHeight;//中间透明部分的高度
    private Paint mPaintMask;//遮罩画笔
    private Paint mPaintText;//文字画笔
    private float mTextSize = 30;//文字大小
    private Path mMaskPath;//遮罩透明部分路径
    private String mTopTripStr = "请扫描本人身份证人像面";
    private String mBottomTripStr = "请保持光线充足，背景干净，手机与卡片持平";
    private Bitmap mPersonBitmap;

    public MaskView(Context context) {
        super(context);
        init();
    }

    public MaskView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MaskView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 当控件大小改变的时候动态调整遮罩的大小
     * @param w
     * @param h
     * @param oldw
     * @param oldh
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mMaskWidth = w/5*3;//遮罩透明部分的宽度为控件宽度的3/5
        mMaskHeight = mMaskWidth*1.59f;//遮罩透明部分的高度根据身份证比例算出
        mMaskPath.reset();
        float left = (w-mMaskWidth)/2;
        float top = (h-mMaskHeight)/2;
        float right = left + mMaskWidth;
        float bottom = top + mMaskHeight;
        mMaskPath.addRoundRect(new RectF(left,top,right,bottom),10,10, Path.Direction.CW);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.person);
        int dstscreen = (int) (mMaskWidth*0.57f);
        mPersonBitmap = Bitmap.createScaledBitmap(bitmap, (int) (dstscreen*0.85f), dstscreen, true);//根据二代身份证人像的比例缩放
        bitmap.recycle();
        invalidate();
    }
    /**
     * 返回遮罩宽度
     */
    public float getmMaskWidth() {
        return mMaskWidth;
    }
    /**
     * 返回遮罩高度
     */
    public float getmMaskHeight() {
        return mMaskHeight;
    }

    private void init(){
        //关闭硬件加速
        setLayerType(LAYER_TYPE_SOFTWARE,null);

        mPaintMask = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintMask.setStyle(Paint.Style.FILL);
        mPaintMask.setColor(Color.BLACK);
        mPaintMask.setAlpha(160);//设置半透明

        mPaintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintText.setStrokeWidth(3);
        mPaintText.setColor(Color.WHITE);//设置文字颜色
        mPaintText.setTextSize(mTextSize);//设置文字大小
        mPaintText.setTextAlign(Paint.Align.CENTER);//文字水平居中

        mMaskPath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();//离屏绘制
        canvas.drawRect(0,0,getWidth(),getHeight(),mPaintMask);//绘制整个控件大小遮罩
        mPaintMask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));//将透明部分抠出来
        canvas.drawPath(mMaskPath,mPaintMask);
        mPaintMask.setXfermode(null);//清除混合模式
        canvas.restore();
        canvas.save();
        canvas.translate(getWidth()/2,getHeight()/2);
        canvas.rotate(90);//旋转90度绘制文字
        canvas.drawText(mTopTripStr,0,-((mMaskWidth/2)+((getWidth()-mMaskWidth)/4)),mPaintText);
        canvas.drawText(mBottomTripStr,0,(mMaskWidth/2)+((getWidth()-mMaskWidth)/4),mPaintText);
        float left = 0.14f*mMaskHeight;//根据二代身份证人像与左边的距离比例来计算出
        float top = -mPersonBitmap.getHeight()/2;
        canvas.drawBitmap(mPersonBitmap,left,top,null);
        canvas.restore();
    }
}
