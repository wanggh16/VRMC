package cc.lym.Renderer;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import javax.microedition.khronos.egl.EGLConfig;

interface HeadlessRenderer {
	void onNewFrame(HeadTransform headTransform);
	void onDrawEye(Eye eye);
	void onFinishFrame(Viewport viewport);
	void onSurfaceChanged(int width, int height);
	void onSurfaceCreated(EGLConfig eglConfig);
	void onRendererShutdown();
}
