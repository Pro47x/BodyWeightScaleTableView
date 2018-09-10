![效果图](http://upload-images.jianshu.io/upload_images/4952738-41d9f206db1376f9.gif?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 需求分析
 1. 绘制刻度
 2. 可滑动，滑动停止后自动移动到最近刻度
 3. 增加了两边透明度变化

## 涉及知识点

 1. canvas绘制
 2. 属性动画
 3. GestureDetector手势识别器

### 大概的实现过程
**首先放上所有成员变量**

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


 1. 实例化画笔，在onDraw中绘制最基本的元素
 
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
 2. 绘制刻度线
 
  //画刻度线
        int everyScaleG = ONE_KG / scaleTableGNum;
        //这个求余，是为了得到当前体重数和最小刻度的偏差，用于接下来绘制的时候进行位置修正
        float offset = bodyWeight % everyScaleG;
        //
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
        //这是为了不同宽度的屏幕适配，所以从中间往两边绘制
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
 3. 绘制遮罩层，用来做两边透明度的效果
 
        //绘制遮罩层，用来进行两端透明度的变化
        if (mForegroundPaint == null) {
            initForegroundPaint(canvasWidth);
        }
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mForegroundPaint);
        
  //初始化遮罩的画笔
  private void initForegroundPaint(int w) {
         mForegroundPaint = new Paint();
   mForegroundPaint.setShader(new LinearGradient(0, 0, w / 2, 0, 0X00FFFFFF, 0XFFFFFFFF, Shader.TileMode.MIRROR));
         mForegroundPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
     }
 4. 实例化手势识别器GestureDetector，用它来接管View的onTouchEvent，并在onScroll和onFling中作出对应动作
 
  @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (mFlingAnim != null) {
                    mFlingAnim.cancel();
                }
                //每次滚动的刻度
                bodyWeight += distanceX * scaleTableGWidth;
                if (bodyWeight >= maxWeight) {
                    bodyWeight = maxWeight;
                } else if (bodyWeight <= minWeight) {
                    bodyWeight = minWeight;
                }
                invalidate();
                return true;
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
以上用到的方法

  //通过滑动速率来得到滑动动画的目标体重值
     private int getTargetWeightByVelocityX(float velocityX) {
        return (int) (MAX_FLING_WEIGHT * mLinearOutSlowInInterpolator.getInterpolation(Math.abs(velocityX / MAX_FLING_SPEED)));
     }
  
  //通过滑动速率来得到滑动动画持续时间
  private int getDurationByVelocityX(float velocityX) {
        return (int) (MAX_FLING_WEIGHT_DURATION * mLinearOutSlowInInterpolator.getInterpolation(Math.abs(velocityX / MAX_FLING_SPEED)));
     }
    
     //开始一个平滑动画
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
 
###实现思想
本Demo是使用Canvas进行绘制刻度，为了适配不同宽度的屏幕，所以小的刻度使用从中心指针处向两边绘制到屏幕边界的方法，使用GestureDetector接管View的触摸事件，在onScroll中修改体重值并不断进行重绘，达到刻度尺滚动的效果。并在onFling中接收抛动的事件，开启一个属性动画达到平滑的效果。由于我没有在GestureDetector中找到能响应不fling的up事件，所以要自己手动在onTouchEvent中写ACIONT_UP时的动作
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
http://hencoder.com/ui-1-2/)
https://github.com/Pro47x/BodyWeightScaleTableView    
