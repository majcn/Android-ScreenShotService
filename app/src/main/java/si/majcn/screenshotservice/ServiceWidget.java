package si.majcn.screenshotservice;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class ServiceWidget extends ImageView {

    public enum State {
        NONE, UP, DOWN
    }

    private State state;

    public ServiceWidget(Context context) {
        super(context);
    }

    public ServiceWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ServiceWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void setState(State state) {
        this.state = state;
        switch (state) {
            case UP:
                setImageResource(android.R.drawable.arrow_up_float);
                break;
            case DOWN:
                setImageResource(android.R.drawable.arrow_down_float);
                break;
            default:
                setImageResource(R.mipmap.ic_launcher);
                break;
        }
    }

    private void nextState() {
        State[] vals = State.values();
        int i = state.ordinal();
        if (i < vals.length - 1) {
            setState(vals[i + 1]);
        } else {
            setState(vals[0]);
        }
    }

    public State getState() {
        return state;
    }

    public void init(final WindowManager windowManager) {
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;

        setState(State.NONE);

        setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            private int lastEvent = MotionEvent.INVALID_POINTER_ID;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        lastEvent = MotionEvent.ACTION_DOWN;
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (lastEvent != MotionEvent.ACTION_MOVE) {
                            nextState();
                        }

                        lastEvent = MotionEvent.ACTION_UP;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(ServiceWidget.this, params);

                        lastEvent = MotionEvent.ACTION_MOVE;
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(ServiceWidget.this, params);
    }
}
