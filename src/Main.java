import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL42.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.vecmath.Vector4d;
import javax.vecmath.Vector4f;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.system.MemoryStack;

import lepton.cpshlib.ComputeShader;
import lepton.cpshlib.SSBO;
import lepton.cpshlib.ShaderDataCompatible;
import lepton.engine.rendering.FrameBuffer;
import lepton.engine.rendering.GLContextInitializer;
import lepton.engine.rendering.Screen;
import lepton.engine.rendering.Shader;
import lepton.util.CleanupTasks;
import lepton.util.InputHandler;
import lepton.util.LeptonUtil;
import lepton.util.advancedLogger.Logger;

public class Main {

	public static boolean PRECISE=true; //Use double precision in the shader to enable more zooming. Makes it run *a ton* slower. NOT FOR OLDER CARDS!!!

	public static double window=4f;
	public static double xoff=0;
	public static double yoff=0;

	public static int maxIter=12;
	public static float magthshld=2f;

	public static float zoom_speed=0.8f;
	public static float mag_speed=0.025f;
	public static InputHandler ih=null;

	static GLFWScrollCallback scroll_callback=new GLFWScrollCallback() {
		@Override
		public void invoke(long win, double x, double y) {
			if(Main.ih.i(GLFW_KEY_LEFT_SHIFT)) {
				magthshld+=(y>0?mag_speed:-mag_speed);
				System.out.println(magthshld);
			} else {
				if(y>0) {
					window*=zoom_speed;
				} else if(y<0) {
					window/=zoom_speed;
				}
				System.out.println(window);
			}
		}
	};
	private static void doubleToFloats(double d, FloatBuffer b) {
		long   x=Double.doubleToRawLongBits(d);
		float hx=Float.intBitsToFloat((int)(x>>32));
		float lx=Float.intBitsToFloat((int)(x));
		b.put(lx);
		b.put(hx);
	}
	public static void main(String[] args) {
		if(args.length>0) {
			if(args[0].strip().equals("--precise")) {
				PRECISE=true;
			}
		}
		Logger.setCleanupTask(()->CleanupTasks.cleanUp());
		CleanupTasks.add(()->GLContextInitializer.destroyGLContext());
		GLContextInitializer.initializeGLContext(true,500,500,false,"Collatz fractal");
		ComputeShader iter=new ComputeShader(PRECISE?"iterate_precise":"iterate");
		Shader screen_basic=new Shader("screen_basic");
		Screen screen=new Screen();
		ih=new InputHandler(GLContextInitializer.win);
		FrameBuffer output=new FrameBuffer(0,1,GL_RGBA32F);
		FrameBuffer normalOutput=new FrameBuffer(0);
		Vector4d dims=new Vector4d();
		Vector4f dimsf=new Vector4f();
		SSBO precDims=null;
		if(PRECISE) {
			precDims=iter.generateNewSSBO("window",4*8);
		}
		LeptonUtil.locationReference=Main.class;
		Random r=new Random();

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
					xoff=xpx;
					yoff=ypx;
				}
			}

			iter.bind();
			output.bindImage(0);
			iter.setUniform1i("img_iter",0);
			iter.setUniform1f("magthshld",magthshld);
			iter.setUniform1f("maxIter",maxIter);
			if(PRECISE) {
				FloatBuffer b=ShaderDataCompatible.mappify(precDims,GL_WRITE_ONLY);
				doubleToFloats(dims.x,b);
				doubleToFloats(dims.y,b);
				doubleToFloats(dims.z,b);
				doubleToFloats(dims.w,b);
				ShaderDataCompatible.unMappify();
				iter.applyAllSSBOs();
			} else {
				dimsf.set(dims);
				iter.setUniform4f("dims",dimsf.x,dimsf.y,dimsf.z,dimsf.w);
			}
			iter.dispatch(GLContextInitializer.winW,GLContextInitializer.winH,1);
			glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

			output.blitTo(normalOutput);
			if(ih.ir(GLFW_KEY_P)) {
				normalOutput.bind();
				ByteBuffer f=BufferUtils.createByteBuffer(GLContextInitializer.winW*GLContextInitializer.winH*4);
				glReadPixels(0,0,GLContextInitializer.winW,GLContextInitializer.winH,GL_RGBA,GL_BYTE,f);
				BufferedImage img=new BufferedImage(GLContextInitializer.winW,GLContextInitializer.winH,BufferedImage.TYPE_3BYTE_BGR);
				for(int i=0;i<GLContextInitializer.winW*GLContextInitializer.winH*4;i+=4) {
					int p=((f.get(i)&0xFF)<<16) | ((f.get(i+1)&0xFF)<<8) | ((f.get(i+2)&0xFF));
					img.setRGB((i/4)%GLContextInitializer.winW,(i/4)/GLContextInitializer.winW,p);
				}
				int rand=r.nextInt(100000-10000)+10000;
				f=null;
				File file=new File(LeptonUtil.getExternalPath()+"/pictures/output"+rand+".png");
				try {
					ImageIO.write(img,"png",file);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				normalOutput.unbind();
			}
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
