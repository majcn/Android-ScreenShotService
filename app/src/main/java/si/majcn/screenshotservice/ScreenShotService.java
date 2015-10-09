package si.majcn.screenshotservice;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ScreenShotService extends Service {

    private static final String TAG = "ScreenShot::Service";

    private WindowManager windowManager;
    private Vibrator vibrator;

    private ServiceWidget serviceWidget;

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        serviceWidget = new ServiceWidget(this);
        serviceWidget.init(windowManager);

        final String pathToWatch = Environment.getExternalStorageDirectory().getPath() + "/Pictures/Screenshots";
        new FileObserver(pathToWatch, FileObserver.CREATE) {
            @Override
            public void onEvent(int event, final String file) {
                Log.d(TAG, "File created [" + pathToWatch + file + "]");

                postProcessScreenShot(file);
            }
        }.startWatching();
    }

    private void postProcessScreenShot(String path) {
        Rect rect = null;
        int rotate = 0;

        int rotation = windowManager.getDefaultDisplay().getRotation();
        if (rotation == Surface.ROTATION_90 && serviceWidget.getState() == ServiceWidget.State.UP) {
            rect = new Rect(563, 19, 1065, 760);
            rotate = 270;
        }

        if (rotation == Surface.ROTATION_90 && serviceWidget.getState() == ServiceWidget.State.DOWN) {
            rect = new Rect(23, 19, 525, 760);
            rotate = 270;
        }

        if (rotation == Surface.ROTATION_270 && serviceWidget.getState() == ServiceWidget.State.UP) {
            rect = new Rect(15, 1160, 517, 1896);
            rotate = 90;
        }

        if (rotation == Surface.ROTATION_270 && serviceWidget.getState() == ServiceWidget.State.DOWN) {
            rect = new Rect(555, 1160, 1057, 1901);
            rotate = 90;
        }

        if (rect == null) {
            return;
        }



        Bitmap bMap = BitmapFactory.decodeFile(path);

        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        Bitmap cropped = Bitmap.createBitmap(bMap, rect.left, rect.top, rect.width(), rect.height(), matrix, true);

        try (FileOutputStream out = new FileOutputStream(path)) {
            cropped.compress(Bitmap.CompressFormat.PNG, 100, out);
            vibrator.vibrate(500);
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
        if (serviceWidget != null) {
            windowManager.removeView(serviceWidget); //added at ServiceWidget.init(windowManager);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
}