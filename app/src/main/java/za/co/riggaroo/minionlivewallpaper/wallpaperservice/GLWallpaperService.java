package za.co.riggaroo.minionlivewallpaper.wallpaperservice;

import android.content.Context;

import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import za.co.riggaroo.minionlivewallpaper.LoggerConfig;


/**
 * Created by rebecca on 2014/09/27.
 */
public abstract class GLWallpaperService extends WallpaperService {

    public class GLEngine extends Engine {

        class WallpaperGLSurfaceView extends GLSurfaceView {
            private static final String TAG = "WallpaperGLSurfaceView";

            WallpaperGLSurfaceView(Context context) {
                super(context);

                if (LoggerConfig.ON) {
                    Log.d(TAG, "WallpaperGLSurfaceView(" + context + ")");
                }
            }

            @Override
            public SurfaceHolder getHolder() {
                if (LoggerConfig.ON) {
                    Log.d(TAG, "getHolder(): returning " + getSurfaceHolder());
                }

                Calendar c = Calendar.getInstance();
                int hour = c.get(Calendar.HOUR_OF_DAY);

                if((hour > 21 && hour < 24) || hour < 6) {
                    hour = -1;
                }

                if(hour > 14) {
                    hour = 24 - hour;
                }

                Intent intent = new Intent("update_hour");
                intent.putExtra("hour", hour);
                sendBroadcast(intent);

                return getSurfaceHolder();
            }

            public void onDestroy() {
                if (LoggerConfig.ON) {
                    Log.d(TAG, "onDestroy()");
                }

                super.onDetachedFromWindow();
            }
        }

        private static final String TAG = "GLEngine";
        public static final long FRAME_RATE = 24;

        private WallpaperGLSurfaceView glSurfaceView;
        private boolean rendererHasBeenSet;
        private Timer timer;
        private TimerTask timerTask;
        private long timerDelay = 0;
        private long timerPeriod = 1000 / FRAME_RATE;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            if (LoggerConfig.ON) {
                Log.d(TAG, "onCreate(" + surfaceHolder + ")");
            }

            super.onCreate(surfaceHolder);

            glSurfaceView = new WallpaperGLSurfaceView(GLWallpaperService.this);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (LoggerConfig.ON) {
                Log.d(TAG, "onVisibilityChanged(" + visible + ")");
            }

            super.onVisibilityChanged(visible);

            if (rendererHasBeenSet) {
                if (visible) {

                    if(timer == null) {
                        timerTask = new TimerTask() {
                            @Override
                            public void run() {
                                if(rendererHasBeenSet) {
                                    glSurfaceView.requestRender();
                                }
                            }
                        };
                        timer = new Timer();
                        timer.scheduleAtFixedRate(timerTask, timerDelay, timerPeriod);
                    }
                    glSurfaceView.onResume();
                } else {
                    if(timer != null) {
                        timer.cancel();
                        timer = null;
                        timerTask = null;
                    }
                    glSurfaceView.onPause();
                }
            }
        }

        @Override
        public void onDestroy() {
            if (LoggerConfig.ON) {
                Log.d(TAG, "onDestroy()");
            }

            super.onDestroy();
            glSurfaceView.onDestroy();
        }

        protected void setRenderer(GLSurfaceView.Renderer renderer) {
            if (LoggerConfig.ON) {
                Log.d(TAG, "setRenderer(" + renderer + ")");
            }

            glSurfaceView.setRenderer(renderer);
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            rendererHasBeenSet = true;
        }

        protected void setPreserveEGLContextOnPause(boolean preserve) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                if (LoggerConfig.ON) {
                    Log.d(TAG, "setPreserveEGLContextOnPause(" + preserve + ")");
                }

                glSurfaceView.setPreserveEGLContextOnPause(preserve);
            }
        }

        protected void setEGLContextClientVersion(int version) {
            if (LoggerConfig.ON) {
                Log.d(TAG, "setEGLContextClientVersion(" + version + ")");
            }

            glSurfaceView.setEGLContextClientVersion(version);
        }
    }
}
