package cn.pro47.demo.keduview;

import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;


/**
 * 一个体重刻度表的View
 * 不明之处可以加Q418755850详询
 *
 * @author Pro47x
 */
public class BodyWeightScaleTableView extends View {
    /**
     * 最大的滑动速度
     */
    private static final int MAX_FLING_SPEED = 6000;

    private static final String KG = "kg";
    private static final int ONE_KG = 1000;
    /**
     * 最大的可滑动过去的体重数 默认：3公斤
     */
    private static final int MAX_FLING_WEIGHT = 3000;
    /**
     * 最大的滑动动画持续时间
     */
    private static final int MAX_FLING_WEIGHT_DURATION = 1000;

    private LinearOutSlowInInterpolator mLinearOutSlowInInterpolator = new LinearOutSlowInInterpolator();
    /**
     * 最小体重：30kg
     */
    private int minWeight = 30000;
    /**
     * 最大体重：200.0kg
     */
    private int maxWeight = 200000;
    /**
     * 默认体重为60.0kg
     * 数字单位为g
     */
    private int bodyWeight = 60000;
    /**
     * 体重的文字大小
     */
    private int textSizeWeight = sp2pix(22);
    /**
     * kg字的文字大小
     */
    private int textSizeKg = sp2pix(16);

    /**
     * 刻度基线的宽度
     */
    private int lineWidthBase = dp2pix(1);
    /**
     * 刻度线g的宽度
     */
    private int lineWidthG = dp2pix(2);
    /**
     * 刻度线g的高度
     */
    private int lineHeightG = dp2pix(20);
    /**
     * 刻度线kg的宽度
     */
    private int lineWidthKg = dp2pix(4);
    /**
     * 刻度线kg的高度
     */
    private int lineHeightKg = dp2pix(40);
    /**
     * 每公斤中的刻度数量
     */
    private int scaleTableGNum = 10;
    /**
     * 每格的宽度
     */
    private int scaleTableGWidth = dp2pix(10);

    private Paint mWeightTextPain;
    private Paint mScaleLinePain;
    private Paint mScaleWeightTextPain;
    /**
     * 遮罩的画笔
     */
    private Paint mForegroundPaint;
    /**
     * 手势识别器
     */
    private GestureDetector mGesture;

    /**
     * 平滑动画
     */
    private ObjectAnimator mFlingAnim;
    private FlingAnimUpdateListener mFlingAnimUpdateListener = new FlingAnimUpdateListener();
    private BodyWeightUpdateListener mBodyWeightUpdateListener;

    {
        mWeightTextPain = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWeightTextPain.setColor(0xFF49BA73);
        mWeightTextPain.setStrokeWidth(lineWidthKg);

        mScaleLinePain = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScaleLinePain.setColor(Color.GRAY);

        mScaleWeightTextPain = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScaleWeightTextPain.setColor(Color.BLACK);
        mScaleWeightTextPain.setTextSize(sp2pix(18));
        mScaleWeightTextPain.setTextAlign(Paint.Align.CENTER);

        mGesture = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (mFlingAnim != null) {
                    mFlingAnim.cancel();
                }
                bodyWeight += distanceX * scaleTableGWidth;
                if (bodyWeight >= maxWeight) {
                    bodyWeight = maxWeight;
                } else if (bodyWeight <= minWeight) {
                    bodyWeight = minWeight;
                }
                invalidate();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                int target;
                if (velocityX > 0) {
                    target = bodyWeight - getTargetWeightByVelocityX(velocityX);
                } else {
                    target = bodyWeight + getTargetWeightByVelocityX(velocityX);
                }
                startSmoothAnim(target, getDurationByVelocityX(velocityX));
                return true;
            }
        });
        mGesture.setIsLongpressEnabled(false);
    }

    public BodyWeightScaleTableView(Context context) {
        this(context, null);
    }

    public BodyWeightScaleTableView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BodyWeightScaleTableView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGesture.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mFlingAnim != null && !mFlingAnim.isRunning()) {
                startSmoothAnim(revisedTarget(bodyWeight), 100);
            }
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int saved = canvas.saveLayer(null, null, Canvas.ALL_SAVE_FLAG);

        String weightStr = String.valueOf(bodyWeight / ONE_KG) + "." + String.valueOf(bodyWeight % ONE_KG);
        int canvasWidth = canvas.getWidth();
        int centerX = canvasWidth / 2;
        int centerY = canvas.getHeight() / 2;

        //画出当前公斤数
        int textLine = centerY - dp2pix(50);
        mWeightTextPain.setTextSize(textSizeWeight);
        mWeightTextPain.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(weightStr, centerX, textLine, mWeightTextPain);
        mWeightTextPain.setTextSize(textSizeKg);
        canvas.drawText(KG, centerX + mWeightTextPain.measureText(weightStr),
                textLine - mWeightTextPain.getFontSpacing() / 2, mWeightTextPain);

        //画刻度基准线
        mScaleLinePain.setStrokeWidth(lineWidthBase);
        canvas.drawLine(0, centerY, canvasWidth, centerY, mScaleLinePain);

        //画刻度线
        int everyScaleG = ONE_KG / scaleTableGNum;
        float offset = bodyWeight % everyScaleG;
        float handOffset = offset / everyScaleG * scaleTableGWidth;

        float currentLeftHandWeight;
        float currentRightHandWeight;
        float leftLineX;
        float rightLineX;
        if (offset == 0) {
            float lineX = centerX;
            float currentHandWeight = bodyWeight;
            drawScaleTable(canvas, centerY, lineX, currentHandWeight);
            currentLeftHandWeight = bodyWeight - everyScaleG;
            currentRightHandWeight = bodyWeight + everyScaleG;
            leftLineX = lineX - scaleTableGWidth;
            rightLineX = lineX + scaleTableGWidth;
        } else {
            currentLeftHandWeight = bodyWeight - offset % everyScaleG;
            currentRightHandWeight = bodyWeight + everyScaleG - offset % everyScaleG;
            leftLineX = centerX - handOffset;
            rightLineX = centerX + scaleTableGWidth - handOffset;
        }
        while (rightLineX < canvasWidth + 2 * scaleTableGWidth) {
            //从中开始向左画指针
            if (currentLeftHandWeight >= minWeight) {
                drawScaleTable(canvas, centerY, leftLineX, currentLeftHandWeight);
            }
            //从中开始向右画指针
            if (currentRightHandWeight <= maxWeight) {
                drawScaleTable(canvas, centerY, rightLineX, currentRightHandWeight);
            }
            currentLeftHandWeight -= everyScaleG;
            currentRightHandWeight += everyScaleG;
            leftLineX -= scaleTableGWidth;
            rightLineX += scaleTableGWidth;
        }
        //画出中间的指针
        canvas.drawLine(centerX, centerY, centerX, centerY + lineHeightKg, mWeightTextPain);

        //绘制遮罩层，用来进行两端透明度的变化
        if (mForegroundPaint == null) {
            initForegroundPaint(canvasWidth);
        }
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mForegroundPaint);

        canvas.restoreToCount(saved);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initForegroundPaint(w);
    }

    private void drawScaleTable(Canvas canvas, int centerY, float lineX, float currentHandWeight) {
        if (currentHandWeight % ONE_KG == 0) {
            mScaleLinePain.setStrokeWidth(lineWidthKg);
            canvas.drawLine(lineX, centerY, lineX, centerY + lineHeightKg, mScaleLinePain);
            canvas.drawText(String.valueOf(currentHandWeight / ONE_KG), lineX, centerY + lineHeightKg + dp2pix(30), mScaleWeightTextPain);
        } else {
            mScaleLinePain.setStrokeWidth(lineWidthG);
            canvas.drawLine(lineX, centerY, lineX, centerY + lineHeightG, mScaleLinePain);
        }
    }

    public int getBodyWeight() {
        return bodyWeight;
    }

    public void setBodyWeight(int bodyWeight) {
        this.bodyWeight = bodyWeight;
        invalidate();
    }

    /**
     * @param anim 是否进行平滑动画
     */
    public void setBodyWeight(int bodyWeight, boolean anim) {
        if (anim) {
            startSmoothAnim(bodyWeight, MAX_FLING_WEIGHT_DURATION);
        } else {
            setBodyWeight(bodyWeight);
        }
    }

    /**
     * 开始一个平滑动画
     */
    private void startSmoothAnim(int targetWeight, int duration) {
        if (targetWeight >= maxWeight) {
            targetWeight = maxWeight;
        } else if (targetWeight <= minWeight) {
            targetWeight = minWeight;
        } else {
            targetWeight = revisedTarget(targetWeight);
        }
        if (mFlingAnim != null) {
            mFlingAnim.cancel();
            mFlingAnim = null;
        }
        mFlingAnim = ObjectAnimator.ofInt(BodyWeightScaleTableView.this, "bodyWeight", this.bodyWeight, targetWeight);
        mFlingAnim.setInterpolator(mLinearOutSlowInInterpolator);
        mFlingAnim.setDuration(duration);
        mFlingAnim.addUpdateListener(mFlingAnimUpdateListener);
        mFlingAnim.start();
    }

    /**
     * 对体重值进行修正，以符合刻度
     */
    private int revisedTarget(int targetWeight) {
        int oneScaleBox = ONE_KG / scaleTableGNum;
        int offset = targetWeight % oneScaleBox;
        int half = 2;
        if (offset != 0) {
            if (offset > oneScaleBox / half) {
                targetWeight += oneScaleBox - offset;
            } else {
                targetWeight -= offset;
            }
        }
        return targetWeight;
    }

    /**
     * 通过滑动速率来得到滑动动画的目标体重值
     */
    private int getTargetWeightByVelocityX(float velocityX) {
        return (int) (MAX_FLING_WEIGHT * mLinearOutSlowInInterpolator.getInterpolation(Math.abs(velocityX / MAX_FLING_SPEED)));
    }

    /**
     * 通过滑动速率来得到滑动动画持续时间
     */
    private int getDurationByVelocityX(float velocityX) {
        return (int) (MAX_FLING_WEIGHT_DURATION * mLinearOutSlowInInterpolator.getInterpolation(Math.abs(velocityX / MAX_FLING_SPEED)));
    }

    private int dp2pix(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    private int sp2pix(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, dp, Resources.getSystem().getDisplayMetrics());
    }

    public BodyWeightUpdateListener getBodyWeightUpdateListener() {
        return mBodyWeightUpdateListener;
    }

    public void setBodyWeightUpdateListener(BodyWeightUpdateListener bodyWeightUpdateListener) {
        mBodyWeightUpdateListener = bodyWeightUpdateListener;
    }

    private void initForegroundPaint(int w) {
        mForegroundPaint = new Paint();
        mForegroundPaint.setShader(new LinearGradient(0, 0, w / 2, 0, 0X00FFFFFF, 0XFFFFFFFF, Shader.TileMode.MIRROR));
        mForegroundPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    private class FlingAnimUpdateListener implements ValueAnimator.AnimatorUpdateListener {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if (mBodyWeightUpdateListener != null) {
                mBodyWeightUpdateListener.update((Integer) animation.getAnimatedValue());
            }
        }
    }

    public interface BodyWeightUpdateListener {
        void update(int bodyWeight);
    }
}
