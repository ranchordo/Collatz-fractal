import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL42.*;

import java.nio.DoubleBuffer;

import javax.vecmath.Vector4f;

import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.system.MemoryStack;

import lepton.cpshlib.ComputeShader;
import lepton.engine.rendering.FrameBuffer;
import lepton.engine.rendering.GLContextInitializer;
import lepton.engine.rendering.Screen;
import lepton.engine.rendering.Shader;
import lepton.util.CleanupTasks;
import lepton.util.InputHandler;
import lepton.util.advancedLogger.Logger;

public class Main {
	public static float window=4f;
	public static float xoff=0;
	public static float yoff=0;

	public static int maxIter=12;
	public static float magthshld=2;

	public static float zoom_speed=0.8f;

	static GLFWScrollCallback scroll_callback=new GLFWScrollCallback() {
		@Override
		public void invoke(long win, double x, double y) {
			if(y>0) {
				window*=zoom_speed;
			} else if(y<0) {
				window/=zoom_speed;
			}
		}
	};
	public static void main(String[] args) {
		Logger.setCleanupTask(()->CleanupTasks.cleanUp());
		CleanupTasks.add(()->GLContextInitializer.destroyGLContext());
		GLContextInitializer.initializeGLContext(true,750,750,false,"Collatz fractal");
		ComputeShader iter=new ComputeShader("iterate_precise");
		Shader screen_basic=new Shader("screen_basic");
		Screen screen=new Screen();
		FrameBuffer output=new FrameBuffer(0,1,GL_RGBA32F);
		FrameBuffer normalOutput=new FrameBuffer(0);
		InputHandler ih=new InputHandler(GLContextInitializer.win);
		Vector4f dims=new Vector4f();

		glfwSetScrollCallback(GLContextInitializer.win, scroll_callback);

		while(!glfwWindowShouldClose(GLContextInitializer.win)) {
			glfwPollEvents();

			if(glfwGetKey(GLContextInitializer.win,GLFW_KEY_ESCAPE)==1) {
				glfwSetWindowShouldClose(GLContextInitializer.win,true);
			}
			float aspect=GLContextInitializer.winW/(float)GLContextInitializer.winH;
			dims.set(xoff-(window*aspect)/2,xoff+(window*aspect)/2,yoff-(window)/2,yoff+(window)/2);
			if(ih.mr(GLFW_MOUSE_BUTTON_LEFT)) {
				try(MemoryStack stack=MemoryStack.stackPush()) {
					DoubleBuffer xb=stack.mallocDouble(1);
					DoubleBuffer yb=stack.mallocDouble(1);
					glfwGetCursorPos(GLContextInitializer.win,xb,yb);
					double xpx=xb.get(0)/(float)GLContextInitializer.winW;
					double ypx=yb.get(0)/(float)GLContextInitializer.winH;
					xpx*=dims.y-dims.x;
					ypx*=dims.w-dims.z;
					xpx+=dims.x;
					ypx+=dims.z;
					xoff=(float)xpx;
					yoff=(float)ypx;
				}
			}

			iter.bind();
			output.bindImage(0);
			iter.setUniform1i("img_iter",0);
			iter.setUniform1f("magthshld",magthshld);
			iter.setUniform1f("maxIter",maxIter);
			iter.setUniform4f("dims",dims.x,dims.y,dims.z,dims.w);
			iter.dispatch(GLContextInitializer.winW,GLContextInitializer.winH,1);
			glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

			output.blitTo(normalOutput);

			glClearColor(1,0,1,1);
			glClear(GL_COLOR_BUFFER_BIT);
			glDisable(GL_DEPTH_TEST);
			normalOutput.bindTexture(0);
			screen_basic.bind();
			FrameBuffer.unbind_all();
			screen.render();
			glEnable(GL_DEPTH_TEST);
			glfwSwapBuffers(GLContextInitializer.win);
		}
		CleanupTasks.cleanUp();
	}
}
