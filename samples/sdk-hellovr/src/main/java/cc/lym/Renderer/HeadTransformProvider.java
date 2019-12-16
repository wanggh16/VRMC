package cc.lym.Renderer;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import javax.microedition.khronos.egl.EGLConfig;

/**
 * Fake renderer where you can get information of HeadTransform
 */
public class HeadTransformProvider implements HeadlessRenderer {
	private boolean initialized=false;
	private final float[]headView=new float[16];
	private final float[]right=new float[3];
	private final float[]up=new float[3];
	private final float[]forward=new float[3];
	private final float[]euler=new float[3];
	private final float[]quaternion=new float[4];
	public boolean isInitialized(){return initialized;}
	public void getHeadView(float[]arr,int offset){System.arraycopy(headView,0,arr,offset,16);}
	public void getRightVector(float[]arr,int offset){System.arraycopy(right,0,arr,offset,3);}
	public void getUpVector(float[]arr,int offset){System.arraycopy(up,0,arr,offset,3);}
	public void getForwardVector(float[]arr,int offset){System.arraycopy(forward,0,arr,offset,3);}
	public void getEulerAngles(float[]arr,int offset){System.arraycopy(euler,0,arr,offset,3);}
	public void getQuaternion(float[]arr,int offset){System.arraycopy(quaternion,0,arr,offset,4);}
	
	@Override
	public void onNewFrame(HeadTransform headTransform)
	{
		initialized=true;
		headTransform.getHeadView(headView,0);
		headTransform.getRightVector(right,0);
		headTransform.getUpVector(up,0);
		headTransform.getForwardVector(forward,0);
		headTransform.getEulerAngles(euler,0);
		headTransform.getQuaternion(quaternion,0);
	}
	@Override
	public void onDrawEye(Eye eye)
	{
	
	}
	@Override
	public void onSurfaceCreated(EGLConfig eglConfig)
	{
	
	}
	@Override public void onFinishFrame(Viewport viewport) {}
	@Override public void onSurfaceChanged(int i, int i1) {}
	@Override public void onRendererShutdown() {}
}
