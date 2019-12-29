package cc.lym.Renderer;

import android.opengl.GLES31;
import android.opengl.Matrix;
import android.util.Log;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;

import cc.lym.leap.Hand;
import cc.lym.leap.LeapReceiver;

public class HandRenderer implements HeadlessRenderer {
	private static final String LOG_TAG="HandRenderer";
	private final LeapReceiver receiver;
	private Hand hand;
	private static final FloatBuffer uvBuffer;
	private static final IntBuffer indexBuffer;
	
	static
	{
		uvBuffer=ByteBuffer.allocateDirect(4*2*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		uvBuffer.put(new float[]{-0.5f,-0.6f,0.5f,-0.6f,0.5f,0.6f,-0.5f,0.6f});
		uvBuffer.rewind();
		indexBuffer=ByteBuffer.allocateDirect(6*4).order(ByteOrder.nativeOrder()).asIntBuffer();
		indexBuffer.put(new int[]{1,2,0,3,1,2});
		indexBuffer.rewind();
	}
	private long lastGet,lastPut;
	private boolean open=false;
	public HandRenderer()
	{
		lastGet=lastPut=System.nanoTime();
		receiver=new LeapReceiver(()->{lastGet=System.nanoTime();},()->{lastPut=System.nanoTime();},()->{open=true;},()->{open=false;});
	}
	
	private static final String vertexShaderSourceCode= "" +
			Renderer.SHADER_VERSION_DIRECTIVE +
			"" +
			"precision highp int;" +
			"precision highp float;" +
			"" +
			"uniform mat4 u_transform;" +
			"uniform mat3 u_center_right_forward;" +
			"" +
			"in vec2 a_UV;" +
			"out vec2 v_UV;" +
			"" +
			"void main()" +
			"{" +
			"	gl_Position=u_transform*vec4(u_center_right_forward*vec3(0.001,a_UV*0.05),1.0);" +
			"	v_UV=a_UV;" +
			"}" +
			"";
	private static final String fragmentShaderSourceCode= "" +
			Renderer.SHADER_VERSION_DIRECTIVE +
			"" +
			"precision highp int;" +
			"precision highp float;" +
			"" +
			"in vec2 v_UV;" +
			"uniform vec4 u_norm;" +
			"out vec4 out_FragColor;" +
			"" +
			"void main()" +
			"{" +
			"	if(abs((v_UV.x)*(v_UV.y))>0.015)discard;" +
			"	float fac=abs(u_norm.y);" +
			"	if(fac<0.0)fac=0.0;" +
			"	fac=fac+0.15;" +
			"	vec3 color=vec3(0.93,0.89,0.75)*0.5+vec3(0,1,0)*u_norm.x+vec3(1,0,0)*u_norm.z+vec3(0,0,1)*u_norm.w;" +
			"	out_FragColor=vec4(fac*color,1.0);" +
			"}" +
			"";
	private int glProgram;
	private int glParam_u_transform;
	private int glParam_u_center_right_forward;
	private int glParam_a_UV;
	private int glParam_u_norm;
	private final float[]headTrans=new float[16];
	private float alpha=0,beta=0;
	public float[]getAlphaBeta(){return new float[]{alpha,beta};}
	
	@Override
	public void onNewFrame(HeadTransform headTransform)
	{
		hand=receiver.getHand();
		headTransform.getHeadView(headTrans,0);
	}
	
	@Override
	public void onDrawEye(Eye eye)
	{
		{
			GLES31.glUseProgram(glProgram);
			Renderer.checkGlError();
			
			float[]headInv=new float[16];
			Matrix.invertM(headInv,0,headTrans,0);
			float[]tmp=new float[16];
			Matrix.multiplyMM(tmp,0,eye.getEyeView(),0,headInv,0);
			float[]head2eye=new float[16];
			Matrix.multiplyMM(head2eye,0,tmp,0,new float[]{-1,0,0,0,0,0,-1,0,0,-1,0,0,0,0,0,1},0);
			float[]perspective=new float[16];
			Matrix.multiplyMM(perspective,0,eye.getPerspective(0.05f,250.0f),0,head2eye,0);
			float[]center_right_forward,norm;
			if(hand!=null&&hand.isPresent)
			{
				float rightx=hand.palmNormY*hand.palmDirectionZ-hand.palmNormZ*hand.palmDirectionY;
				float righty=hand.palmNormZ*hand.palmDirectionX-hand.palmNormX*hand.palmDirectionZ;
				float rightz=hand.palmNormX*hand.palmDirectionY-hand.palmNormY*hand.palmDirectionX;
				center_right_forward=new float[]{hand.palmPosX,hand.palmPosY,hand.palmPosZ,rightx,righty,rightz,hand.palmDirectionX,hand.palmDirectionY,hand.palmDirectionZ};
				
				norm=new float[]{(float)Math.exp((System.nanoTime()-lastGet)/-3e8),hand.palmNormY,(float)Math.exp((System.nanoTime()-lastPut)/-3e8),open?1:0};
			}
			else
			{
				center_right_forward=new float[]{0,340,0,-1,0,0,0,0,-1};
				norm=new float[]{0,1,0,0};
			}
			alpha=-center_right_forward[0]/center_right_forward[1];
			beta=-center_right_forward[2]/center_right_forward[1];
			GLES31.glUniformMatrix4fv(glParam_u_transform,1,false,perspective,0);
			GLES31.glUniformMatrix3fv(glParam_u_center_right_forward,1,false,center_right_forward,0);
			GLES31.glUniform4fv(glParam_u_norm,1,norm,0);
			Renderer.checkGlError();
			GLES31.glEnableVertexAttribArray(glParam_a_UV);
			Renderer.checkGlError();
			
			GLES31.glVertexAttribPointer(glParam_a_UV,2, GLES31.GL_FLOAT,false,0,uvBuffer);
			Renderer.checkGlError();
			GLES31.glDrawElements(GLES31.GL_TRIANGLE_STRIP,indexBuffer.limit(),GLES31.GL_UNSIGNED_INT,indexBuffer);
			Renderer.checkGlError();
		}
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
		glParam_u_transform=GLES31.glGetUniformLocation(glProgram,"u_transform");
		glParam_u_center_right_forward=GLES31.glGetUniformLocation(glProgram,"u_center_right_forward");
		glParam_u_norm=GLES31.glGetUniformLocation(glProgram,"u_norm");
		glParam_a_UV=GLES31.glGetAttribLocation(glProgram,"a_UV");
		Renderer.checkGlError();
	}
	
	@Override public void onFinishFrame(Viewport viewport) {}
	@Override public void onSurfaceChanged(int width, int height) {}
	@Override public void onRendererShutdown() {}
}
