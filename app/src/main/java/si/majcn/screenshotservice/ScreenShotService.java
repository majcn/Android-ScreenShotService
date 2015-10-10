package si.majcn.screenshotservice;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ScreenShotService extends Service {

    private static final String TAG = "ScreenShot::Service";

    private WindowManager windowManager;
    private Vibrator vibrator;

    private ServiceWidget serviceWidget;

    private FileObserver mFileObserver;

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        serviceWidget = new ServiceWidget(this);
        serviceWidget.init(windowManager);

        final String pathToWatch = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/Screenshots/";
        mFileObserver = new FileObserver(pathToWatch, FileObserver.CREATE) {
            @Override
            public void onEvent(int event, final String file) {
                Log.d(TAG, "File created [" + pathToWatch + file + "]");

                postProcessScreenShot(pathToWatch + file);
            }
        };
        mFileObserver.startWatching();
    }

    private Rect getPostProcessScreenShotCropRect() {
        switch (serviceWidget.getState()) {
            case UP:
                return new Rect(19, 15, 760, 517);
            case DOWN:
                return new Rect(19, 555, 760, 1057);
            default:
                return null;
        }
    }

    private void postProcessScreenShot(String path) {
        Rect rect = getPostProcessScreenShotCropRect();
        if (rect == null) {
            return;
        }

        try {
            Thread.sleep(1000); // TODO: hack - wait 1sec to ensure file is created...
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Bitmap bMap = BitmapFactory.decodeFile(path);
        Bitmap cropped = Bitmap.createBitmap(bMap, rect.left, rect.top, rect.width(), rect.height());

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
        mFileObserver.stopWatching();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
}