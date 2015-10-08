package si.majcn.screenshotservice;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenShotService extends Service {

    private static final String TAG = "ScreenShot::Service";

    private WindowManager windowManager;
    private Vibrator vibrator;
    private LinearLayout container;
    private ImageView chatHeadUp;
    private ImageView chatHeadDown;

    private final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        chatHeadUp = generateChatHead(new Rect(563, 19, 1065, 760), new Rect(15, 1160, 517, 1896));
        chatHeadDown = generateChatHead(new Rect(23, 19, 525, 760), new Rect(555, 1160, 1057, 1901));

        container.addView(chatHeadUp);
        container.addView(chatHeadDown);

        windowManager.addView(container, params);
    }

    private ImageView generateChatHead(final Rect rect90, final Rect rect270) {
        final ImageView chatHead = new ImageView(this);
        chatHead.setImageResource(R.mipmap.ic_launcher);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;

        chatHead.setOnTouchListener(new View.OnTouchListener() {
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
                            int rotation = windowManager.getDefaultDisplay().getRotation();
                            if (rotation == Surface.ROTATION_90) {
                                postProcessScreenShot(takeScreenShot(), rect90, 270);
                            } else if (rotation == Surface.ROTATION_270) {
                                postProcessScreenShot(takeScreenShot(), rect270, 90);
                            }
                        }

                        lastEvent = MotionEvent.ACTION_UP;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(container, params);

                        lastEvent = MotionEvent.ACTION_MOVE;
                        return true;
                }
                return false;
            }
        });

        return chatHead;
    }

    private String takeScreenShot() {
        try {
            Process sh = Runtime.getRuntime().exec("su", null,null);

            String fileName = new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss'.png'").format(new Date());
            String fullPath = "/sdcard/SS_Service/" + fileName;
            OutputStream os = sh.getOutputStream();
            os.write(("/system/bin/screencap -p " + fullPath).getBytes("ASCII"));
            os.flush();
            os.close();
            sh.waitFor();

            vibrator.vibrate(500);
            Log.v(TAG, "Screenshot taken: " + fullPath);
            return fullPath;
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Error while taking screenshot", e);
        }

        return null;
    }

    private void postProcessScreenShot(String path, Rect rect, int rotate) {
        Bitmap bMap = BitmapFactory.decodeFile(path);

        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        Bitmap cropped = Bitmap.createBitmap(bMap, rect.left, rect.top, rect.width(), rect.height(), matrix, true);

        try (FileOutputStream out = new FileOutputStream(path)) {
            cropped.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (FileNotFoundException e) {
            // should never happened
            Log.e(TAG, "", e);
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (container != null)
            windowManager.removeView(container);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
}