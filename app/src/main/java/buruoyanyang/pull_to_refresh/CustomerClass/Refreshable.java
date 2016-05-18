package buruoyanyang.pull_to_refresh.CustomerClass;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.Timer;

import buruoyanyang.pull_to_refresh.R;

/**
 * 不若艳阳
 * 16/5/18 20:13 23:12.
 */
public class Refreshable extends LinearLayout implements View.OnTouchListener {

    //refreshing
    public static final int STATUS_REFRESHING = 0;

    //release to refresh
    public static final int STATUS_RELEASE_TO_REFRESH = 1;

    //pull to refresh
    public static final int STATUS_PULL_TO_REFRESH = 2;

    //refresh over
    public static final int STATUS_REFRESHED = 3;

    //head back speed
    public int HEAD_SCROLL_BACK_SPEED = 8;

    //status
    public int STATUS_REFRESH = STATUS_REFRESHED;

    private OnRefreshListener refreshListener;

    //refresh succeed
    public static final int REFRESH_SUCCEED = 0;

    //refresh failed
    public static final int REFRESH_FAILED = 1;

    //refresh head
    private View headView;

    //refresh content view
    private View contentView;

    private float downY = 0;

    private float lastY = 0;

    public float moveDeltaY = 0;

    private float refreshDist = 200;

    private Timer timer;

    private MyTimerTask timerTask;

    //first time Layout
    private boolean isLayout = false;

    //can do refresh
    private boolean canPull = true;

    //touch in refreshing
    private boolean isTouchInRefreshing = false;

    //touch / head
    private float radio = 2;

    //arrow animation
    private RotateAnimation arrowAnimation;

    private RotateAnimation refreshingAnimation;

    //arrow icon
    private View pullView;

    //refreshing icon
    private View refreshingView;

    //refresh result icon
    private View stateImageView;

    //refresh result : fail or succeed
    private TextView stateTextView;

    @SuppressLint("HandlerLeak")
    Handler updateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //scroll back fast and fast
            HEAD_SCROLL_BACK_SPEED = (int) (8 + 5 * Math.tan(Math.PI / 2 / getMeasuredHeight() * moveDeltaY));
            if (STATUS_REFRESH == STATUS_REFRESHING && moveDeltaY < refreshDist && !isTouchInRefreshing) {
                //refreshing and do not pull up ,hold "refreshing..."
                moveDeltaY = refreshDist;
                //cancel handler
                timerTask.cancel();
            }
            if (canPull) {
                moveDeltaY -= HEAD_SCROLL_BACK_SPEED;
            }
            if (moveDeltaY <= 0) {
                //SCROLL BACK FINISHED
                moveDeltaY = 0;
                pullView.clearAnimation();
                // TODO: 16/5/18
                if (STATUS_REFRESH != STATUS_REFRESHING) {
                    changeState(STATUS_PULL_TO_REFRESH);
                    timerTask.cancel();
                }
                requestLayout();
            }
        }
    };

    public void setOnRefreshListener(OnRefreshListener listener) {
        refreshListener = listener;
    }

    public Refreshable(Context context) {
        super(context);
    }

    public Refreshable(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);

    }

    public Refreshable(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void hideHead() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        timerTask = new MyTimerTask(updateHandler);
        timer.schedule(timerTask, 0, 5);
    }

    //refresh complete
    @SuppressLint("HandlerLeak")
    public void refreshFinish(int refreshResult) {
        refreshingView.clearAnimation();
        refreshingView.setVisibility(GONE);
        switch (refreshResult) {
            case REFRESH_SUCCEED:
                stateImageView.setVisibility(VISIBLE);
                stateTextView.setText(R.string.refreshed);
                stateImageView.setBackgroundResource(R.drawable.refresh_succeed);
                break;
            case REFRESH_FAILED:
                stateImageView.setVisibility(VISIBLE);
                stateTextView.setText(R.string.refresh_failed);
                stateImageView.setBackgroundResource(R.drawable.refresh_failed);
                break;
            default:
                break;
        }
        //hold head 1 min
        new Handler() {
            @Override
            public void handleMessage(Message msg) {
                STATUS_REFRESH = STATUS_PULL_TO_REFRESH;
                hideHead();
            }
        }.sendEmptyMessageDelayed(0, 1000);
    }

    private void changeState(int to) {
        STATUS_REFRESH = to;
        switch (STATUS_REFRESH) {
            case STATUS_PULL_TO_REFRESH:
                stateImageView.setVisibility(GONE);
                stateTextView.setText(R.string.pull_to_refresh);
                pullView.clearAnimation();
                pullView.setVisibility(VISIBLE);
                break;
            case STATUS_RELEASE_TO_REFRESH:
                stateTextView.setText(R.string.release_to_refresh);
                pullView.startAnimation(arrowAnimation);
                break;
            case STATUS_REFRESHING:
                pullView.clearAnimation();
                refreshingView.setVisibility(VISIBLE);
                pullView.setVisibility(INVISIBLE);
                refreshingView.startAnimation(refreshingAnimation);
                stateTextView.setText(R.string.refreshing);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            //按下动作
            case MotionEvent.ACTION_DOWN:
                downY = event.getY();
                lastY = downY;
                if (timerTask != null) {
                    timerTask.cancel();
                }
                //如果点击了下拉头布局,但是又没有对下拉头做事件响应,所以直接返回一个true,不交给父类分发.
                if (event.getY() < moveDeltaY)
                    return true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (canPull) {
                    //造成用力拉的感觉,radio为阻尼系数
                    moveDeltaY = moveDeltaY + (event.getY() - lastY) / radio;
                    if (moveDeltaY < 0)
                        //超出不算
                        moveDeltaY = 0;
                    if (moveDeltaY > getMeasuredHeight())
                        //超出头不算
                        moveDeltaY = getMeasuredHeight();
                    if (STATUS_REFRESH == STATUS_REFRESHING) {
                        //在刷新的时候触摸
                        isTouchInRefreshing = true;
                    }
                }
                lastY = event.getY();
                radio = (float) (2 + 2 * Math.tan(Math.PI / 2 / getMeasuredHeight() * moveDeltaY));
                requestLayout();
                if (moveDeltaY <= refreshDist && STATUS_REFRESH == STATUS_RELEASE_TO_REFRESH) {
                    //如果下拉距离没达到是刷新距离,且当前状态为释放刷新,则改状态为下拉刷新
                    changeState(STATUS_PULL_TO_REFRESH);
                }
                if (moveDeltaY >= refreshDist && STATUS_REFRESH == STATUS_PULL_TO_REFRESH) {
                    changeState(STATUS_RELEASE_TO_REFRESH);
                }
                if (moveDeltaY > 8) {
                    //防止下拉过程中误触发长按事件和点击事件
                    clearContentViewEvents();
                }
                if (moveDeltaY > 0) {
                    //正在下拉刷新,不让子控件捕获事件
                    return true;
                }
            case MotionEvent.ACTION_UP:
                if (moveDeltaY > refreshDist)
                    //正在刷新时往下拉释放后下拉头不隐藏
                    isTouchInRefreshing = false;
                if (STATUS_REFRESH == STATUS_RELEASE_TO_REFRESH) {
                    changeState(STATUS_REFRESHING);
                    //刷新操作
                    if (refreshListener != null) {
                        refreshListener.onRefresh();
                    }
                }
                hideHead();
            default:
                break;

        }
        //让父类分发事件
        return super.dispatchTouchEvent(event);
    }

    //通过反射修改字段去掉长按事件和点击事件
    private void clearContentViewEvents() {
        try {
            Field[] fields = AbsListView.class.getDeclaredFields();
            for (Field field : fields)
                if (field.getName().equals("mPendingCheckForLongPress")) {
                    // mPendingCheckForLongPress是AbsListView中的字段，通过反射获取并从消息列表删除，去掉长按事件
                    field.setAccessible(true);
                    contentView.getHandler().removeCallbacks((Runnable) field.get(contentView));
                } else if (field.getName().equals("mTouchMode")) {
                    // TOUCH_MODE_REST = -1， 这个可以去除点击事件
                    field.setAccessible(true);
                    field.set(contentView, -1);
                }
            // 去掉焦点
            ((AbsListView) contentView).getSelector().setState(new int[]{0});
        } catch (Exception ignored) {
        }
    }

    /**
     * 绘制阴影效果
     *
     * @param canvas
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (moveDeltaY == 0) {
            return;
        }
        RectF rectF = new RectF(0, 0, getMeasuredWidth(), moveDeltaY);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        //阴影高度为26
        LinearGradient linearGradient = new LinearGradient(0, moveDeltaY, 0, moveDeltaY - 26, 0x66000000, 0x00000000, Shader.TileMode.CLAMP);
        paint.setShader(linearGradient);
        paint.setStyle(Paint.Style.FILL);
        //在moveDeltaY处向上变淡
        canvas.drawRect(rectF, paint);
    }

    private void initView(Context context) {
        timer = new Timer();
        timerTask = new MyTimerTask(updateHandler);
        arrowAnimation = (RotateAnimation) AnimationUtils.loadAnimation(context, R.anim.reverse_anim);
        refreshingAnimation = (RotateAnimation) AnimationUtils.loadAnimation(context, R.anim.rotating);
        //add rotate animation
        LinearInterpolator lir = new LinearInterpolator();
        refreshingAnimation.setInterpolator(lir);
    }

    private void initView() {
        pullView = headView.findViewById(R.id.pull_icon);
        stateTextView = (TextView) headView.findViewById(R.id.state_tv);
        refreshingView = headView.findViewById(R.id.refreshing_icon);
        stateImageView = headView.findViewById(R.id.state_iv);
    }

    /**
     * 重写布局初始化
     *
     * @param changed
     * @param l
     * @param t
     * @param r
     * @param b
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!isLayout) {
            //第一次进来初始化
            headView = getChildAt(0);
            contentView = getChildAt(1);
            //给AbsListView设置onTouchListener
            contentView.setOnTouchListener(this);
            isLayout = true;
            initView();
            refreshDist = ((ViewGroup) headView).getChildAt(0).getMeasuredHeight();
        }
        if (canPull) {
            //改变子控件布局
            headView.layout(0, (int) moveDeltaY - headView.getMeasuredHeight(), headView.getMeasuredWidth(), (int) moveDeltaY);
            contentView.layout(0, (int) moveDeltaY, contentView.getMeasuredWidth(), (int) moveDeltaY + contentView.getMeasuredHeight());
        } else {
            super.onLayout(changed, l, t, r, b);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //第一个item可见且滑动到顶部
        AbsListView absListView = null;
        try {
            absListView = (AbsListView)v;
        }
        catch (Exception e)
        {
            return false;
        }
        if (absListView.getCount() == 0)
        {
            //没有item的时候也可以下拉刷新
            canPull = true;
        }
        else if (absListView.getFirstVisiblePosition() == 0 && absListView.getChildAt(0).getTop() >= 0)
        {
            canPull = true;
        }
        else if (absListView.getFirstVisiblePosition() == 0 && absListView.getChildAt(0).getTop() >= 0)
        {
            //已经滑动到AbsListView的顶部了
            canPull = true;
        }
        else
        {
            canPull = false;
        }
        return false;
    }
}




















































