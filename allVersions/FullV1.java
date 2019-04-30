package allVersions;

import graphicslib3D.*;
import graphicslib3D.light.*;
import graphicslib3D.GLSLUtils.*;
import graphicslib3D.shape.*;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.nio.*;
import javax.swing.*;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.common.nio.Buffers;

public class FullV1 extends JFrame implements GLEventListener, KeyListener
{	private GLCanvas myCanvas;
	private Material thisMaterial;
	private String[] vBlinn1ShaderSource, vBlinn2ShaderSource, fBlinn2ShaderSource, vObjShaderSource, fObjShaderSource;
	private int rendering_program1, rendering_program2, rendering_programObj, rendering_programSkybox;
	private int vao[] = new int[1];
	private int vbo[] = new int[9];
	private int mv_location, proj_location, vertexLoc, n_location;
	private float aspect;
	private GLSLUtils util = new GLSLUtils();
	
	// location of torus and camera
	private Point3D torusLoc = new Point3D(1.6, 0.0, -0.3);
	private Point3D pyrLoc = new Point3D(-1.0, 0.1, 0.3);
	private Point3D cameraLoc = new Point3D(0.0, 0.2, 6.0);
	private Point3D lightLoc = new Point3D(-3.8f, 2.2f, 1.1f);
	
	private Matrix3D m_matrix = new Matrix3D();
	private Matrix3D v_matrix = new Matrix3D();
	private Matrix3D mv_matrix = new Matrix3D();
	private Matrix3D proj_matrix = new Matrix3D();
	
	// light stuff
	private float [] globalAmbient = new float[] { 0.7f, 0.7f, 0.7f, 1.0f };
	private PositionalLight currentLight = new PositionalLight();
	
	// shadow stuff
	private int scSizeX, scSizeY;
	private int [] shadow_tex = new int[1];
	private int [] shadow_buffer = new int[1];
	private Matrix3D lightV_matrix = new Matrix3D();
	private Matrix3D lightP_matrix = new Matrix3D();
	private Matrix3D shadowMVP1 = new Matrix3D();
	private Matrix3D shadowMVP2 = new Matrix3D();
	private Matrix3D shadowMVP3 = new Matrix3D();
	private Matrix3D b = new Matrix3D();

	// model stuff
	private ImportedModel pyramid = new ImportedModel("../pyr.obj");
	private Torus myTorus = new Torus(0.6f, 0.4f, 48);
	private int numPyramidVertices, numTorusVertices;
	
	private ImportedModel myObj;
	private int numObjVertices;
	
	private int objTexture;
	private Texture joglObjTexture;
	
	//look at values
	Vector3D eye = new Vector3D(-cameraLoc.getX(), -cameraLoc.getY(), -cameraLoc.getZ());
	Vector3D lookingAt = new Vector3D(torusLoc.getX(), torusLoc.getY(), torusLoc.getZ());
	Vector3D up = new Vector3D(0,1,0);
	
	Vector3D cameraUp;
	Vector3D cameraRight;
	Vector3D cameraFwd;
		
	//movement
	private boolean forward;
	private boolean backward;
	private boolean strafeLeft;
	private boolean strafeRight;
	private boolean moveDown;
	private boolean moveUp;
	private boolean panLeft;
	private boolean panRight;
	private boolean pitchUp;
	private boolean pitchDown;
	private boolean camera = true;
	
	float pan;
	float pitch;
	
	private boolean toggleOn = true;
	
	private float[] currAmbient = currentLight.getAmbient();
	private float[] currSpecular = currentLight.getSpecular();
	private float[] currDiffuse = currentLight.getDiffuse();
	
	//skybox variables
	private String[] skyVertShaderSource, skyFragShaderSource;
	private int skyboxTexture;
	
	public FullV1()
	{	setTitle("Chapter8 - program 1");
		setSize(800, 800);
		GLProfile profile=GLProfile.get(GLProfile.GL4);
		GLCapabilities capabilities=new GLCapabilities(profile);
		myCanvas = new GLCanvas(capabilities);
		myCanvas.addGLEventListener(this);
		getContentPane().add(myCanvas);
		setVisible(true);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		FPSAnimator animator = new FPSAnimator(myCanvas, 30);
		animator.start();
		addKeyListener(this);
	}

	public void display(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		keyInput();

		currentLight.setPosition(lightLoc);
		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		proj_matrix = perspective(50.0f, aspect, 0.1f, 1000.0f);
		
		float bkg[] = { 0.0f, 0.0f, 0.0f, 1.0f };
		FloatBuffer bkgBuffer = Buffers.newDirectFloatBuffer(bkg);
		gl.glClearBufferfv(GL_COLOR, 0, bkgBuffer);
		
		/*cameraFwd = new Vector3D(
			(Math.cos(Math.toRadians(pitch)) * Math.cos(Math.toRadians(pan))),
			(Math.sin(Math.toRadians(pitch))),
			(Math.cos(Math.toRadians(pitch)) * Math.sin(Math.toRadians(pan)))
		);
			
		Matrix3D lookAtMat = lookAt(eye, lookingAt, up);
		m_matrix.setToIdentity();
		
		v_matrix.setToIdentity();
		v_matrix.translate(-cameraLoc.getX(),-cameraLoc.getY(),-cameraLoc.getZ());
		
		mv_matrix.setToIdentity();
		mv_matrix.concatenate(v_matrix);
		mv_matrix.concatenate(m_matrix);*/
		
		gl.glBindFramebuffer(GL_FRAMEBUFFER, shadow_buffer[0]);
		gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadow_tex[0], 0);
	
		gl.glDrawBuffer(GL_NONE);
		gl.glEnable(GL_DEPTH_TEST);

		gl.glEnable(GL_POLYGON_OFFSET_FILL);	// for reducing
		gl.glPolygonOffset(2.0f, 4.0f);			//  shadow artifacts

		passOne();
		
		gl.glDisable(GL_POLYGON_OFFSET_FILL);	// artifact reduction, continued
		
		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, shadow_tex[0]);
	
		gl.glDrawBuffer(GL_FRONT);
		
		passTwo();
	}
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public void passOne()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		gl.glUseProgram(rendering_program1);
		
		Point3D origin = new Point3D(0.0, 0.0, 0.0);
		Vector3D up = new Vector3D(0.0, 1.0, 0.0);
		lightV_matrix.setToIdentity();
		lightP_matrix.setToIdentity();
	
		lightV_matrix = lookAt(currentLight.getPosition(), origin, up);	// vector from light to origin
		lightP_matrix = perspective(50.0f, aspect, 0.1f, 1000.0f);

		// draw the torus
		
		m_matrix.setToIdentity();
		m_matrix.translate(torusLoc.getX(),torusLoc.getY(),torusLoc.getZ());
		m_matrix.rotateX(25.0);
		
		shadowMVP1.setToIdentity();
		shadowMVP1.concatenate(lightP_matrix);
		shadowMVP1.concatenate(lightV_matrix);
		shadowMVP1.concatenate(m_matrix);
		int shadow_location = gl.glGetUniformLocation(rendering_program1, "shadowMVP");
		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP1.getFloatValues(), 0);
		
		// set up torus vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);	
	
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, numTorusVertices);

		// ---- draw the pyramid
		
		//  build the MODEL matrix
		m_matrix.setToIdentity();
		m_matrix.translate(pyrLoc.getX(),pyrLoc.getY(),pyrLoc.getZ());
		m_matrix.rotateX(30.0);
		m_matrix.rotateY(40.0);

		shadowMVP1.setToIdentity();
		shadowMVP1.concatenate(lightP_matrix);
		shadowMVP1.concatenate(lightV_matrix);
		shadowMVP1.concatenate(m_matrix);

		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP1.getFloatValues(), 0);
		
		// set up vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
	
		gl.glDrawArrays(GL_TRIANGLES, 0, pyramid.getNumVertices());
		
		
		//draw Object
		
		gl.glUseProgram(rendering_programObj);
		
		/*shadowMVP1.setToIdentity();
		shadowMVP1.concatenate(lightP_matrix);
		shadowMVP1.concatenate(lightV_matrix);
		shadowMVP1.concatenate(m_matrix); */
		shadow_location = gl.glGetUniformLocation(rendering_programObj, "shadowMVP");
		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP1.getFloatValues(), 0); 
		
		m_matrix.setToIdentity();
		m_matrix.translate(pyrLoc.getX(),pyrLoc.getY()+2,pyrLoc.getZ());
		m_matrix.rotateX(30.0);
		m_matrix.rotateY(40.0);

		shadowMVP1.setToIdentity();
		shadowMVP1.concatenate(lightP_matrix);
		shadowMVP1.concatenate(lightV_matrix);
		shadowMVP1.concatenate(m_matrix);
		
		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP1.getFloatValues(), 0);
		
		// set up vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		/*gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, objTexture); */

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
	
		gl.glDrawArrays(GL_TRIANGLES, 0, myObj.getNumVertices());
	}
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public void passTwo()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		gl.glUseProgram(rendering_program2);
		
		cameraFwd = new Vector3D(
			(Math.cos(Math.toRadians(pitch)) * Math.cos(Math.toRadians(pan))),
			(Math.sin(Math.toRadians(pitch))),
			(Math.cos(Math.toRadians(pitch)) * Math.sin(Math.toRadians(pan)))
		);
			
		Matrix3D lookAtMat = lookAt(eye, lookingAt, up);
		
		m_matrix.setToIdentity();
		//m_matrix.translate(torusLoc.getX(),torusLoc.getY(),torusLoc.getZ());
		m_matrix.rotateX(25.0);

		//  build the VIEW matrix
		v_matrix.setToIdentity();
		v_matrix.translate(-cameraLoc.getX(),-cameraLoc.getY(),-cameraLoc.getZ());

		mv_matrix.setToIdentity();
		mv_matrix.concatenate(v_matrix);
		mv_matrix.concatenate(m_matrix);
		mv_matrix.concatenate(lookAtMat);
		
		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);

		// draw the torus
		
		thisMaterial = graphicslib3D.Material.BRONZE;		
		
		mv_location = gl.glGetUniformLocation(rendering_program2, "mv_matrix");
		proj_location = gl.glGetUniformLocation(rendering_program2, "proj_matrix");
		n_location = gl.glGetUniformLocation(rendering_program2, "normalMat");
		int shadow_location = gl.glGetUniformLocation(rendering_program2,  "shadowMVP");
		
		//  build the MODEL matrix
		m_matrix.setToIdentity();
		m_matrix.translate(torusLoc.getX(),torusLoc.getY(),torusLoc.getZ());
		m_matrix.rotateX(25.0);

		//  build the VIEW matrix
		v_matrix.setToIdentity();
		v_matrix.translate(-cameraLoc.getX(),-cameraLoc.getY(),-cameraLoc.getZ());
		
		if (toggleOn) {
			currentLight.setAmbient(currAmbient);
			currentLight.setDiffuse(currDiffuse);
			currentLight.setSpecular(currSpecular);
			
			installLights(rendering_program2, v_matrix);
		} else {
			float[] off = {0.0f, 0.0f, 0.0f, 0.0f};
			
			currentLight.setAmbient(off);
			currentLight.setDiffuse(off);
			currentLight.setSpecular(off);
			
			installLights(rendering_program2, v_matrix);
		}
		
		//  build the MODEL-VIEW matrix
		mv_matrix.setToIdentity();
		mv_matrix.concatenate(v_matrix);
		mv_matrix.concatenate(m_matrix);
		
		shadowMVP2.setToIdentity();
		shadowMVP2.concatenate(b);
		shadowMVP2.concatenate(lightP_matrix);
		shadowMVP2.concatenate(lightV_matrix);
		shadowMVP2.concatenate(m_matrix);
		
		//  put the MV and PROJ matrices into the corresponding uniforms
		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_location, 1, false, proj_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(), 0);
		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP2.getFloatValues(), 0);
		
		// set up torus vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// set up torus normals buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);	
	
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
	
		gl.glDrawArrays(GL_TRIANGLES, 0, numTorusVertices);

		// draw the pyramid
		
		thisMaterial = graphicslib3D.Material.GOLD;	
		if (toggleOn) {
			currentLight.setAmbient(currAmbient);
			currentLight.setDiffuse(currDiffuse);
			currentLight.setSpecular(currSpecular);
			
			installLights(rendering_program2, v_matrix);
		} else {
			float[] off = {0.0f, 0.0f, 0.0f, 0.0f};
			
			currentLight.setAmbient(off);
			currentLight.setDiffuse(off);
			currentLight.setSpecular(off);
			
			installLights(rendering_program2, v_matrix);
		}
		//installLights(rendering_program2, v_matrix);
		
		//  build the MODEL matrix
		m_matrix.setToIdentity();
		m_matrix.translate(pyrLoc.getX(),pyrLoc.getY(),pyrLoc.getZ());
		m_matrix.rotateX(30.0);
		m_matrix.rotateY(40.0);

		//  build the MODEL-VIEW matrix
		mv_matrix.setToIdentity();
		mv_matrix.concatenate(v_matrix);
		mv_matrix.concatenate(m_matrix);
		
		shadowMVP2.setToIdentity();
		shadowMVP2.concatenate(b);
		shadowMVP2.concatenate(lightP_matrix);
		shadowMVP2.concatenate(lightV_matrix);
		shadowMVP2.concatenate(m_matrix);
		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP2.getFloatValues(), 0);

		//  put the MV and PROJ matrices into the corresponding uniforms
		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_location, 1, false, proj_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(), 0);
		
		// set up vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// set up normals buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, pyramid.getNumVertices());
		

		//draw the object
		
		gl.glUseProgram(rendering_programObj);
		thisMaterial = Material.BRONZE;
		
		//gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP2.getFloatValues(), 0);
		
		if (toggleOn) {
			currentLight.setAmbient(currAmbient);
			currentLight.setDiffuse(currDiffuse);
			currentLight.setSpecular(currSpecular);
			
			installLights(rendering_programObj, v_matrix);
		} else {
			float[] off = {0.0f, 0.0f, 0.0f, 0.0f};
			
			currentLight.setAmbient(off);
			currentLight.setDiffuse(off);
			currentLight.setSpecular(off);
			
			installLights(rendering_programObj, v_matrix);
		}
		
		mv_location = gl.glGetUniformLocation(rendering_programObj, "mv_matrix");
		proj_location = gl.glGetUniformLocation(rendering_programObj, "proj_matrix");
		n_location = gl.glGetUniformLocation(rendering_programObj, "normalMat");
		shadow_location = gl.glGetUniformLocation(rendering_programObj,  "shadowMVP");
		
		m_matrix.setToIdentity();
		m_matrix.translate(pyrLoc.getX(),pyrLoc.getY()+2,pyrLoc.getZ());
		m_matrix.rotateX(30.0);
		m_matrix.rotateY(40.0);
		
	//  build the MODEL-VIEW matrix
		mv_matrix.setToIdentity();
		mv_matrix.concatenate(v_matrix);
		mv_matrix.concatenate(m_matrix);
		
		shadowMVP2.setToIdentity();
		shadowMVP2.concatenate(b);
		shadowMVP2.concatenate(lightP_matrix);
		shadowMVP2.concatenate(lightV_matrix);
		shadowMVP2.concatenate(m_matrix);
		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP2.getFloatValues(), 0);
	
		//put the MV and PROJ matrices into the corresponding uniforms
		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_location, 1, false, proj_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(), 0);
		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP2.getFloatValues(), 0);
		
		// set up vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// set up texture buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, objTexture);
		
		// set up normals buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		//gl.glClear(GL_DEPTH_BUFFER_BIT);
		//gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, myObj.getNumVertices());
	}

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		myObj = new ImportedModel("../shuttle.obj");

		createShaderPrograms();
		setupVertices();
		setupShadowBuffers();
				
		b.setElementAt(0,0,0.5);b.setElementAt(0,1,0.0);b.setElementAt(0,2,0.0);b.setElementAt(0,3,0.5f);
		b.setElementAt(1,0,0.0);b.setElementAt(1,1,0.5);b.setElementAt(1,2,0.0);b.setElementAt(1,3,0.5f);
		b.setElementAt(2,0,0.0);b.setElementAt(2,1,0.0);b.setElementAt(2,2,0.5);b.setElementAt(2,3,0.5f);
		b.setElementAt(3,0,0.0);b.setElementAt(3,1,0.0);b.setElementAt(3,2,0.0);b.setElementAt(3,3,1.0f);
		
		eye = new Vector3D(cameraLoc.getX(), cameraLoc.getY(), cameraLoc.getZ());
		lookingAt = new Vector3D(0.0f, 0.0f, 0.0f);
		up = new Vector3D(0.0f, 1.0f, 0.0f);
		
		pitch = 0.0f;
		pan = -90.0f;
		
		// may reduce shadow border artifacts
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		
		joglObjTexture = loadTexture("Objects/spstob_1.jpg");
		objTexture = joglObjTexture.getTextureObject();
		
		Texture t = loadTexture("Skybox3/bikiniBottom2.jpg");
		skyboxTexture = t.getTextureObject();
	}
	
	public void setupShadowBuffers()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		scSizeX = myCanvas.getWidth();
		scSizeY = myCanvas.getHeight();
	
		gl.glGenFramebuffers(1, shadow_buffer, 0);
	
		gl.glGenTextures(1, shadow_tex, 0);
		gl.glBindTexture(GL_TEXTURE_2D, shadow_tex[0]);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32,
						scSizeX, scSizeY, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
	}

// -----------------------------
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		setupShadowBuffers();
	}

	private void setupVertices()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
	float[] cube_vertices =
        {	-1.0f,  1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
		1.0f, -1.0f, -1.0f, 1.0f,  1.0f, -1.0f, -1.0f,  1.0f, -1.0f,
		1.0f, -1.0f, -1.0f, 1.0f, -1.0f,  1.0f, 1.0f,  1.0f, -1.0f,
		1.0f, -1.0f,  1.0f, 1.0f,  1.0f,  1.0f, 1.0f,  1.0f, -1.0f,
		1.0f, -1.0f,  1.0f, -1.0f, -1.0f,  1.0f, 1.0f,  1.0f,  1.0f,
		-1.0f, -1.0f,  1.0f, -1.0f,  1.0f,  1.0f, 1.0f,  1.0f,  1.0f,
		-1.0f, -1.0f,  1.0f, -1.0f, -1.0f, -1.0f, -1.0f,  1.0f,  1.0f,
		-1.0f, -1.0f, -1.0f, -1.0f,  1.0f, -1.0f, -1.0f,  1.0f,  1.0f,
		-1.0f, -1.0f,  1.0f,  1.0f, -1.0f,  1.0f,  1.0f, -1.0f, -1.0f,
		1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,  1.0f,
		-1.0f,  1.0f, -1.0f, 1.0f,  1.0f, -1.0f, 1.0f,  1.0f,  1.0f,
		1.0f,  1.0f,  1.0f, -1.0f,  1.0f,  1.0f, -1.0f,  1.0f, -1.0f
	};

	float[] cube_texture_coord =
    	{	.25f,  .666666666f, .25f, .3333333333f, .5f, .3333333333f,	// front face lower left
		.5f, .333333333333f, .5f,  .66666666666f, .25f,  .66666666666f,	// front face upper right
		.5f, .3333333333f, .75f, .33333333333f,  .5f,  .6666666666f,	// right face lower left
		.75f, .33333333333f,  .75f,  .66666666666f, .5f,  .6666666666f,	// right face upper right
		.75f, .3333333333f,  1.0f, .3333333333f, .75f,  .66666666666f,	// back face lower
		1.0f, .3333333333f, 1.0f,  .6666666666f, .75f,  .6666666666f,	// back face upper
		0.0f, .333333333f,  .25f, .333333333f, 0.0f,  .666666666f,	// left face lower
		.25f, .333333333f, .25f,  .666666666f, 0.0f,  .666666666f,	// left face upper
		.25f, 0.0f,  .5f, 0.0f,  .5f, .333333333f,			// bottom face front
		.5f, .333333333f, .25f, .333333333f, .25f, 0.0f,		// bottom face back
		.25f,  .666666666f, .5f,  .666666666f, .5f,  1.0f,		// top face back
		.5f,  1.0f,  .25f,  1.0f, .25f,  .666666666f			// top face front
	};

		// pyramid definition
		Vertex3D[] pyramid_vertices = pyramid.getVertices();
		numPyramidVertices = pyramid.getNumVertices();

		float[] pyramid_vertex_positions = new float[numPyramidVertices*3];
		float[] pyramid_normals = new float[numPyramidVertices*3];

		for (int i=0; i<numPyramidVertices; i++)
		{	pyramid_vertex_positions[i*3]   = (float) (pyramid_vertices[i]).getX();			
			pyramid_vertex_positions[i*3+1] = (float) (pyramid_vertices[i]).getY();
			pyramid_vertex_positions[i*3+2] = (float) (pyramid_vertices[i]).getZ();
			
			pyramid_normals[i*3]   = (float) (pyramid_vertices[i]).getNormalX();
			pyramid_normals[i*3+1] = (float) (pyramid_vertices[i]).getNormalY();
			pyramid_normals[i*3+2] = (float) (pyramid_vertices[i]).getNormalZ();
		}

		Vertex3D[] torus_vertices = myTorus.getVertices();
		
		int[] torus_indices = myTorus.getIndices();	
		float[] torus_fvalues = new float[torus_indices.length*3];
		float[] torus_nvalues = new float[torus_indices.length*3];
		
		for (int i=0; i<torus_indices.length; i++)
		{	torus_fvalues[i*3]   = (float) (torus_vertices[torus_indices[i]]).getX();			
			torus_fvalues[i*3+1] = (float) (torus_vertices[torus_indices[i]]).getY();
			torus_fvalues[i*3+2] = (float) (torus_vertices[torus_indices[i]]).getZ();
			
			torus_nvalues[i*3]   = (float) (torus_vertices[torus_indices[i]]).getNormalX();
			torus_nvalues[i*3+1] = (float) (torus_vertices[torus_indices[i]]).getNormalY();
			torus_nvalues[i*3+2] = (float) (torus_vertices[torus_indices[i]]).getNormalZ();
		}
		
		numTorusVertices = torus_indices.length;
		
		Vertex3D[] objVertices = myObj.getVertices();
		numObjVertices = myObj.getNumVertices();
		
		float[] pvaluesObj = new float[numObjVertices*3];
		float[] tvaluesObj = new float[numObjVertices*2];
		float[] nvaluesObj = new float[numObjVertices*3];
		
		for (int i=0; i<numObjVertices; i++)
		{	pvaluesObj[i*3]   = (float) (objVertices[i]).getX();
			pvaluesObj[i*3+1] = (float) (objVertices[i]).getY();
			pvaluesObj[i*3+2] = (float) (objVertices[i]).getZ();
			tvaluesObj[i*2]   = (float) (objVertices[i]).getS();
			tvaluesObj[i*2+1] = (float) (objVertices[i]).getT();
			nvaluesObj[i*3]   = (float) (objVertices[i]).getNormalX();
			nvaluesObj[i*3+1] = (float) (objVertices[i]).getNormalY();
			nvaluesObj[i*3+2] = (float) (objVertices[i]).getNormalZ();
		}

		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);

		gl.glGenBuffers(vbo.length, vbo, 0);

		//  put the Torus vertices into the first buffer,
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(torus_fvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);
		
		//  load the pyramid vertices into the second buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer pyrVertBuf = Buffers.newDirectFloatBuffer(pyramid_vertex_positions);
		gl.glBufferData(GL_ARRAY_BUFFER, pyrVertBuf.limit()*4, pyrVertBuf, GL_STATIC_DRAW);
		
		// load the torus normal coordinates into the third buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer torusNorBuf = Buffers.newDirectFloatBuffer(torus_nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, torusNorBuf.limit()*4, torusNorBuf, GL_STATIC_DRAW);
		
		// load the pyramid normal coordinates into the fourth buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer pyrNorBuf = Buffers.newDirectFloatBuffer(pyramid_normals);
		gl.glBufferData(GL_ARRAY_BUFFER, pyrNorBuf.limit()*4, pyrNorBuf, GL_STATIC_DRAW);
		
		//load the object vertex coordinates into the 5th buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer vertBufObj = Buffers.newDirectFloatBuffer(pvaluesObj);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBufObj.limit()*4, vertBufObj, GL_STATIC_DRAW);

		//load the object texture coordinates into the 6th buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		FloatBuffer texBufObj = Buffers.newDirectFloatBuffer(tvaluesObj);
		gl.glBufferData(GL_ARRAY_BUFFER, texBufObj.limit()*4, texBufObj, GL_STATIC_DRAW);

		//load the object normal coordinates into the 7th buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		FloatBuffer norBufObj = Buffers.newDirectFloatBuffer(nvaluesObj);
		gl.glBufferData(GL_ARRAY_BUFFER, norBufObj.limit()*4,norBufObj, GL_STATIC_DRAW); 
		
		//load the skybox vertices into the 8th buffer 
		
		//load the skybox texture coordinates into the 9th buffer
	}
	
	private void installLights(int rendering_program, Matrix3D v_matrix)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		Material currentMaterial = new Material();
		currentMaterial = thisMaterial;
		
		Point3D lightP = currentLight.getPosition();
		Point3D lightPv = lightP.mult(v_matrix);
		
		float [] currLightPos = new float[] { (float) lightPv.getX(),
			(float) lightPv.getY(),
			(float) lightPv.getZ() };

		// get the location of the global ambient light field in the shader
		int globalAmbLoc = gl.glGetUniformLocation(rendering_program, "globalAmbient");
	
		// set the current globalAmbient settings
		gl.glProgramUniform4fv(rendering_program, globalAmbLoc, 1, globalAmbient, 0);

		// get the locations of the light and material fields in the shader
		int ambLoc = gl.glGetUniformLocation(rendering_program, "light.ambient");
		int diffLoc = gl.glGetUniformLocation(rendering_program, "light.diffuse");
		int specLoc = gl.glGetUniformLocation(rendering_program, "light.specular");
		int posLoc = gl.glGetUniformLocation(rendering_program, "light.position");

		int MambLoc = gl.glGetUniformLocation(rendering_program, "material.ambient");
		int MdiffLoc = gl.glGetUniformLocation(rendering_program, "material.diffuse");
		int MspecLoc = gl.glGetUniformLocation(rendering_program, "material.specular");
		int MshiLoc = gl.glGetUniformLocation(rendering_program, "material.shininess");

		// set the uniform light and material values in the shader
		gl.glProgramUniform4fv(rendering_program, ambLoc, 1, currentLight.getAmbient(), 0);
		gl.glProgramUniform4fv(rendering_program, diffLoc, 1, currentLight.getDiffuse(), 0);
		gl.glProgramUniform4fv(rendering_program, specLoc, 1, currentLight.getSpecular(), 0);
		gl.glProgramUniform3fv(rendering_program, posLoc, 1, currLightPos, 0);
	
		gl.glProgramUniform4fv(rendering_program, MambLoc, 1, currentMaterial.getAmbient(), 0);
		gl.glProgramUniform4fv(rendering_program, MdiffLoc, 1, currentMaterial.getDiffuse(), 0);
		gl.glProgramUniform4fv(rendering_program, MspecLoc, 1, currentMaterial.getSpecular(), 0);
		gl.glProgramUniform1f(rendering_program, MshiLoc, currentMaterial.getShininess());
	}

	public static void main(String[] args) { new FullV1(); }

	@Override
	public void dispose(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) drawable.getGL();
		gl.glDeleteVertexArrays(1, vao, 0);
	}

//-----------------
	private void createShaderPrograms()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		int[] vertCompiled = new int[1];
		int[] fragCompiled = new int[1];

		vBlinn1ShaderSource = util.readShaderSource("Shadow/blinnVert1.shader");
		vBlinn2ShaderSource = util.readShaderSource("Shadow/blinnVert2.shader");
		fBlinn2ShaderSource = util.readShaderSource("Shadow/blinnFrag2.shader");
		vObjShaderSource = util.readShaderSource("ShaderSolarSystem/vert3.shader");
		fObjShaderSource = util.readShaderSource("ShaderSolarSystem/frag3.shader");
		
		int vertexShader1 = gl.glCreateShader(GL_VERTEX_SHADER);
		int vertexShader2 = gl.glCreateShader(GL_VERTEX_SHADER);
		int fragmentShader2 = gl.glCreateShader(GL_FRAGMENT_SHADER);
		int vObjShader = gl.glCreateShader(GL_VERTEX_SHADER);
		int fObjShader = gl.glCreateShader(GL_FRAGMENT_SHADER);

		gl.glShaderSource(vertexShader1, vBlinn1ShaderSource.length, vBlinn1ShaderSource, null, 0);
		gl.glShaderSource(vertexShader2, vBlinn2ShaderSource.length, vBlinn2ShaderSource, null, 0);
		gl.glShaderSource(fragmentShader2, fBlinn2ShaderSource.length, fBlinn2ShaderSource, null, 0);
		gl.glShaderSource(vObjShader, vObjShaderSource.length, vObjShaderSource, null, 0);
		gl.glShaderSource(fObjShader, fObjShaderSource.length, fObjShaderSource, null, 0);
		
		gl.glCompileShader(vertexShader1);
		gl.glCompileShader(vertexShader2);
		gl.glCompileShader(fragmentShader2);
		gl.glCompileShader(vObjShader);
		gl.glCompileShader(fObjShader);

		rendering_program1 = gl.glCreateProgram();
		rendering_program2 = gl.glCreateProgram();
		rendering_programObj = gl.glCreateProgram();

		gl.glAttachShader(rendering_program1, vertexShader1);
		gl.glAttachShader(rendering_program2, vertexShader2);
		gl.glAttachShader(rendering_program2, fragmentShader2);
		gl.glAttachShader(rendering_programObj, vObjShader);
		gl.glAttachShader(rendering_programObj, fObjShader);

		gl.glLinkProgram(rendering_program1);
		gl.glLinkProgram(rendering_program2);
		gl.glLinkProgram(rendering_programObj);
	}

//------------------
	private Matrix3D perspective(float fovy, float aspect, float n, float f)
	{	float q = 1.0f / ((float) Math.tan(Math.toRadians(0.5f * fovy)));
		float A = q / aspect;
		float B = (n + f) / (n - f);
		float C = (2.0f * n * f) / (n - f);
		Matrix3D r = new Matrix3D();
		r.setElementAt(0,0,A);
		r.setElementAt(1,1,q);
		r.setElementAt(2,2,B);
		r.setElementAt(3,2,-1.0f);
		r.setElementAt(2,3,C);
		r.setElementAt(3,3,0.0f);
		return r;
	}

	private Matrix3D lookAt(Point3D eye, Point3D target, Vector3D y)
	{	Vector3D eyeV = new Vector3D(eye);
		Vector3D targetV = new Vector3D(target);
		Vector3D fwd = (targetV.minus(eyeV)).normalize();
		Vector3D side = (fwd.cross(y)).normalize();
		Vector3D up = (side.cross(fwd)).normalize();
		Matrix3D look = new Matrix3D();
		look.setElementAt(0,0, side.getX());
		look.setElementAt(1,0, up.getX());
		look.setElementAt(2,0, -fwd.getX());
		look.setElementAt(3,0, 0.0f);
		look.setElementAt(0,1, side.getY());
		look.setElementAt(1,1, up.getY());
		look.setElementAt(2,1, -fwd.getY());
		look.setElementAt(3,1, 0.0f);
		look.setElementAt(0,2, side.getZ());
		look.setElementAt(1,2, up.getZ());
		look.setElementAt(2,2, -fwd.getZ());
		look.setElementAt(3,2, 0.0f);
		look.setElementAt(0,3, side.dot(eyeV.mult(-1)));
		look.setElementAt(1,3, up.dot(eyeV.mult(-1)));
		look.setElementAt(2,3, (fwd.mult(-1)).dot(eyeV.mult(-1)));
		look.setElementAt(3,3, 1.0f);
		return(look);
	}
	
	private Matrix3D lookAt(Vector3D eyeV, Vector3D targetV, Vector3D y) {
		Vector3D fwd = cameraFwd;
		Vector3D side = (fwd.cross(y)).normalize();
		cameraRight = side;
		Vector3D up = (side.cross(fwd)).normalize();
		cameraUp = up;
		
		Matrix3D look = new Matrix3D();
		look.setElementAt(0, 0, side.getX());
		look.setElementAt(1, 0, up.getX());
		look.setElementAt(2, 0, -fwd.getX());
		look.setElementAt(3, 0, 0.0f);
		look.setElementAt(0, 1, side.getY());
		look.setElementAt(1, 1, up.getY());
		look.setElementAt(2, 1, -fwd.getY());
		look.setElementAt(3, 1, 0.0f);
		look.setElementAt(0, 2, side.getZ());
		look.setElementAt(1, 2, up.getZ());
		look.setElementAt(2, 2, -fwd.getZ());
		look.setElementAt(3, 2, 0.0f);
		look.setElementAt(0, 3, side.dot(eyeV.mult(-1)));
		look.setElementAt(1, 3, up.dot(eyeV.mult(-1)));
		look.setElementAt(2, 3, (fwd.mult(-1)).dot(eyeV.mult(-1)));
		look.setElementAt(3, 3, 1.0f);
		
		return(look);
	}
	
	public Texture loadTexture(String textureFileName)
	{	Texture tex = null;
		try { tex = TextureIO.newTexture(new File(textureFileName), false); }
		catch (Exception e) { e.printStackTrace(); }
		return tex;
	}
	
	public void keyInput() {
		double movement = 0.3f;
		if (camera) {
			if (forward) {
			eye = eye.add(cameraFwd.mult(movement));
			} else if (backward) {
				eye = eye.add(cameraFwd.mult(-movement));
			} else if (strafeLeft) {
				eye = eye.add(cameraRight.mult(-movement));
			} else if (strafeRight) {
				eye = eye.add(cameraRight.mult(movement));
			} else if (moveUp) {
				eye = eye.add(cameraUp.mult(movement));
			} else if (moveDown) {
				eye = eye.add(cameraUp.mult(-movement));
			} else if (panLeft) {
				pan -= 1.0f;
			} else if (panRight) {
				pan += 1.0f;
			} else if (pitchUp) {
				pitch += 1.0f;
				if (pitch > 90.0f) {
					pitch = 90.0f;
				}
			} else if (pitchDown) {
				pitch -= 1.0f;
				if (pitch < -90.0f) {
					pitch = -90.0f;
				}
			}
		} else {
			if (forward) {
				Point3D offset = new Point3D(0, 0, movement);
				lightLoc = lightLoc.add(offset);
			} else if (backward) {
				Point3D offset = new Point3D(0, 0, -movement);
				lightLoc = lightLoc.add(offset);
			} else if (strafeLeft) {
				//System.out.println(lightLoc);
				Point3D offset = new Point3D(-movement, 0, 0);
				lightLoc = lightLoc.add(offset);
			} else if (strafeRight) {
				Point3D offset = new Point3D(movement, 0, 0);
				lightLoc = lightLoc.add(offset);
			} else if (moveUp) {
				Point3D offset = new Point3D(0, movement, 0);
				lightLoc = lightLoc.add(offset);
			} else if (moveDown) {
				Point3D offset = new Point3D(0, -movement, 0);
				lightLoc = lightLoc.add(offset);
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		int keyCode = e.getKeyCode();
		if (keyCode == KeyEvent.VK_O) {
			if (toggleOn) {
				currAmbient = currentLight.getAmbient();
				currDiffuse = currentLight.getDiffuse();
				currSpecular = currentLight.getSpecular();
			}
			System.out.println("pressed O");
			toggleOn = !toggleOn;
		} else if (keyCode == KeyEvent.VK_W) {
			forward = true;
		} else if(keyCode == KeyEvent.VK_S) {
			backward = true;
		} else if(keyCode == KeyEvent.VK_A) {
			strafeLeft = true;
		} else if(keyCode == KeyEvent.VK_D) {
			strafeRight = true;
		} else if(keyCode == KeyEvent.VK_Q) {
			moveUp = true;
		} else if(keyCode == KeyEvent.VK_E) {
			moveDown = true;
		} else if(keyCode == KeyEvent.VK_LEFT) {
			panLeft = true;
		} else if(keyCode == KeyEvent.VK_RIGHT) {
			panRight = true;
		} else if(keyCode == KeyEvent.VK_UP) {
			pitchUp = true;
		} else if(keyCode == KeyEvent.VK_DOWN) {
			pitchDown = true;
		} else if(keyCode == KeyEvent.VK_L) {
			camera = !camera;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		int keyCode = e.getKeyCode();
		if (keyCode == KeyEvent.VK_W) {
			forward = false;
		} else if(keyCode == KeyEvent.VK_S) {
			backward = false;
		} else if(keyCode == KeyEvent.VK_A) {
			strafeLeft = false;
		} else if(keyCode == KeyEvent.VK_D) {
			strafeRight = false;
		} else if(keyCode == KeyEvent.VK_Q) {
			moveUp = false;
		} else if(keyCode == KeyEvent.VK_E) {
			moveDown = false;
		} else if(keyCode == KeyEvent.VK_LEFT) {
			panLeft = false;
		} else if(keyCode == KeyEvent.VK_RIGHT) {
			panRight = false;
		} else if(keyCode == KeyEvent.VK_UP) {
			pitchUp = false;
		} else if(keyCode == KeyEvent.VK_DOWN) {
			pitchDown = false;
		} 
	}
}