package cc.lym.Renderer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES31;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;

import cc.lym.util.Consumer;
import cc.lym.util.Location;
import cc.lym.util.Supplier;
import cc.lym.util.Util;

public class EntityRenderer implements HeadlessRenderer {
	private final static String LOG_TAG="EntityRenderer";
	private final int entityCount;
	private final int rectPerEntity;
	private final byte[]enabled;
	private final float[]parameters;
	private final float[]illu;
	private final int attribCount;
	private final long xMin,xMax,yMin,yMax,zMin,zMax;
	private final Bitmap texture;
	private final Supplier<Location> locationSupplier;
	
	private static class BufferGroup
	{
		final ByteBuffer enabled;
		final FloatBuffer parameters;
		final FloatBuffer illu;
		BufferGroup(int maxInstanceCount,int rectPerEntity,int attributeCount)
		{
			enabled=ByteBuffer.allocateDirect(maxInstanceCount*rectPerEntity*4*1);
			parameters=ByteBuffer.allocateDirect(maxInstanceCount*rectPerEntity*4*(attributeCount-1)*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
			illu=ByteBuffer.allocateDirect(maxInstanceCount*rectPerEntity*4*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		}
	}
	private BufferGroup mainBuffer,idleBuffer;
	private FloatBuffer uv;
	private ByteBuffer pointType;
	private IntBuffer indexBuffer;
	
	public EntityRenderer(long xMin, long xMax, long yMin, long yMax, long zMin, long zMax,
						  Supplier<Location>locationSupplier, Model model, int maxInstanceCount, byte[] texture)
	{
		this.xMin=xMin;this.xMax=xMax;this.yMin=yMin;this.yMax=yMax;this.zMin=zMin;this.zMax=zMax;
		this.sceneSize[0]=(xMax-xMin);this.sceneSize[1]=(yMax-yMin);this.sceneSize[2]=(zMax-zMin);
		if(xMin>xMax||yMin>yMax||zMin>zMax)
		{
			Log.e(LOG_TAG,String.format("illegal range x%d~%d y%d~%d z%d~%d",xMin,xMax,yMin,yMax,zMin,zMax));
			throw new IllegalArgumentException();
		}
		this.locationSupplier=locationSupplier;
		
		attribCount=model.attribCount;
		glParam_a_param=new int[attribCount];
		this.texture=BitmapFactory.decodeByteArray(texture,0,texture.length);
		entityCount=maxInstanceCount;
		rectPerEntity=model.rectangles.length;
		
		enabled=new byte[entityCount];
		parameters=new float[entityCount*(attribCount-1)];
		illu=new float[entityCount];
		
		mainBuffer=new BufferGroup(entityCount,rectPerEntity,attribCount);
		idleBuffer=new BufferGroup(entityCount,rectPerEntity,attribCount);
		uv=ByteBuffer.allocateDirect(entityCount*rectPerEntity*4*2*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		pointType=ByteBuffer.allocateDirect(entityCount*rectPerEntity*4*1);
		indexBuffer=ByteBuffer.allocateDirect(entityCount*rectPerEntity*6*4).order(ByteOrder.nativeOrder()).asIntBuffer();
		int counter=0;
		for(int i=0;i<entityCount;i++)
		{
			for(Model.Rectangle rect:model.rectangles)
			{
				uv.put(rect.tex_left/(float)this.texture.getWidth());uv.put(rect.tex_top/(float)this.texture.getHeight());
				uv.put((rect.tex_left+rect.tex_width)/(float)this.texture.getWidth());uv.put(rect.tex_top/(float)this.texture.getHeight());
				uv.put((rect.tex_left+rect.tex_width)/(float)this.texture.getWidth());uv.put((rect.tex_top+rect.tex_height)/(float)this.texture.getHeight());
				uv.put(rect.tex_left/(float)this.texture.getWidth());uv.put((rect.tex_top+rect.tex_height)/(float)this.texture.getHeight());
				for(Model.Point point:new Model.Point[]{rect.left_down,rect.right_down,rect.right_top,rect.left_top})
					pointType.put((byte)(int)model.points.get(point));
				indexBuffer.put(counter+0);
				indexBuffer.put(counter+1);
				indexBuffer.put(counter+2);
				indexBuffer.put(counter+0);
				indexBuffer.put(counter+2);
				indexBuffer.put(counter+3);
				counter+=4;
			}
		}
		uv.rewind();pointType.rewind();indexBuffer.rewind();
		
		StringBuilder rendererCode=new StringBuilder(vertexShaderSourceCodeHead);
		rendererCode.append("\n");
		for(int i=1;i<model.attribCount;i++)
			rendererCode.append("in float param_").append(i).append(";\n");
		rendererCode.append(vertexShaderSourceCodeMiddle).append("\n");
		rendererCode.append("switch(pointType)\n{\n");
		for(Map.Entry<Model.Point,Integer> point:model.points.entrySet())
			rendererCode.append("case ").append((int)point.getValue()).append(": rel=").append(point.getKey().repr()).append(";break;\n");
		//rendererCode.append("default: while(1==1);\n");
		rendererCode.append("}\n");
		rendererCode.append("vec3 base=vec3(").append(model.x.repr()).append(",").append(model.y.repr()).append(",").append(model.z.repr()).append(");\n");
		rendererCode.append("float theta=").append(model.theta.repr()).append(";\n");
		rendererCode.append(vertexShaderSourceCodeTail);
		vertexShaderSourceCode=rendererCode.toString();
		Log.i(LOG_TAG,vertexShaderSourceCode);
		new DaemonThread().start();
		Log.i(LOG_TAG,this.hashCode()+" created at:",new Exception("everything OK"));
	}
	private synchronized void update()
	{
		Log.i(LOG_TAG,"updating buffers");
		synchronized (idleBuffer)
		{
			ByteBuffer enabled=idleBuffer.enabled;//i*r*4
			FloatBuffer parameters=idleBuffer.parameters;//i*r*4*(a-1)
			FloatBuffer illu=idleBuffer.illu;//i*r*4
			
			for(int i=0;i<entityCount;i++)
				for(int j=0;j<rectPerEntity*4;j++)
					enabled.put(this.enabled[i]);
			for(int i=0;i<entityCount;i++)
				for(int j=0;j<rectPerEntity*4;j++)
					illu.put(this.illu[i]);
			for(int i=0;i<entityCount;i++)
				for(int j=0;j<rectPerEntity*4;j++)
					for(int k=1;k<attribCount;k++)
						parameters.put(this.parameters[i*(attribCount-1)+k-1]);
			
			enabled.rewind();parameters.rewind();illu.rewind();
		}
		Log.i(LOG_TAG,"committing updated buffers");
		BufferGroup group=mainBuffer;
		mainBuffer=idleBuffer;
		idleBuffer=group;
		Log.i(LOG_TAG,"updated buffers committed");
	}
	private Semaphore pendingModification=new Semaphore(0);
	public synchronized void setEntityAttrib(int index,float[]entity)
	{
		if(entity.length!=attribCount-1)
			throw new IllegalArgumentException();
		System.arraycopy(entity,0,parameters,index*(attribCount-1),attribCount-1);
		Log.i(LOG_TAG,this.hashCode()+" set entity attribute "+index);
		pendingModification.release();
	}
	public synchronized void setEntityIllumination(int index,float illumination)
	{
		if(illumination<0||illumination>1)
			throw new IllegalArgumentException();
		illu[index]=illumination;
		Log.i(LOG_TAG,this.hashCode()+" set entity illumination "+index);
		pendingModification.release();
	}
	public synchronized void enableEntity(int index)
	{
		enabled[index]=1;
		Log.i(LOG_TAG,this.hashCode()+" enable entity "+index);
		pendingModification.release();
	}
	public synchronized void disableEntity(int index)
	{
		enabled[index]=0;
		Log.i(LOG_TAG,this.hashCode()+" disable entity "+index);
		pendingModification.release();
	}
	private class DaemonThread extends Thread
	{
		@Override public void run()
		{
			while(true)
			{
				pendingModification.acquireUninterruptibly();
				try
				{
					long deadline=System.nanoTime()+3000000;
					while(System.nanoTime()<deadline&&pendingModification.tryAcquire(300,TimeUnit.MICROSECONDS))
						;
					if(System.nanoTime()>=deadline)
						Log.w(LOG_TAG,"daemon is too busy "+pendingModification.availablePermits());
				}catch(InterruptedException ignored){}
				update();
			}
		}
	}
	
	private int glProgram;
	private int glParam_u_transform;
	private int glParam_u_LowerBound;
	private int glParam_u_SceneSize;
	private int glParam_a_UV;
	private int glParam_a_illu;
	private final int[]glParam_a_param;
	private int glParam_u_param0;
	private int glParam_pointType;
	private int glParam_enabled;
	private int[]textureId=new int[1];
	private static final String vertexShaderSourceCodeHead= "" +
			Renderer.SHADER_VERSION_DIRECTIVE +
			"" +
			"precision highp int;" +
			"precision highp float;" +
			"" +
			"uniform mat4 u_transform;" +
			"uniform vec3 u_LowerBound;" +
			"uniform vec3 u_SceneSize;" +
			"" +
			"in vec2 a_UV;" +
			"out vec2 v_UV;" +
			"in float a_illu;" +
			"out float v_illu;" +
			"uniform float param_0;" +//time
			"in int pointType;" +
			"in int enabled;" +
			"";
	private static final String vertexShaderSourceCodeMiddle= "" +
			"" +
			"void main()" +
			"{" +
			"	if(enabled==0)gl_Position=vec4(5,5,5,1);" +
			"	else" +
			"{" +
			"	vec3 rel;";
	private static final String vertexShaderSourceCodeTail= "" +
			"	base=mod(base-u_LowerBound,u_SceneSize)+u_LowerBound;" +
			"	vec3 pos=vec3(rel.x*cos(theta)-rel.y*sin(theta),rel.x*sin(theta)+rel.y*cos(theta),rel.z);" +
			"" +
			"	gl_Position=u_transform*vec4(base+pos,1.0);" +
			"	v_UV=a_UV;" +
			"	v_illu=a_illu;" +
			"}" +
			"}" +
			"";
	private final String vertexShaderSourceCode;
	private static final String fragmentShaderSourceCode= "" +
			Renderer.SHADER_VERSION_DIRECTIVE +
			"" +
			"precision highp int;" +
			"precision highp float;" +
			"precision highp sampler2D;" +
			"" +
			"in vec2 v_UV;" +
			"in float v_illu;" +
			"uniform sampler2D u_Texture;" +
			"out vec4 out_FragColor;" +
			"" +
			"void main()" +
			"{" +
			"	vec4 color=texture(u_Texture,vec2(v_UV.x,v_UV.y));" +
			"	if(color.a<0.5)discard;" +
			"	out_FragColor=vec4(color.rgb*v_illu,1.0);" +
			"}" +
			"";
	
	private Location location=new Location(0,0,0);
	private final float[]headTrans=new float[16];
	private final float[]lowerBound=new float[3];
	private final float[]sceneSize=new float[3];
	
	@Override
	public void onNewFrame(HeadTransform headTransform)
	{
		Location tmp=locationSupplier.get();
		location=new Location(Util.fold(tmp.x,xMin,xMax), Util.fold(tmp.y,yMin,yMax), Util.fold(tmp.z,zMin,zMax));//fold
		lowerBound[0]=(float)(location.x-sceneSize[0]/2);lowerBound[1]=(float)(location.y-sceneSize[1]/2);lowerBound[2]=(float)(location.z-sceneSize[2]/2);
		headTransform.getHeadView(headTrans,0);
	}
	
	@Override
	public void onDrawEye(Eye eye)
	{
		GLES31.glUseProgram(glProgram);
		Renderer.checkGlError();
		
		float[]head2eye=new float[16];
		Matrix.multiplyMM(head2eye,0,eye.getEyeView(),0,new float[]{0,0,-1,0,-1,0,0,0,0,1,0,0,(float)location.y,-(float)location.z,(float)location.x,1},0);
		float[]perspective=new float[16];
		Matrix.multiplyMM(perspective,0,eye.getPerspective(0.05f,250.0f),0,head2eye,0);
		
		GLES31.glUniformMatrix4fv(glParam_u_transform,1,false,perspective,0);
		GLES31.glUniform3fv(glParam_u_LowerBound,1,lowerBound,0);
		GLES31.glUniform3fv(glParam_u_SceneSize,1,sceneSize,0);
		GLES31.glUniform1f(glParam_u_param0,System.nanoTime());
		Renderer.checkGlError();
		GLES31.glEnableVertexAttribArray(glParam_a_UV);
		GLES31.glEnableVertexAttribArray(glParam_a_illu);
		for(int i=1;i<attribCount;i++)
			GLES31.glEnableVertexAttribArray(glParam_a_param[i]);
		GLES31.glEnableVertexAttribArray(glParam_pointType);
		GLES31.glEnableVertexAttribArray(glParam_enabled);
		Renderer.checkGlError();
		
		BufferGroup buffers=mainBuffer;
		
		synchronized (buffers)
		{
			GLES31.glVertexAttribPointer(glParam_a_UV,2, GLES31.GL_FLOAT,false,0,uv);
			GLES31.glVertexAttribPointer(glParam_a_illu,1, GLES31.GL_FLOAT,false,0,buffers.illu);
			for(int i=1;i<attribCount;i++)
			{
				buffers.parameters.position(i-1);
				GLES31.glVertexAttribPointer(glParam_a_param[i],1,GLES31.GL_FLOAT,false,(attribCount-1)*4,buffers.parameters);
			}
			buffers.parameters.rewind();
			GLES31.glVertexAttribIPointer(glParam_pointType,1,GLES31.GL_BYTE,0,pointType);
			GLES31.glVertexAttribIPointer(glParam_enabled,1,GLES31.GL_BYTE,0,buffers.enabled);
			Renderer.checkGlError();
			GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
			GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureId[0]);
			Renderer.checkGlError();
			GLES31.glDrawElements(GLES31.GL_TRIANGLES,indexBuffer.limit(),GLES31.GL_UNSIGNED_INT,indexBuffer);
			Renderer.checkGlError();
		}
	}
	
	@Override
	public void onSurfaceCreated(EGLConfig eglConfig) {
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
		glParam_u_LowerBound=GLES31.glGetUniformLocation(glProgram,"u_LowerBound");
		glParam_u_SceneSize=GLES31.glGetUniformLocation(glProgram,"u_SceneSize");
		glParam_a_UV=GLES31.glGetAttribLocation(glProgram,"a_UV");
		glParam_a_illu=GLES31.glGetAttribLocation(glProgram,"a_illu");
		glParam_u_param0=GLES31.glGetUniformLocation(glProgram,"param_0");
		for(int i=1;i<attribCount;i++)
			glParam_a_param[i]=GLES31.glGetAttribLocation(glProgram,"param_"+i);
		glParam_pointType =GLES31.glGetAttribLocation(glProgram,"pointType");
		glParam_enabled =GLES31.glGetAttribLocation(glProgram,"enabled");
		Renderer.checkGlError();
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
	@Override public void onSurfaceChanged(int width, int height) {}
	@Override public void onRendererShutdown() {}
	
	public static class ExprBuilder
	{
		public abstract class Expr
		{
			abstract String repr();
			abstract void visit();
			final ExprBuilder builder;
			Expr(){builder= ExprBuilder.this;}
		}
		class Param extends Expr
		{
			private final int ID;
			Param(int ID){this.ID=ID;}
			@Override String repr(){return "param_"+ID;}
			@Override void visit(){visited[ID]=true;}
		}
		class Binary extends Expr
		{
			final Expr lhs,rhs;
			final String op;
			Binary(Expr lhs, String op, Expr rhs)
			{
				if(lhs.builder!= ExprBuilder.this||rhs.builder!= ExprBuilder.this)throw new IllegalArgumentException();
				this.lhs=lhs;this.rhs=rhs;this.op=op;
			}
			@Override String repr(){return "("+lhs.repr()+op+rhs.repr()+")";}
			@Override void visit(){lhs.visit();rhs.visit();}
		}
		class Func extends Expr
		{
			final Expr param;
			final String func;
			Func(String func, Expr param)
			{
				if(param.builder!= ExprBuilder.this)throw new IllegalArgumentException();
				this.param=param;this.func=func;
			}
			@Override String repr(){return func+"("+param.repr()+")";}
			@Override void visit(){param.visit();}
		}
		class BinFunc extends Expr
		{
			final Expr param1,param2;
			final String func;
			BinFunc(String func, Expr param1, Expr param2)
			{
				if(param1.builder!= ExprBuilder.this||param2.builder!= ExprBuilder.this)throw new IllegalArgumentException();
				this.param1=param1;this.param2=param2;this.func=func;
			}
			@Override String repr(){return func+"("+param1.repr()+","+param2.repr()+")";}
			@Override void visit(){param1.visit();param2.visit();}
		}
		class Const extends Expr
		{
			final float val;
			Const(float val){this.val=val;}
			@Override String repr(){return ""+val;}
			@Override void visit(){}
		}
		class Rel extends Expr
		{
			final Expr lhs,rhs,pass,fail;
			final String op;
			Rel(Expr lhs, String op, Expr rhs, Expr pass, Expr fail)
			{
				if(lhs.builder!= ExprBuilder.this||rhs.builder!= ExprBuilder.this)throw new IllegalArgumentException();
				if(pass.builder!= ExprBuilder.this||fail.builder!= ExprBuilder.this)throw new IllegalArgumentException();
				this.lhs=lhs;this.rhs=rhs;this.pass=pass;this.fail=fail;this.op=op;
			}
			@Override String repr(){return "(("+lhs.repr()+op+rhs.repr()+")?"+pass.repr()+":"+fail.repr()+")";}
			@Override void visit(){lhs.visit();rhs.visit();pass.visit();fail.visit();}
		}
		
		private boolean[]visited=null;
		Model model=null;
		void initVisited(){visited=new boolean[nextParamID];}
		boolean allVisited()
		{
			for(int i=1;i<nextParamID;i++)
				if(!visited[i])
					return false;
			return true;
		}
		
		private int nextParamID=1;
		int getNextParamID(){return nextParamID;}
		
		public Expr nextParam(){return new Param(nextParamID++);}
		public Expr time(){return new Param(0);}
		public Expr constant(float val){return new Const(val);}
		
		public Expr lessThan(Expr lhs, Expr rhs, Expr ifTrue, Expr ifFalse){return new Rel(lhs,"<",rhs,ifTrue,ifFalse);}
		public Expr lessEqual(Expr lhs, Expr rhs, Expr ifTrue, Expr ifFalse){return new Rel(lhs,"<=",rhs,ifTrue,ifFalse);}
		public Expr greaterThan(Expr lhs, Expr rhs, Expr ifTrue, Expr ifFalse){return new Rel(lhs,">",rhs,ifTrue,ifFalse);}
		public Expr greaterEqual(Expr lhs, Expr rhs, Expr ifTrue, Expr ifFalse){return new Rel(lhs,">=",rhs,ifTrue,ifFalse);}
		public Expr equal(Expr lhs, Expr rhs, Expr ifTrue, Expr ifFalse){return new Rel(lhs,"==",rhs,ifTrue,ifFalse);}
		public Expr notEqual(Expr lhs, Expr rhs, Expr ifTrue, Expr ifFalse){return new Rel(lhs,"!=",rhs,ifTrue,ifFalse);}
		
		public Expr add(Expr lhs, Expr rhs){return new Binary(lhs,"+",rhs);}
		public Expr sub(Expr lhs, Expr rhs){return new Binary(lhs,"-",rhs);}
		public Expr mul(Expr lhs, Expr rhs){return new Binary(lhs,"*",rhs);}
		public Expr div(Expr lhs, Expr rhs){return new Binary(lhs,"/",rhs);}
		
		public Expr sin(Expr param){return new Func("sin",param);}
		public Expr cos(Expr param){return new Func("cos",param);}
		public Expr tan(Expr param){return new Func("tan",param);}
		public Expr asin(Expr param){return new Func("asin",param);}
		public Expr acos(Expr param){return new Func("acos",param);}
		public Expr atan2(Expr param1, Expr param2){return new BinFunc("atan",param1,param2);}
		public Expr sinh(Expr param){return new Func("sinh",param);}
		public Expr cosh(Expr param){return new Func("cosh",param);}
		public Expr tanh(Expr param){return new Func("tanh",param);}
		public Expr asinh(Expr param){return new Func("asinh",param);}
		public Expr acosh(Expr param){return new Func("acosh",param);}
		public Expr atanh(Expr param){return new Func("atanh",param);}
		
		public Expr pow(Expr param1, Expr param2){return new BinFunc("pow",param1,param2);}
		public Expr exp(Expr param){return new Func("exp",param);}
		public Expr log(Expr param){return new Func("log",param);}
		public Expr exp2(Expr param){return new Func("exp2",param);}
		public Expr log2(Expr param){return new Func("log2",param);}
		public Expr sqrt(Expr param){return new Func("sqrt",param);}
		public Expr inversesqrt(Expr param){return new Func("inversesqrt",param);}
		
		public Expr abs(Expr param){return new Func("abs",param);}
		public Expr sign(Expr param){return new Func("sign",param);}
		public Expr floor(Expr param){return new Func("floor",param);}
		public Expr trunc(Expr param){return new Func("trunc",param);}
		public Expr round(Expr param){return new Func("roundEven",param);}
		public Expr ceil(Expr param){return new Func("ceil",param);}
		public Expr fract(Expr param){return new Func("fract",param);}
		public Expr mod(Expr param1, Expr param2){return new BinFunc("mod",param1,param2);}
		public Expr min(Expr param1, Expr param2){return new BinFunc("min",param1,param2);}
		public Expr max(Expr param1, Expr param2){return new BinFunc("max",param1,param2);}
	}
	
	public static class Model
	{
		public static class Rectangle
		{
			final ExprBuilder builder;
			final Point left_down,right_down,right_top,left_top;
			final int tex_left,tex_width,tex_top,tex_height;
			public Rectangle(Point left_down, Point right_down, Point right_top, Point left_top, int tex_left, int tex_width, int tex_top, int tex_height)
			{
				if(left_down.builder!=right_down.builder||left_down.builder!=right_top.builder||left_down.builder!=left_top.builder)
					throw new IllegalArgumentException();
				builder=left_down.builder;
				this.left_down=left_down;this.right_down=right_down;this.right_top=right_top;this.left_top=left_top;
				this.tex_left=tex_left;this.tex_width=tex_width;this.tex_top=tex_top;this.tex_height=tex_height;
			}
			void visit(){left_down.visit();right_down.visit();right_top.visit();left_top.visit();}
		}
		public static class Point
		{
			final ExprBuilder builder;
			final ExprBuilder.Expr x,y,z;
			public Point(ExprBuilder.Expr x, ExprBuilder.Expr y, ExprBuilder.Expr z)
			{
				if(x.builder!=y.builder||x.builder!=z.builder)
					throw new IllegalArgumentException();
				builder=x.builder;
				this.x=x;this.y=y;this.z=z;
			}
			void visit(){x.visit();y.visit();z.visit();}
			String repr(){return "vec3("+x.repr()+","+y.repr()+","+z.repr()+")";}
		}
		final int attribCount;
		final ExprBuilder.Expr x,y,z,theta;
		final Map<Point,Integer>points=new HashMap<>();
		final Rectangle[]rectangles;
		public Model(ExprBuilder exprBuilder, Rectangle[]rectangles,
					 ExprBuilder.Expr x, ExprBuilder.Expr y, ExprBuilder.Expr z, ExprBuilder.Expr theta)
		{
			attribCount=exprBuilder.getNextParamID();
			
			if(exprBuilder.model!=null)
				throw new IllegalArgumentException("multiple model from the same expression builder");
			exprBuilder.model=this;
			
			for(Rectangle rect:rectangles)
				if(rect.builder!=exprBuilder)
					throw new IllegalArgumentException();
			this.rectangles=new Rectangle[rectangles.length];
			System.arraycopy(rectangles,0,this.rectangles,0,rectangles.length);
			if(x.builder!=exprBuilder||y.builder!=exprBuilder||z.builder!=exprBuilder||theta.builder!=exprBuilder)
				throw new IllegalArgumentException();
			this.x=x;this.y=y;this.z=z;this.theta=theta;
			
			exprBuilder.initVisited();
			for(Rectangle rect:rectangles)
				rect.visit();
			x.visit();y.visit();z.visit();theta.visit();
			if(!exprBuilder.allVisited())
				throw new IllegalArgumentException("not using all parameters");
			
			for(Rectangle rect:rectangles)
			{
				Consumer<Point> func = item ->
				{
					if (!points.containsKey(item))
						points.put(item, points.size());
				};
				func.accept(rect.left_down);
				func.accept(rect.right_down);
				func.accept(rect.right_top);
				func.accept(rect.left_top);
			}
		}
	}
}
