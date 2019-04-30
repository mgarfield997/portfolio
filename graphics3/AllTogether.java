package allVersions;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import graphicslib3D.*;
import graphicslib3D.light.PositionalLight;
import graphicslib3D.shape.Sphere;
import graphicslib3D.shape.Torus;
//import project3.Models.ImportedModel;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.nio.FloatBuffer;

import static com.jogamp.opengl.GL4.*;

public class AllTogether extends JFrame implements GLEventListener, KeyListener
{	private GLCanvas myCanvas;
	private Material thisMaterial;
	private String[] vBlinn1ShaderSource, vBlinn2ShaderSource, fBlinn1ShaderSource, fBlinn2ShaderSource;
	private int rendering_program1, rendering_program2;
	private int vao[] = new int[1];
	private int vbo[] = new int[15];
	private int mv_location, proj_location, vertexLoc, n_location;
	private float aspect;
	private GLSLUtils util = new GLSLUtils();
	
	// location of torus and camera
	private double x = System.nanoTime()/1000.0;
	private Point3D sphLoc = new Point3D(0.0, 3.0*Math.sin(x), 0.0);
	private Point3D cameraLoc = new Point3D(0.0, 10.0, 15.0);
	private Point3D lightLoc = new Point3D(10, 10.0f, 10);

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
	private Matrix3D b = new Matrix3D();

	// model stuff
	private Torus myTorus = new Torus(0.6f, 0.4f, 48);
	private Sphere mySphere = new Sphere(24);
	private int numPyramidVertices, numTorusVertices;

	//camera movement
	private boolean light;
	private boolean zoomIn;
	private boolean zoomOut;
	private boolean strafeLeft; //a – move the camera a small amount in the negative-U direction (also called “strafe left”).
	private boolean strafeRight; //d – move the camera a small amount in the positive-U direction (also called “strafe right”).
	private boolean moveDown;//e – move the camera a small amount in the negative-V direction (“move down”).
	private boolean moveUp;//q – move the camera a small amount in the positive-V direction (“move up”).
	private boolean panLeft;//(left and right arrow) – rotate the camera by a small amount left/right around its V axis (“pan”).
	private boolean panRight;
	private boolean pitchUp;//(up and down arrow) – rotate the camera by a small amount up/down around its U axis (“pitch).
	private boolean pitchDown;
	private boolean lightLeft; //j – move the light a small amount in the negative-U direction (also called “strafe left”).
	private boolean lightRight; //l – move the light a small amount in the positive-U direction (also called “strafe right”).
	private boolean lightDown;//o – move the light a small amount in the negative-V direction (“move down”).
	private boolean lightUp;//u – move the light a small amount in the positive-V direction (“move up”).
	private boolean lightForward;//i – move the light a small amount in the negative-V direction (“move down”).
	private boolean lightBackward;//k – move the light a small amount in the positive-V direction (“move up”).

	Vector3D cameraUp;
	Vector3D cameraRight;
	Vector3D cameraFwd;

	Vector3D eye;
	Vector3D target;
	Vector3D up;

	float yaw;
	float pitch;

	//skybox stuff
	private int textureID1, textureID2;
	private String[] CvertShaderSource, CfragShaderSource;
	private int rendering_program_skybox;

	//imported model stuff
	private int numObjVertices;
	private ImportedModel myObj;
	private int objectTexture;
	private Texture joglObjectTexture;
	private String[] importvshaderSource, importfshaderSource;
	private int rendering_program_import;
	private Point3D objectLoc = new Point3D(0,0,-1);


	public AllTogether()
	{	setTitle("Lights, Shadows, Everything Combined....");
		setSize(2000, 1500);
		GLProfile profile=GLProfile.get(GLProfile.GL4);
		GLCapabilities capabilities=new GLCapabilities(profile);
		myCanvas = new GLCanvas(capabilities);
		myCanvas.addGLEventListener(this);
		getContentPane().add(myCanvas);
		setVisible(true);
		FPSAnimator animator = new FPSAnimator(myCanvas, 30);
		animator.start();
		addKeyListener(this);
		light = true;
	}

	public void display(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		handleInput();
		x = System.nanoTime()/1000000000.0;
		sphLoc = new Point3D(0.0, 3 + Math.sin(x), 0.0);
		currentLight.setPosition(lightLoc);

		float depthClearVal[] = new float[1];
		depthClearVal[0] = 1.0f;
		gl.glClearBufferfv(GL_DEPTH, 0, depthClearVal,0);

		//  build the PROJECTION matrix
		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		proj_matrix = perspective(60.0f, aspect, 0.1f, 1000.0f);

		//  build the VIEW matrix
		v_matrix.setToIdentity();
		v_matrix.translate(-cameraLoc.getX(),-cameraLoc.getY(),-cameraLoc.getZ());
		Vector3D target = new Vector3D(0,0,0);
		Vector3D up = new Vector3D(0,1,0);
		cameraFwd = new Vector3D(
				(Math.cos(Math.toRadians(pitch)) * Math.cos(Math.toRadians(yaw))),
				(Math.sin(Math.toRadians(pitch))),
				(Math.cos(Math.toRadians(pitch)) * Math.sin(Math.toRadians(yaw)))
		);
		v_matrix = lookAt(eye, target, up);

		// draw the skybox --------------------------------------------------------------------------------------
		gl.glUseProgram(rendering_program_skybox);

		//  build the MODEL matrix
		m_matrix.setToIdentity();
		m_matrix.translate(eye.getX(),eye.getY(),eye.getZ());

		//  build the MODEL-VIEW matrix
		mv_matrix.setToIdentity();
		mv_matrix.concatenate(v_matrix);
		mv_matrix.concatenate(m_matrix);

		//  put the MV and PROJ matrices into the corresponding uniforms
		mv_location = gl.glGetUniformLocation(rendering_program_skybox, "mv_matrix");
		proj_location = gl.glGetUniformLocation(rendering_program_skybox, "proj_matrix");
		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_location, 1, false, proj_matrix.getFloatValues(), 0);

		// set up vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// set up texture coordinates buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		// activate the skybox texture
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, textureID2);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);	// cube is CW, but we are viewing the inside
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		gl.glEnable(GL_DEPTH_TEST);

		// draw scene ---------------------------------------------------------------------------------------
		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		proj_matrix = perspective(50.0f, aspect, 0.1f, 1000.0f);

		gl.glBindFramebuffer(GL_FRAMEBUFFER, shadow_buffer[0]);
		gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadow_tex[0], 0);
	
		gl.glDrawBuffer(GL_NONE);
		gl.glEnable(GL_DEPTH_TEST);

		gl.glEnable(GL_POLYGON_OFFSET_FILL);	// for reducing
		gl.glPolygonOffset(2.0f, 4.0f);		//  shadow artifacts

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
	
		lightV_matrix = lookAtLight(currentLight.getPosition(), origin, up);	// vector from light to origin
		lightP_matrix = perspective(50.0f, aspect, 0.1f, 1000.0f);

		// draw the sphere --------------------------------------------------------------------------------------
		m_matrix.setToIdentity();
		m_matrix.translate(sphLoc.getX(), sphLoc.getY(), sphLoc.getZ());
		m_matrix.rotateX(25.0);
		
		shadowMVP1.setToIdentity();
		shadowMVP1.concatenate(lightP_matrix);
		shadowMVP1.concatenate(lightV_matrix);
		shadowMVP1.concatenate(m_matrix);
		int shadow_location = gl.glGetUniformLocation(rendering_program1, "shadowMVP");
		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP1.getFloatValues(), 0);
		
		// set up sphere vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);	
	
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, mySphere.getIndices().length);

		// draw the imported object ---------------------------------------------------------------------------
		m_matrix.setToIdentity();
		m_matrix.translate(objectLoc.getX(), objectLoc.getY(), objectLoc.getZ());
		//m_matrix.rotateX(35.0f);
		double x = (double) (System.currentTimeMillis())/10000.0;  // time factor
		m_matrix.rotate(100*x, 100*x, 100*x);
//		m_matrix.rotateY(135.0f);

		shadowMVP1.setToIdentity();
		shadowMVP1.concatenate(lightP_matrix);
		shadowMVP1.concatenate(lightV_matrix);
		shadowMVP1.concatenate(m_matrix);

		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP1.getFloatValues(), 0);

		//set up imported model vertices
		int numVerts = myObj.getVertices().length;
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);//Vertices
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);

		// ---- draw the floor ----------------------------------------------------------------------------------
		//  build the MODEL matrix
		m_matrix.setToIdentity();
		m_matrix.translate(0,0,0);

		shadowMVP1.setToIdentity();
		shadowMVP1.concatenate(lightP_matrix);
		shadowMVP1.concatenate(lightV_matrix);
		shadowMVP1.concatenate(m_matrix);

		gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP1.getFloatValues(), 0);

		// set up vertices buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, 6);
	}
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public void passTwo()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		//  build the VIEW matrix
		v_matrix.setToIdentity();
		v_matrix.translate(-cameraLoc.getX(),-cameraLoc.getY(),-cameraLoc.getZ());
		Vector3D target = new Vector3D(0,0,0);
		Vector3D up = new Vector3D(0,1,0);
		cameraFwd = new Vector3D(
				(Math.cos(Math.toRadians(pitch)) * Math.cos(Math.toRadians(yaw))),
				(Math.sin(Math.toRadians(pitch))),
				(Math.cos(Math.toRadians(pitch)) * Math.sin(Math.toRadians(yaw)))
		);
		v_matrix = lookAt(eye, target, up);

		// draw the floor
		gl.glUseProgram(rendering_program2);
		thisMaterial = Material.SILVER;
		installLights(rendering_program2, v_matrix);

		mv_location = gl.glGetUniformLocation(rendering_program2, "mv_matrix");
		proj_location = gl.glGetUniformLocation(rendering_program2, "proj_matrix");
		n_location = gl.glGetUniformLocation(rendering_program2, "normalMat");
		int shadow_location = gl.glGetUniformLocation(rendering_program2,  "shadowMVP");

		//  build the MODEL matrix
		m_matrix.setToIdentity();
		m_matrix.translate(0,0,0);

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
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// set up normals buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, 6);

		if(light){
			// draw the sphere for the light
			thisMaterial = Material.GOLD;

			mv_location = gl.glGetUniformLocation(rendering_program2, "mv_matrix");
			proj_location = gl.glGetUniformLocation(rendering_program2, "proj_matrix");
			n_location = gl.glGetUniformLocation(rendering_program2, "normalMat");
			shadow_location = gl.glGetUniformLocation(rendering_program2,  "shadowMVP");

			//  build the MODEL matrix
			m_matrix.setToIdentity();
			m_matrix.translate(lightLoc.getX(),lightLoc.getY()+1,lightLoc.getZ());
			m_matrix.scale(0.1,0.1,0.1);

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

			// set up sphere vertices buffer
			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
			gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(0);

			// set up sphere normals buffer
			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
			gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(1);

			gl.glClear(GL_DEPTH_BUFFER_BIT);
			gl.glEnable(GL_CULL_FACE);
			gl.glFrontFace(GL_CCW);
			gl.glEnable(GL_DEPTH_TEST);
			gl.glDepthFunc(GL_LEQUAL);

			gl.glDrawArrays(GL_TRIANGLES, 0, mySphere.getIndices().length);
		}

		// draw the sphere --------------------------------------------------------------------------------------
		gl.glUseProgram(rendering_program_import);
		thisMaterial = Material.BRONZE;
		installLights(rendering_program_import, v_matrix);

		mv_location = gl.glGetUniformLocation(rendering_program_import, "mv_matrix");
		proj_location = gl.glGetUniformLocation(rendering_program_import, "proj_matrix");
		n_location = gl.glGetUniformLocation(rendering_program_import, "norm_matrix");
		shadow_location = gl.glGetUniformLocation(rendering_program_import,  "shadowMVP");

		//  build the MODEL matrix
		m_matrix.setToIdentity();
		m_matrix.translate(sphLoc.getX(), sphLoc.getY(), sphLoc.getZ());
		m_matrix.rotateX(25.0);

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


		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);//Vertices
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);//Textures
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, objectTexture);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);//Normals
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, mySphere.getIndices().length);


		//draw the imported object -------------------------------------------------------------------------------
		gl.glUseProgram(rendering_program_import);
		thisMaterial = Material.GOLD;
		installLights(rendering_program_import, v_matrix);

		mv_location = gl.glGetUniformLocation(rendering_program_import, "mv_matrix");
		proj_location = gl.glGetUniformLocation(rendering_program_import, "proj_matrix");
		n_location = gl.glGetUniformLocation(rendering_program_import, "norm_matrix");
		shadow_location = gl.glGetUniformLocation(rendering_program_import,  "shadowMVP");

		m_matrix.setToIdentity();
		m_matrix.translate(objectLoc.getX(), objectLoc.getY(), objectLoc.getZ());
		double x = (double) (System.currentTimeMillis())/10000.0;  // time factor
		m_matrix.rotate(100*x, 100*x, 100*x);

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
		int numVerts = myObj.getVertices().length;
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);//Vertices
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);//Textures
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, textureID1);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);//Normals
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
	}

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		createShaderPrograms();
		setupShadowBuffers();

		//imported object
		myObj = new ImportedModel("../spongebob.obj");
		joglObjectTexture = loadTexture("Objects/sponge.png");
		objectTexture = joglObjectTexture.getTextureObject();
		objectLoc.setX(5.0); objectLoc.setY(5.0f); objectLoc.setZ(0.0f);
		setupVertices();

		Texture t = loadTexture("Skybox/brick1.jpg");
		textureID1 = t.getTextureObject();

		t = loadTexture("Skybox3/bikiniBottom2");
		textureID2 = t.getTextureObject();

		eye = new Vector3D(cameraLoc.getX(), cameraLoc.getY(), cameraLoc.getZ());
		target = new Vector3D(0.0f, 0.0f, 0.0f);
		up = new Vector3D(0.0f, 1.0f, 0.0f);

		pitch = 0.0f;
		yaw = -90.0f;
				
		b.setElementAt(0,0,0.5);b.setElementAt(0,1,0.0);b.setElementAt(0,2,0.0);b.setElementAt(0,3,0.5f);
		b.setElementAt(1,0,0.0);b.setElementAt(1,1,0.5);b.setElementAt(1,2,0.0);b.setElementAt(1,3,0.5f);
		b.setElementAt(2,0,0.0);b.setElementAt(2,1,0.0);b.setElementAt(2,2,0.5);b.setElementAt(2,3,0.5f);
		b.setElementAt(3,0,0.0);b.setElementAt(3,1,0.0);b.setElementAt(3,2,0.0);b.setElementAt(3,3,1.0f);
		
		// may reduce shadow border artifacts
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
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

		// may reduce shadow border artifacts
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}

// -----------------------------
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		setupShadowBuffers();
	}

	private void setupVertices()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();

		// plane definition
		float[] floor_vertex_positions =
				{
						-10, 0, 10, 10, 0, 10, -10, 0, -10,
						10, 0, 10, 10, 0, -10, -10, 0, -10
				};

		float[] floor_normals =
				{
						0, 1, 0, 0, 1, 0, 0, 1, 0,
						0, 1, 0, 0, 1, 0, 0, 1, 0
				};

	
		// sphere definition
		Vertex3D[] sphere_vertices = mySphere.getVertices();
		int[] sphere_indices = mySphere.getIndices();

		float[] pvalues = new float[sphere_indices.length*3];
		float[] tvalues = new float[sphere_indices.length*2];
		float[] nvalues = new float[sphere_indices.length*3];

		for (int i=0; i<sphere_indices.length; i++)
		{	pvalues[i*3] = (float) (sphere_vertices[sphere_indices[i]]).getX();
			pvalues[i*3+1] = (float) (sphere_vertices[sphere_indices[i]]).getY();
			pvalues[i*3+2] = (float) (sphere_vertices[sphere_indices[i]]).getZ();
			tvalues[i*2] = (float) (sphere_vertices[sphere_indices[i]]).getS();
			tvalues[i*2+1] = (float) (sphere_vertices[sphere_indices[i]]).getT();
			nvalues[i*3] = (float) (sphere_vertices[sphere_indices[i]]).getNormalX();
			nvalues[i*3+1]= (float)(sphere_vertices[sphere_indices[i]]).getNormalY();
			nvalues[i*3+2]=(float) (sphere_vertices[sphere_indices[i]]).getNormalZ();
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

		// imported model
		Vertex3D[] vertices = myObj.getVertices();
		numObjVertices = myObj.getNumVertices();

		float[] importpvalues = new float[numObjVertices*3];
		float[] importtvalues = new float[numObjVertices*2];
		float[] importnvalues = new float[numObjVertices*3];

		for (int i=0; i<numObjVertices; i++)
		{	importpvalues[i*3]   = (float) (vertices[i]).getX();
			importpvalues[i*3+1] = (float) (vertices[i]).getY();
			importpvalues[i*3+2] = (float) (vertices[i]).getZ();
			importtvalues[i*2]   = (float) (vertices[i]).getS();
			importtvalues[i*2+1] = (float) (vertices[i]).getT();
			importnvalues[i*3]   = (float) (vertices[i]).getNormalX();
			importnvalues[i*3+1] = (float) (vertices[i]).getNormalY();
			importnvalues[i*3+2] = (float) (vertices[i]).getNormalZ();
		}

		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);

		gl.glGenBuffers(vbo.length, vbo, 0);

		//  put the Torus vertices into the first buffer,
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(torus_fvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);
		
		//  load the sphere vertices into the second buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer sphereVertBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, sphereVertBuf.limit()*4, sphereVertBuf, GL_STATIC_DRAW);
		
		// load the torus normal coordinates into the third buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer torusNorBuf = Buffers.newDirectFloatBuffer(torus_nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, torusNorBuf.limit()*4, torusNorBuf, GL_STATIC_DRAW);
		
		// load the sphere normal coordinates into the fourth buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit()*4, norBuf, GL_STATIC_DRAW);

		// load the floor vertices into the fifth buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer floorBuf = Buffers.newDirectFloatBuffer(floor_vertex_positions);
		gl.glBufferData(GL_ARRAY_BUFFER, floorBuf.limit()*4, floorBuf, GL_STATIC_DRAW);

		// load the floor normals into the sixth buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		FloatBuffer floorNorBuf = Buffers.newDirectFloatBuffer(floor_normals);
		gl.glBufferData(GL_ARRAY_BUFFER, floorNorBuf.limit()*4, floorNorBuf, GL_STATIC_DRAW);

		// load the cube vertex coordinates into the seventh buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		FloatBuffer cubeVertBuf = Buffers.newDirectFloatBuffer(cube_vertices);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeVertBuf.limit()*4, cubeVertBuf, GL_STATIC_DRAW);

		// load the cube texture coordinates into the eight buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		FloatBuffer cubeTexBuf = Buffers.newDirectFloatBuffer(cube_texture_coord);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeTexBuf.limit()*4, cubeTexBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		FloatBuffer importvertBuf = Buffers.newDirectFloatBuffer(importpvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, importvertBuf.limit()*4, importvertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		FloatBuffer importtexBuf = Buffers.newDirectFloatBuffer(importtvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, importtexBuf.limit()*4, importtexBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		FloatBuffer importnorBuf = Buffers.newDirectFloatBuffer(importnvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, importnorBuf.limit()*4, importnorBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		FloatBuffer sphereTexBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, sphereTexBuf.limit()*4, sphereTexBuf, GL_STATIC_DRAW);
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

		float [] noLight = new float[] { (float) 0.0f,
				(float) 0.0f,
				(float) 0.0f };

		// get the location of the global ambient light field in the shader
		int globalAmbLoc = gl.glGetUniformLocation(rendering_program, "globalAmbient");

		// set the current globalAmbient settings
		gl.glProgramUniform4fv(rendering_program, globalAmbLoc, 1, globalAmbient, 0);

		if(light) {
			// get the locations of the light and material fields in the shader
			int ambLoc = gl.glGetUniformLocation(rendering_program, "light.ambient");
			int diffLoc = gl.glGetUniformLocation(rendering_program, "light.diffuse");
			int specLoc = gl.glGetUniformLocation(rendering_program, "light.specular");
			int posLoc = gl.glGetUniformLocation(rendering_program, "light.position");

			// set the uniform light and material values in the shader
			gl.glProgramUniform4fv(rendering_program, ambLoc, 1, currentLight.getAmbient(), 0);
			gl.glProgramUniform4fv(rendering_program, diffLoc, 1, currentLight.getDiffuse(), 0);
			gl.glProgramUniform4fv(rendering_program, specLoc, 1, currentLight.getSpecular(), 0);
			gl.glProgramUniform3fv(rendering_program, posLoc, 1, currLightPos, 0);
		}
		else {
			// get the locations of the light and material fields in the shader
			int ambLoc = gl.glGetUniformLocation(rendering_program, "light.ambient");
			int diffLoc = gl.glGetUniformLocation(rendering_program, "light.diffuse");
			int specLoc = gl.glGetUniformLocation(rendering_program, "light.specular");
			int posLoc = gl.glGetUniformLocation(rendering_program, "light.position");

			// set the uniform light and material values in the shader
			gl.glProgramUniform4fv(rendering_program, ambLoc, 1, noLight, 0);
			gl.glProgramUniform4fv(rendering_program, diffLoc, 1, noLight, 0);
			gl.glProgramUniform4fv(rendering_program, specLoc, 1, noLight, 0);
			gl.glProgramUniform3fv(rendering_program, posLoc, 1, currLightPos, 0);
		}
		int MambLoc = gl.glGetUniformLocation(rendering_program, "material.ambient");
		int MdiffLoc = gl.glGetUniformLocation(rendering_program, "material.diffuse");
		int MspecLoc = gl.glGetUniformLocation(rendering_program, "material.specular");
		int MshiLoc = gl.glGetUniformLocation(rendering_program, "material.shininess");
		gl.glProgramUniform4fv(rendering_program, MambLoc, 1, currentMaterial.getAmbient(), 0);
		gl.glProgramUniform4fv(rendering_program, MdiffLoc, 1, currentMaterial.getDiffuse(), 0);
		gl.glProgramUniform4fv(rendering_program, MspecLoc, 1, currentMaterial.getSpecular(), 0);
		gl.glProgramUniform1f(rendering_program, MshiLoc, currentMaterial.getShininess());
	}

	public static void main(String[] args) { new AllTogether(); }

	@Override
	public void dispose(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) drawable.getGL();
		gl.glDeleteVertexArrays(1, vao, 0);
	}

//-----------------
	private void createShaderPrograms()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();

		vBlinn1ShaderSource = util.readShaderSource("AllTogether/blinnVert1.shader");
		vBlinn2ShaderSource = util.readShaderSource("AllTogether/blinnVert2.shader");
		fBlinn1ShaderSource = util.readShaderSource("AllTogether/blinnFrag1.shader");
		fBlinn2ShaderSource = util.readShaderSource("AllTogether/blinnFrag2.shader");
		CvertShaderSource = util.readShaderSource("AllTogether/vert.shader");
		CfragShaderSource = util.readShaderSource("AllTogether/frag.shader");
		importvshaderSource = util.readShaderSource("AllTogether/vert2.shader");
		importfshaderSource = util.readShaderSource("AllTogether/frag2.shader");

		int vertexShader1 = gl.glCreateShader(GL_VERTEX_SHADER);
		int vertexShader2 = gl.glCreateShader(GL_VERTEX_SHADER);
		int fragmentShader1 = gl.glCreateShader(GL_FRAGMENT_SHADER);
		int fragmentShader2 = gl.glCreateShader(GL_FRAGMENT_SHADER);
		int CvertexShader = gl.glCreateShader(GL_VERTEX_SHADER);
		int CfragmentShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
		int importvShader = gl.glCreateShader(GL_VERTEX_SHADER);
		int importfShader = gl.glCreateShader(GL_FRAGMENT_SHADER);

		gl.glShaderSource(vertexShader1, vBlinn1ShaderSource.length, vBlinn1ShaderSource, null, 0);
		gl.glShaderSource(vertexShader2, vBlinn2ShaderSource.length, vBlinn2ShaderSource, null, 0);
		gl.glShaderSource(fragmentShader1, fBlinn1ShaderSource.length, fBlinn1ShaderSource, null, 0);
		gl.glShaderSource(fragmentShader2, fBlinn2ShaderSource.length, fBlinn2ShaderSource, null, 0);
		gl.glShaderSource(CvertexShader, CvertShaderSource.length, CvertShaderSource, null, 0);
		gl.glShaderSource(CfragmentShader, CfragShaderSource.length, CfragShaderSource, null, 0);
		gl.glShaderSource(importvShader, importvshaderSource.length, importvshaderSource, null, 0);
		gl.glShaderSource(importfShader, importfshaderSource.length, importfshaderSource, null, 0);

		gl.glCompileShader(vertexShader1);
		gl.glCompileShader(vertexShader2);
		gl.glCompileShader(fragmentShader1);
		gl.glCompileShader(fragmentShader2);
		gl.glCompileShader(CvertexShader);
		gl.glCompileShader(CfragmentShader);
		gl.glCompileShader(importvShader);
		gl.glCompileShader(importfShader);

		rendering_program1 = gl.glCreateProgram();
		rendering_program2 = gl.glCreateProgram();
		rendering_program_skybox = gl.glCreateProgram();
		rendering_program_import = gl.glCreateProgram();

		gl.glAttachShader(rendering_program1, vertexShader1);
		gl.glAttachShader(rendering_program1, fragmentShader1);
		gl.glAttachShader(rendering_program2, vertexShader2);
		gl.glAttachShader(rendering_program2, fragmentShader2);
		gl.glAttachShader(rendering_program_skybox, CvertexShader);
		gl.glAttachShader(rendering_program_skybox, CfragmentShader);
		gl.glAttachShader(rendering_program_import, importvShader);
		gl.glAttachShader(rendering_program_import, importfShader);

		gl.glLinkProgram(rendering_program1);
		gl.glLinkProgram(rendering_program2);
		gl.glLinkProgram(rendering_program_skybox);
		gl.glLinkProgram(rendering_program_import);

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

	public Texture loadTexture(String textureFileName)
	{	Texture tex = null;
		try { tex = TextureIO.newTexture(new File(textureFileName), false); }
		catch (Exception e) { e.printStackTrace(); }
		return tex;
	}

	private Matrix3D lookAtLight(Point3D eye, Point3D target, Vector3D y)
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

	private Matrix3D lookAt(Vector3D eyeV, Vector3D targetV, Vector3D y)
	{
		Matrix3D look = new Matrix3D();
		cameraRight = (cameraFwd.cross(y)).normalize();
		cameraUp = (cameraRight.cross(cameraFwd)).normalize();

		look.setElementAt(0, 0, cameraRight.getX());
		look.setElementAt(1, 0, cameraUp.getX());
		look.setElementAt(2, 0, -cameraFwd.getX());
		look.setElementAt(3, 0, 0);
		look.setElementAt(0, 1, cameraRight.getY());
		look.setElementAt(1, 1, cameraUp.getY());
		look.setElementAt(2, 1, -cameraFwd.getY());
		look.setElementAt(3, 1, 0);
		look.setElementAt(0, 2, cameraRight.getZ());
		look.setElementAt(1, 2, cameraUp.getZ());
		look.setElementAt(2, 2, -cameraFwd.getZ());
		look.setElementAt(3, 2, 0);
		look.setElementAt(0, 3, cameraRight.dot(eyeV.mult(-1)));
		look.setElementAt(1, 3, cameraUp.dot(eyeV.mult(-1)));
		look.setElementAt(2, 3, (cameraFwd.mult(-1)).dot(eyeV.mult(-1)));
		look.setElementAt(3, 3, 1);
		return look;
	}

	public void handleInput() {
		double cameraSpeed = 0.4f;
		if (zoomIn) {
			eye = eye.add(cameraFwd.mult(cameraSpeed));
		}
		if(zoomOut) {
			eye = eye.add(cameraFwd.mult(-cameraSpeed));
		}
		if (strafeLeft) {
			eye = eye.add(cameraRight.mult(-cameraSpeed));
		}
		if (strafeRight) {
			eye = eye.add(cameraRight.mult(cameraSpeed));
		}
		if (moveUp) {
			eye = eye.add(cameraUp.mult(cameraSpeed));
		}
		if (moveDown) {
			eye = eye.add(cameraUp.mult(-cameraSpeed));
		}
		if (panLeft) {
			yaw -= 1.0f;
		}
		if(panRight) {
			yaw += 1.0f;
		}
		if(pitchUp) {
			pitch += 1.0f;
			if (pitch > 90.0f)
			{
				pitch = 90.0f;
			}
		}
		if(pitchDown) {
			pitch -= 1.0f;
			if (pitch < -90.0f)
			{
				pitch = -90.0f;
			}
		}
		Point3D lightSpeedLeft = new Point3D(-1,0,0);
		Point3D lightSpeedRight = new Point3D(1,0,0);
		Point3D lightSpeedUp = new Point3D(0,1,0);
		Point3D lightSpeedDown = new Point3D(0,-1,0);
		Point3D lightSpeedForward = new Point3D(0,0,-1);
		Point3D lightSpeedBackward = new Point3D(0,0,1);
		if(light) { // if the light is present, you can move it
			if (lightLeft) {
				lightLoc = lightLoc.add(lightSpeedLeft);
			}
			if (lightRight) {
				lightLoc = lightLoc.add(lightSpeedRight);
			}
			if (lightUp) {
				lightLoc = lightLoc.add(lightSpeedUp);
			}
			if (lightDown) {
				lightLoc = lightLoc.add(lightSpeedDown);
			}
			if (lightForward) {
				lightLoc = lightLoc.add(lightSpeedForward);
			}
			if (lightBackward) {
				lightLoc = lightLoc.add(lightSpeedBackward);
			}
		}
	}

	public void setLight(boolean b) {
		light = b;
	}

	public void keyPress(int keyCode) {
		if ((keyCode == KeyEvent.VK_W)) {
			zoomIn = true;
		}
		else if ((keyCode == KeyEvent.VK_S)) {
			zoomOut = true;
		}
		else if ((keyCode == KeyEvent.VK_A)) {
			strafeLeft = true;
		}
		else if ((keyCode == KeyEvent.VK_D)) {
			strafeRight = true;
		}
		else if ((keyCode == KeyEvent.VK_Q)) {
			moveUp = true;
		}
		else if ((keyCode == KeyEvent.VK_E)) {
			moveDown = true;
		}
		else if ((keyCode == KeyEvent.VK_LEFT)) {
			panLeft = true;
		}
		else if ((keyCode == KeyEvent.VK_RIGHT)) {
			panRight = true;
		}
		else if ((keyCode == KeyEvent.VK_UP)) {
			pitchUp = true;
		}
		else if ((keyCode == KeyEvent.VK_DOWN)) {
			pitchDown = true;
		}
		else if ((keyCode == KeyEvent.VK_SPACE)) {
			setLight(!light);
		}
		else if ((keyCode == KeyEvent.VK_J)) {
			lightLeft = true;
		}
		else if ((keyCode == KeyEvent.VK_L)) {
			lightRight = true;
		}
		else if ((keyCode == KeyEvent.VK_U)) {
			lightUp = true;
		}
		else if ((keyCode == KeyEvent.VK_O)) {
			lightDown = true;
		}
		else if ((keyCode == KeyEvent.VK_I)) {
			lightForward = true;
		}
		else if ((keyCode == KeyEvent.VK_K)) {
			lightBackward = true;
		}

	}

	public void keyRelease(int keyCode) {
		if ((keyCode == KeyEvent.VK_W)) {
			zoomIn = false;
		}
		else if ((keyCode == KeyEvent.VK_S)) {
			zoomOut = false;
		}
		else if ((keyCode == KeyEvent.VK_A)) {
			strafeLeft = false;
		}
		else if ((keyCode == KeyEvent.VK_D)) {
			strafeRight = false;
		}
		else if ((keyCode == KeyEvent.VK_Q)) {
			moveUp = false;
		}
		else if ((keyCode == KeyEvent.VK_E)) {
			moveDown = false;
		}
		else if ((keyCode == KeyEvent.VK_LEFT)) {
			panLeft = false;
		}
		else if ((keyCode == KeyEvent.VK_RIGHT)) {
			panRight = false;
		}
		else if ((keyCode == KeyEvent.VK_UP)) {
			pitchUp = false;
		}
		else if ((keyCode == KeyEvent.VK_DOWN)) {
			pitchDown = false;
		}
		else if ((keyCode == KeyEvent.VK_J)) {
			lightLeft = false;
		}
		else if ((keyCode == KeyEvent.VK_L)) {
			lightRight = false;
		}
		else if ((keyCode == KeyEvent.VK_U)) {
			lightUp = false;
		}
		else if ((keyCode == KeyEvent.VK_O)) {
			lightDown = false;
		}
		else if ((keyCode == KeyEvent.VK_I)) {
			lightForward = false;
		}
		else if ((keyCode == KeyEvent.VK_K)) {
			lightBackward = false;
		}
	}

	// key event
	public void keyTyped(KeyEvent key) {}
	public void keyPressed(KeyEvent key) {
		keyPress(key.getKeyCode());
	}
	public void keyReleased(KeyEvent key) {
		keyRelease(key.getKeyCode());
	}
}
