package cc.lym.Renderer;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLES31;
import android.opengl.GLUtils;
import android.util.Log;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;

/**
 * Renderer that renders the overlay
 * the image will be displayed over the whole viewport, pixel with alpha<0.5 will be considered totally transparent, others considered totally non-transparent
 */
public class OverlayRenderer implements HeadlessRenderer {
	private final static String LOG_TAG="OverlayRenderer";
	
	private FloatBuffer PositionBuffer;
	private FloatBuffer UVBuffer;
	private IntBuffer indexBuffer;
	
	private int glProgram;
	private int glParam_a_Position;
	private int glParam_a_UV;
	private int[]textureId=new int[1];
	
	private Bitmap texture;
	private boolean changed;
	
	public OverlayRenderer(Bitmap content)
	{
		texture=content.copy(content.getConfig(),false);
		float[]positions={-1,-1, 1,-1, 1,1, -1,-1, 1,1, -1,1};
		float[]uv={0,1,1,1,1,0,0,1,1,0,0,0};
		int[]indices={0,1,2,3,4,5};
		PositionBuffer=ByteBuffer.allocateDirect(4*positions.length).order(ByteOrder.nativeOrder()).asFloatBuffer();PositionBuffer.put(positions);PositionBuffer.rewind();
		UVBuffer=ByteBuffer.allocateDirect(4*uv.length).order(ByteOrder.nativeOrder()).asFloatBuffer();UVBuffer.put(uv);UVBuffer.rewind();
		indexBuffer=ByteBuffer.allocateDirect(4*indices.length).order(ByteOrder.nativeOrder()).asIntBuffer();indexBuffer.put(indices);indexBuffer.rewind();
	}
	public synchronized void changeContent(Bitmap content)
	{
		texture=content.copy(content.getConfig(),false);
		changed=true;
	}
	
	private static final String vertexShaderSourceCode= "" +
			Renderer.SHADER_VERSION_DIRECTIVE +
			"in vec2 a_Position;" +
			"in vec2 a_UV;" +
			"out vec2 v_UV;" +
			"" +
			"void main()" +
			"{" +
			"	gl_Position=vec4(a_Position,0.0,1.0);" +
			"	v_UV=a_UV;" +
			"}" +
			"";
	private static final String fragmentShaderSourceCode= "" +
			Renderer.SHADER_VERSION_DIRECTIVE +
			"precision highp int;" +
			"precision highp float;" +
			"precision highp sampler2D;" +
			"in vec2 v_UV;" +
			"uniform sampler2D u_Texture;" +
			"out vec4 out_FragColor;" +
			"" +
			"void main()" +
			"{" +
			"	vec4 color=texture(u_Texture,vec2(v_UV.x,v_UV.y));" +
			"	if(color.a<0.5)discard;" +
			"	out_FragColor=vec4(color.rgb,1.0);" +
			"}" +
			"";
	
	@Override
	public void onNewFrame(HeadTransform headTransform) {}
	@Override
	public void onDrawEye(Eye eye)
	{
		GLES31.glDepthFunc(GLES31.GL_ALWAYS);
		
		GLES31.glUseProgram(glProgram);
		Renderer.checkGlError();
		
		if(changed)
		synchronized (this)
		{
		if(changed)
		{
			changed=false;
			Log.i(LOG_TAG,"change picture");
			GLES31.glDeleteTextures(1,textureId,0);
			GLES31.glGenTextures(1,textureId,0);
			GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
			GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureId[0]);
			Renderer.checkGlError();
			GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D,GLES31.GL_TEXTURE_WRAP_S,GLES31.GL_CLAMP_TO_EDGE);
			GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D,GLES31.GL_TEXTURE_WRAP_T,GLES31.GL_CLAMP_TO_EDGE);
			GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D,GLES31.GL_TEXTURE_MIN_FILTER,GLES31.GL_LINEAR_MIPMAP_NEAREST);
			GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D,GLES31.GL_TEXTURE_MAG_FILTER,GLES31.GL_NEAREST);
			Renderer.checkGlError();
			GLUtils.texImage2D(GLES31.GL_TEXTURE_2D,0,texture,0);
			GLES31.glGenerateMipmap(GLES31.GL_TEXTURE_2D);
			Renderer.checkGlError();
		}
		}
		
		GLES31.glEnableVertexAttribArray(glParam_a_Position);
		GLES31.glEnableVertexAttribArray(glParam_a_UV);
		Renderer.checkGlError();
		
		GLES31.glVertexAttribPointer(glParam_a_Position,2, GLES20.GL_FLOAT,false,0,PositionBuffer);
		GLES31.glVertexAttribPointer(glParam_a_UV,2, GLES20.GL_FLOAT,false,0,UVBuffer);
		Renderer.checkGlError();
		GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
		GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureId[0]);
		Renderer.checkGlError();
		GLES31.glDrawElements(GLES20.GL_TRIANGLES,indexBuffer.limit(),GLES31.GL_UNSIGNED_INT,indexBuffer);
		Renderer.checkGlError();
		
		GLES31.glDepthFunc(GLES31.GL_LESS);
	}
	@Override
	public void onSurfaceCreated(EGLConfig eglConfig)
	{
		Log.i(LOG_TAG,"onSurfaceCreated");
		int vs=Renderer.loadShader(GLES31.GL_VERTEX_SHADER,vertexShaderSourceCode);
		int fs=Renderer.loadShader(GLES31.GL_FRAGMENT_SHADER,fragmentShaderSourceCode);
		glProgram = GLES31.glCreateProgram();
		GLES31.glAttachShader(glProgram,vs);
		Renderer.checkGlError();
		GLES31.glAttachShader(glProgram,fs);
		Renderer.checkGlError();
		GLES31.glLinkProgram(glProgram);
		int[] linkStatus = new int[1];
		GLES31.glGetProgramiv(glProgram, GLES31.GL_LINK_STATUS, linkStatus, 0);
		if(linkStatus[0]!=GLES31.GL_TRUE)
		{
			Log.e("opengl","link error: "+GLES31.glGetProgramInfoLog(glProgram));
			throw new RuntimeException(GLES31.glGetProgramInfoLog(glProgram));
		}
		Renderer.checkGlError();
		glParam_a_Position=GLES31.glGetAttribLocation(glProgram,"a_Position");
		glParam_a_UV=GLES31.glGetAttribLocation(glProgram,"a_UV");
		Renderer.checkGlError();
		changed=false;
		GLES31.glGenTextures(1,textureId,0);
		GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
		GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureId[0]);
		Renderer.checkGlError();
		GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D,GLES31.GL_TEXTURE_WRAP_S,GLES31.GL_CLAMP_TO_EDGE);
		GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D,GLES31.GL_TEXTURE_WRAP_T,GLES31.GL_CLAMP_TO_EDGE);
		GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D,GLES31.GL_TEXTURE_MIN_FILTER,GLES31.GL_LINEAR_MIPMAP_NEAREST);
		GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D,GLES31.GL_TEXTURE_MAG_FILTER,GLES31.GL_NEAREST);
		Renderer.checkGlError();
		GLUtils.texImage2D(GLES31.GL_TEXTURE_2D,0,texture,0);
		GLES31.glGenerateMipmap(GLES31.GL_TEXTURE_2D);
		Renderer.checkGlError();
	}
	@Override public void onFinishFrame(Viewport viewport) {}
	@Override public void onSurfaceChanged(int i, int i1) {}
	@Override public void onRendererShutdown() {}
}
