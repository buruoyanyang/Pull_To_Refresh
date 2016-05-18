package buruoyanyang.pull_to_refresh.CustomerClass;

import android.os.Handler;

import java.util.TimerTask;

/**
 * Created by 不若艳阳 on 16/5/18 21:00.
 */
public class MyTimerTask extends TimerTask {
    Handler handler;

    public MyTimerTask(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {
        handler.sendMessage(handler.obtainMessage());
    }
}
