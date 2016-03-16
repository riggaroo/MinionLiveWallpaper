package za.co.riggaroo.minionlivewallpaper.wallpaperservice;

import android.opengl.GLSurfaceView;

/**
 * Created by rebecca on 2014/09/27.
 */
public class MinionWallpaperService extends OpenGLES2WallpaperService {
    @Override
    GLSurfaceView.Renderer getNewRenderer() {
        return new MinionRenderer(getApplicationContext());
    }
}
