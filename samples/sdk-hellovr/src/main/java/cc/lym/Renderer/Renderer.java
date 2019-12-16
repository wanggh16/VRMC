package cc.lym.Renderer;

import android.opengl.GLES31;
import android.opengl.GLU;
import android.util.Log;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import cc.lym.util.BiConsumer;
import cc.lym.util.Consumer;

import javax.microedition.khronos.egl.EGLConfig;

/**
 * Usage:
 * GvrView.StereoRenderer renderer = Renderer.base().andThen(new BlockRenderer(...)).andThen(new EntityRenderer(...));
 */
public class Renderer implements GvrView.StereoRenderer {
	final static String SHADER_VERSION_DIRECTIVE="#version 310 es\n";
	
	private final Consumer<HeadTransform> newFrame;
	private final Consumer<Eye> drawEye;
	private final Consumer<Viewport> finishFrame;
	private final BiConsumer<Integer,Integer> surfaceChanged;
	private final Consumer<EGLConfig> surfaceCreated;
	private final Runnable rendererShutdown;
	
	private final static Renderer BASE=new Renderer();
	private Renderer()
	{
		this.newFrame=x->{};
		this.drawEye=x->
		{
			GLES31.glEnable(GLES31.GL_DEPTH_TEST);
			GLES31.glEnable(GLES31.GL_CULL_FACE);
			GLES31.glCullFace(GLES31.GL_BACK);
			GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT | GLES31.GL_DEPTH_BUFFER_BIT);
			checkGlError();
		};
		this.finishFrame=x->{};
		this.surfaceChanged=(x,y)->{};
		this.surfaceCreated=x->{GLES31.glClearColor(0.7f, 0.7f, 0.95f, 0.0f);};
		this.rendererShutdown=()->{};
	}
	private Renderer(Consumer<HeadTransform> newFrame,Consumer<Eye> drawEye,Consumer<Viewport> finishFrame,
					 BiConsumer<Integer,Integer> surfaceChanged,Consumer<EGLConfig> surfaceCreated,Runnable rendererShutdown)
	{
		this.newFrame=newFrame;this.drawEye=drawEye;this.finishFrame=finishFrame;
		this.surfaceChanged=surfaceChanged;this.surfaceCreated=surfaceCreated;this.rendererShutdown=rendererShutdown;
	}
	
	static int counter=0;
	static void checkGlError()
	{
		int error;
		if((error=GLES31.glGetError())!=GLES31.GL_NO_ERROR)
		{
			Log.e("opengl", GLU.gluErrorString(error),new RuntimeException());
			counter++;
			if(counter>30)
				throw new RuntimeException();
		}
	}
	static int loadShader(int type,String shaderCode)
	{//build a shader, from code of developer.android.com/training/graphics/opengl/draw
		int shader = GLES31.glCreateShader(type);
		GLES31.glShaderSource(shader,shaderCode);
		GLES31.glCompileShader(shader);
		int[]tmp=new int[1];
		GLES31.glGetShaderiv(shader,GLES31.GL_COMPILE_STATUS,tmp,0);
		if(tmp[0]!=GLES31.GL_TRUE)
		{
			Log.e("opengl","comile error: "+GLES31.glGetShaderInfoLog(shader));
			throw new RuntimeException(GLES31.glGetShaderInfoLog(shader));
		}
		checkGlError();
		return shader;
	}
	
	public static Renderer base(){return BASE;}
	public Renderer andThen(HeadlessRenderer other)
	{
		return new Renderer(
				newFrame.andThen(x->other.onNewFrame(x)),
				drawEye.andThen(x->other.onDrawEye(x)),
				finishFrame.andThen(x->other.onFinishFrame(x)),
				surfaceChanged.andThen((x,y)->other.onSurfaceChanged(x,y)),
				surfaceCreated.andThen(x->other.onSurfaceCreated(x)),
				()->{rendererShutdown.run();other.onRendererShutdown();}
		);
	}
	
	@Override public void onNewFrame(HeadTransform headTransform) {newFrame.accept(headTransform);}
	@Override public void onDrawEye(Eye eye) {drawEye.accept(eye);}
	@Override public void onFinishFrame(Viewport viewport) {finishFrame.accept(viewport);}
	@Override public void onSurfaceChanged(int width, int height) {surfaceChanged.accept(width,height);}
	@Override public void onSurfaceCreated(EGLConfig eglConfig) {surfaceCreated.accept(eglConfig);}
	@Override public void onRendererShutdown() {rendererShutdown.run();}
}
