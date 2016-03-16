package za.co.riggaroo.minionlivewallpaper.wallpaperservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MinionRenderer implements GLSurfaceView.Renderer {

    public volatile String fragmentShader =

    "precision mediump float;\n"+
    "uniform float time;\n"+
    "uniform vec2 resolution;\n\n" +

    "//palette color\n" +
    "vec3 white  = vec3(1.0,1.0,1.0);\n" +
    "vec3 black  = vec3(0.0,0.0,0.0);\n" +
    "vec3 grey   = vec3(0.4,0.4,0.4);\n" +
    "vec3 yellow = vec3(1.0,0.87,0.13);\n" +
    "vec3 brown  = vec3(89.0,59.0,20.0)/255.0;\n" +
    "vec3 dark   = vec3(47.0,29.0,10.0)/255.0;\n\n" +

    "//draw eye at position\n" +
    "vec3 drawEye(vec3 col, vec2 uv, vec2 position, float blinkOffset) {\n" +
        "\tuv-=position;\n" +
        "\tfloat bl = smoothstep(0.6,1.0,pow(0.5+0.5*cos(time*2.0+blinkOffset),3.0));\n" +
        "\tfloat bp = -1.0+2.0*smoothstep(-0.02,0.02,cos(time+0.5*3.14)+0.1);\n" +
        "\tfloat p1 = length( uv-vec2(0.0,0.65));\n" +
        "\tfloat p2 = length(uv-vec2(0.0,-0.15 - 9.2*bl));\n" +
        "\tfloat p5 = length(uv-vec2(0.0, 1.3 + 9.2*bl));\n" +
        "\tfloat p3 = length( uv-vec2(-0.01+0.1*bp,0.63));\n" +
        "\tfloat p4 = length( uv-vec2(-0.07+0.1*bp,0.65));\n" +
        "\tfloat c1 = smoothstep(0.85,0.86,0.02+p2-bl*9.0);\n" +
        "\tfloat c2 = smoothstep(0.85,0.86,0.02+p5-bl*9.0);\n" +
        "\tfloat c3 = smoothstep(0.04,0.05,p3);\n" +
        "\tfloat c4 = smoothstep(0.09,0.1,p3);\n" +
        "\tfloat c5 = smoothstep(0.06,0.1,p3);\n" +
        "\tfloat c6 = smoothstep(0.24,0.25,p1);\n" +
        "\tfloat c7 = smoothstep(0.34,0.35,p1);\n" +
        "\tfloat c8 = smoothstep(0.015,0.025,p4);\n\n" +

        "\treturn mix(mix(mix(mix(mix(mix(white,mix(mix(black,brown,c3),dark,c5),c8),white,c4),yellow,c1),yellow,c2),grey,c6),col,c7);\n" +
    "}\n\n" +

    "void main(void) {\n" +
        "\tvec2 uv = gl_FragCoord.xy / vec2(resolution.x, resolution.x * 0.5625);\n" +

        "//canvas\n" +
        "\tvec3 col = yellow;\n\n" +

        "if(uv.y > 1.02 && uv.y < 2.01) {\n" +
        "\tfloat i = resolution.y / resolution.x;\n" +
        "\tuv.x *= i;\n\n" +

        "//ribbon\n" +
        "\tfloat r = smoothstep(0.57,0.58,uv.y - 1.0)-smoothstep(0.72,0.73,uv.y - 1.0);\n" +
        "\tcol = mix(col,black,r);\n\n" +

        "//left eye\n" +
        "\tcol = drawEye(col, uv, vec2(0.33*i,1.0), 0.0);\n\n" +

        "//right eye\n" +
        "\tcol = drawEye(col, uv, vec2(0.67*i,1.0), 0.05);\n\n" +

        "//mouth\n" +
        "\tfloat bm = -0.1*smoothstep(0.5,1.0,pow(0.5+0.5*cos(time*2.0+2.0*3.14),8.0));\n" +
        "\tfloat p9 = length(vec2(uv.x, uv.y - 1.0)-vec2(0.5*i,0.9+0.8*bm));\n" +
        "\tfloat m  = atan(uv.y-0.68-1.0,uv.x-0.52*i);\n" +
        "\tfloat r5 = (smoothstep(0.8+bm,0.81+bm,p9)-smoothstep(0.86+bm,0.87+bm,p9))*(1.0-smoothstep((-0.75+0.2*bm)*3.14,(-0.755+0.2*bm)*3.14,m)- smoothstep((-0.255-0.2*bm)*3.14,(-0.25-0.2*bm)*3.14,m));\n\n" +

        "\tcol = mix(col, black, r5);\n" +
        "\tgl_FragColor = vec4(col, 1.0);\n" +
        "} else {\n" +
        "\tdiscard;\n" +
        "}" +
    "}";

	private static final String vertexShader =
		"attribute vec2 position;"+
		"void main()"+
		"{"+
			"gl_Position = vec4( position, 0.0, 1.0 );"+
		"}";
    private int program = 0;
	private int timeLoc;
	private int positionLoc;
    private int resolutionLoc;
	private final float resolution[] = new float[]{0, 0};
    private float speedFactor = 1.0f;
	private float counterValue = (1.0f / GLWallpaperService.GLEngine.FRAME_RATE) * speedFactor;
    private float timeCounter = -1.0f;
    private Context context;
    private ByteBuffer vertexBuffer;

    public MinionRenderer(Context context) {
        this.context = context;

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int hour = intent.getIntExtra("hour", 0);

                if(hour != -1) {
                    speedFactor = hour * 0.05f;
                    counterValue = (1.0f / GLWallpaperService.GLEngine.FRAME_RATE) * speedFactor;
                } else {
                    timeCounter = 0.0f;
                    counterValue = 0.0f;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("update_hour");

        context.registerReceiver(broadcastReceiver, filter);
    }

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		final byte screenCoords[] = {-1, 1, -1, -1, 1, 1, 1, -1};
        vertexBuffer = ByteBuffer.allocateDirect(8);
		vertexBuffer.put(screenCoords).position(0);

		if(fragmentShader != null) {
			loadProgram();
		}

        GLES20.glClearColor(1.0f, 0.87f, 0.13f, 1.0f);
	}

	@Override
	public void onDrawFrame(GL10 gl) {

        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(program);
        GLES20.glEnableVertexAttribArray(positionLoc);
        GLES20.glVertexAttribPointer(positionLoc, 2, GLES20.GL_BYTE, false, 0, vertexBuffer);

        if(timeCounter > 1.0f || timeCounter < -1.0f) {
            counterValue = -counterValue;
        }

		timeCounter += counterValue;

		if(timeLoc > -1) {
            GLES20.glUniform1f(timeLoc, timeCounter);
        }

        if(resolutionLoc > -1) {
            GLES20.glUniform2fv(resolutionLoc, 1, resolution, 0);
        }

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(positionLoc);
    }

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
        int orientation = context.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE){
            resolution[1] = width;
            resolution[0] = height;
        } else {
            resolution[0] = width;
            resolution[1] = height;
        }
	}

	private void loadProgram()
	{
		if(program != 0) {
            GLES20.glDeleteProgram(program);
        }

		if((program = Shader.loadProgram(vertexShader, fragmentShader)) == 0) {
			return;
		}

		positionLoc = GLES20.glGetAttribLocation(program, "position" );
		timeLoc = GLES20.glGetUniformLocation(program, "time" );
        resolutionLoc = GLES20.glGetUniformLocation(program, "resolution" );
	}
}
