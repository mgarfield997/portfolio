package allVersions;

import graphicslib3D.*;
import graphicslib3D.GLSLUtils.*;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.nio.*;
import javax.swing.*;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_CCW;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.common.nio.Buffers;

public class SolarSystem extends JFrame implements GLEventListener, KeyListener
{
	private static final long serialVersionUID = 1L;
	private GLCanvas myCanvas;
	private int rendering_program;
	private int vao[] = new int[1];
	private int vbo[] = new int[15];
	
	private float cameraX, cameraY, cameraZ;
	private float sphLocX, sphLocY, sphLocZ;
	
	private GLSLUtils util = new GLSLUtils();
	private Sphere sphere = new Sphere(48);
	private PentagonalPrism pentagon = new PentagonalPrism();
	
	//look at values
	Vector3D eye = new Vector3D(-cameraX, -cameraY, -cameraZ);
	Vector3D lookingAt = new Vector3D(sphLocX, sphLocY, sphLocZ);
	Vector3D up = new Vector3D(0,1,0);
	
	Vector3D cameraUp;
	Vector3D cameraRight;
	Vector3D cameraFwd;
	
	//textures
	private int sunTexture;
	private Texture joglSunTexture;
	private int earthTexture;
	private Texture jogleEarthTexture;
	private int marsTexture;
	private Texture jogleMarsTexture;
	private int moonEarthTexture;
	private Texture jogleMoonEarthTexture;
	private int moonMarsTexture;
	private Texture jogleMoonMarsTexture;
	private int axesRedTexture;
	private Texture jogleAxesRedTexture;
	private int axesBlueTexture;
	private Texture jogleAxesBlueTexture;
	private int axesGreenTexture;
	private Texture jogleAxesGreenTexture;
	private int prismTexture;
	private Texture joglePrismTexture;
	private int venusTexture;
	private Texture jogleVenusTexture;
	
	//camera movement values
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
	private boolean toggle;
	
	float pan;
	float pitch;
	
	//Skybox variables
	private String[] CvertShaderSource, CfragShaderSource;
	private int SkyboxTexID;
	
	//Imported Object variables
	private float objLocX, objLocY, objLocZ;
	private int spongebobTexture;
	private Texture joglSpongebobTexture;
	private int numObjVertices;
	private int numObjVerticesStar;
	private ImportedModel myObj;
	private ImportedModel myObjStar;
	private int patrickTexture;
	private Texture joglPatrickTexture;
	
	//Lighting variables
	private float [] globalAmbient = new float[] { 0.7f, 0.7f, 0.7f, 1.0f };
	
	private	MatrixStack mvStack = new MatrixStack(20);

	public SolarSystem()
	{	setTitle("Chapter4 - program4");
		setSize(900, 700);
		GLProfile profile=GLProfile.get(GLProfile.GL4);
		GLCapabilities capabilities=new GLCapabilities(profile);
		myCanvas = new GLCanvas(capabilities);
		myCanvas.addGLEventListener(this);
		getContentPane().add(myCanvas);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setVisible(true);
		FPSAnimator animator = new FPSAnimator(myCanvas, 50);
		animator.start();
		addKeyListener(this);
	}

	public void display(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		keyInput();

		gl.glClear(GL_DEPTH_BUFFER_BIT);
		float bkg[] = { 0.0f, 0.0f, 0.0f, 1.0f };
		FloatBuffer bkgBuffer = Buffers.newDirectFloatBuffer(bkg);
		gl.glClearBufferfv(GL_COLOR, 0, bkgBuffer);

		gl.glClear(GL_DEPTH_BUFFER_BIT);

		gl.glUseProgram(rendering_program);

		int mv_loc = gl.glGetUniformLocation(rendering_program, "mv_matrix");
		int proj_loc = gl.glGetUniformLocation(rendering_program, "proj_matrix");

		float aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		Matrix3D pMat = perspective(60.0f, aspect, 0.1f, 1000.0f);

		// push view matrix onto the stack
		mvStack.pushMatrix();
		
		gl.glUniformMatrix4fv(proj_loc, 1, false, pMat.getFloatValues(), 0);
		
		
		cameraFwd = new Vector3D(
			(Math.cos(Math.toRadians(pitch)) * Math.cos(Math.toRadians(pan))),
			(Math.sin(Math.toRadians(pitch))),
			(Math.cos(Math.toRadians(pitch)) * Math.sin(Math.toRadians(pan)))
		);
		
		Matrix3D lookAtMat = lookAt(eye, lookingAt, up);
		mvStack.pushMatrix();
		mvStack.multMatrix(lookAtMat);
		gl.glUniformMatrix4fv(mv_loc, 1, false, lookAtMat.getFloatValues(), 0);

		double amt = (double)(System.currentTimeMillis())/1000.0;
	
		//----------------------- axes
		if (toggle) {
			//X axis
			mvStack.pushMatrix();
			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
			gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(0);
			gl.glEnable(GL_DEPTH_TEST);
			
			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
			gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(1);
			
			gl.glActiveTexture(GL_TEXTURE0);
			gl.glBindTexture(GL_TEXTURE_2D, axesRedTexture);
			
			gl.glDrawArrays(GL_LINES, 0, 2);
			
			//Y axis
			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
			gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(0);
			gl.glEnable(GL_DEPTH_TEST);
			
			gl.glActiveTexture(GL_TEXTURE0);
			gl.glBindTexture(GL_TEXTURE_2D, axesGreenTexture);
			
			gl.glDrawArrays(GL_LINES, 0, 2);
			
			//Z axis
			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
			gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(0);
			gl.glEnable(GL_DEPTH_TEST);
			
			gl.glActiveTexture(GL_TEXTURE0);
			gl.glBindTexture(GL_TEXTURE_2D, axesBlueTexture);
			
			gl.glDrawArrays(GL_LINES, 0, 2);
			mvStack.popMatrix(); 
		}
		
		//----------------------- skybox
		mvStack.pushMatrix();
		//mvStack.translate(sphLocX, sphLocY, sphLocZ);
		mvStack.scale(600, 600, 600);
		gl.glUniformMatrix4fv(mv_loc, 1, false, mvStack.peek().getFloatValues(), 0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, SkyboxTexID);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);	// cube is CW, but we are viewing the inside
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		gl.glEnable(GL_DEPTH_TEST);
		
		mvStack.popMatrix();
		
		//----------------------- sphere == sun
		int numVerts = sphere.getIndices().length;
		mvStack.pushMatrix();
		mvStack.translate(sphLocX, sphLocY, sphLocZ);
		mvStack.pushMatrix();
		mvStack.rotate((System.currentTimeMillis())/10.0,0.0,1.0,0.0);
		mvStack.scale(1.3, 1.3, 1.3);
		gl.glUniformMatrix4fv(mv_loc, 1, false, mvStack.peek().getFloatValues(), 0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glEnable(GL_DEPTH_TEST);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, sunTexture);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);

		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
		mvStack.popMatrix(); 
		
		//----------------------- Imported Object - spongebob
		mvStack.pushMatrix();
		mvStack.translate(Math.sin(amt)*3.0f, 0.0f, Math.cos(amt)*3.0f);
		mvStack.scale(.05, .05, .05);
		//mvStack.pushMatrix();
		gl.glUniformMatrix4fv(mv_loc, 1, false, mvStack.peek().getFloatValues(), 0);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, spongebobTexture);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		int numVertsObj = myObj.getVertices().length;
		gl.glDrawArrays(GL_TRIANGLES, 0, numVertsObj);
		mvStack.popMatrix();
		
		//----------------------- Imported Object - patrick
		/*mvStack.pushMatrix();
		
		mvStack.translate(Math.sin(amt)*4.0f, 0.0f, Math.cos(amt)*4.0f);
		//mvStack.scale(.05, .05, .05);
		//mvStack.pushMatrix();
		gl.glUniformMatrix4fv(mv_loc, 1, false, mvStack.peek().getFloatValues(), 0);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[13]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[14]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, patrickTexture);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		int numVertsStar = myObjStar.getVertices().length;
		gl.glDrawArrays(GL_TRIANGLES, 0, numVertsStar);
		
		mvStack.popMatrix(); mvStack.popMatrix(); */
		
		//-----------------------  planet 1 - earth
		mvStack.pushMatrix();
		mvStack.translate(Math.sin(amt)*5.0f, 0.0f, Math.cos(amt)*5.0f);
		mvStack.pushMatrix();
		mvStack.rotate((System.currentTimeMillis())/10.0,0.1,0.0,0.0);
		mvStack.scale(0.75, 0.75, 0.75);
		gl.glUniformMatrix4fv(mv_loc, 1, false, mvStack.peek().getFloatValues(), 0);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
			
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1); 
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, earthTexture);
		
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
		mvStack.popMatrix();

		//-----------------------  moon - for earth
		mvStack.pushMatrix();
		mvStack.translate(0.0f, Math.sin(amt*.95)*2.0f, Math.cos(amt*.95)*2.0f);
		mvStack.rotate((System.currentTimeMillis())/10.0,0.0,0.0,1.0);
		mvStack.scale(0.25, 0.25, 0.25);
		gl.glUniformMatrix4fv(mv_loc, 1, false, mvStack.peek().getFloatValues(), 0);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1); 
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, moonEarthTexture);
		
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts); 
		mvStack.popMatrix(); mvStack.popMatrix();  
		
		//------------------------- planet 2 - mars
		mvStack.pushMatrix();
		mvStack.translate(Math.sin(amt*.65)*6.5f, -Math.sin(amt*.65)*2.5, Math.cos(amt*.65)*6.5f);
		mvStack.pushMatrix();
		mvStack.rotate((System.currentTimeMillis())/9.0,0.0,1.0,0.0);
		mvStack.scale(0.55, 0.55, 0.55);
		gl.glUniformMatrix4fv(mv_loc, 1, false, mvStack.peek().getFloatValues(), 0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1); 
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, marsTexture);
		
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
		mvStack.popMatrix();
		
		//-------------------------- moon 2 - for mars
		mvStack.pushMatrix();
		mvStack.translate(0.0f, Math.sin(amt*.45)*0.95f, Math.cos(amt*.45)*0.95f);
		mvStack.rotate((System.currentTimeMillis())/10.0,0.0,0.0,1.0);
		mvStack.scale(0.2, 0.2, 0.2);
		gl.glUniformMatrix4fv(mv_loc, 1, false, mvStack.peek().getFloatValues(), 0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1); 
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, moonMarsTexture);
		
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts); 
		mvStack.popMatrix(); mvStack.popMatrix();
		
		//--------------------------- prism
		mvStack.pushMatrix();
		mvStack.translate(Math.sin(amt*.85)*2.0f, Math.sin(amt*.85)*3.0f, Math.cos(amt*.85)*3.0f);
		mvStack.rotate((System.currentTimeMillis())/10.0,1.0,1.0,0.0);
		mvStack.scale(0.5, 0.5, 0.5);
		gl.glUniformMatrix4fv(mv_loc, 1, false, mvStack.peek().getFloatValues(), 0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1); 
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, prismTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

		gl.glDisable(GL_CULL_FACE);
		//gl.glFrontFace(GL_CCW); 
		
		gl.glDrawArrays(GL_TRIANGLES, 0, pentagon.getVertexPositions().length); 
		mvStack.popMatrix();
		
		//---------------------------- Venus
		mvStack.pushMatrix();
		mvStack.translate(Math.sin(amt*.35)*-6.0f, Math.sin(amt*.35)*8.0f, Math.cos(amt*.35)*8.0f);
		mvStack.rotate((System.currentTimeMillis())/10.0,1.0,1.0,0.0);
		gl.glUniformMatrix4fv(mv_loc, 1, false, mvStack.peek().getFloatValues(), 0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1); 
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, venusTexture);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW); 
		
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts); 
		mvStack.popMatrix();
		
		
		mvStack.popMatrix(); mvStack.popMatrix(); mvStack.popMatrix();
		//mvStack.popMatrix();
		
	}

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		myObj = new ImportedModel("../spongebob.obj");
		myObjStar = new ImportedModel("../patrick.obj");
		rendering_program = createShaderProgram();
		setupVertices();
		cameraX = 0.0f; cameraY = 0.0f; cameraZ = 15.0f;
		sphLocX = 0.0f; sphLocY = 0.0f; sphLocZ = 0.0f;
		
		eye = new Vector3D(cameraX, cameraY, cameraZ);
		lookingAt = new Vector3D(0.0f, 0.0f, 0.0f);
		up = new Vector3D(0.0f, 1.0f, 0.0f);
		
		pitch = 0.0f;
		pan = -90.0f;
		
		joglSunTexture = loadTexture("shaderSolarSystem/sun.jpg");
		sunTexture = joglSunTexture.getTextureObject();
		
		jogleEarthTexture = loadTexture("shaderSolarSystem/earth.jpg");
		earthTexture = jogleEarthTexture.getTextureObject();
		
		jogleMarsTexture = loadTexture("shaderSolarSystem/mars.jpg");
		marsTexture = jogleMarsTexture.getTextureObject();
		
		jogleMoonEarthTexture = loadTexture("shaderSolarSystem/moonEarth.jpg");
		moonEarthTexture = jogleMoonEarthTexture.getTextureObject();
		
		jogleMoonMarsTexture = loadTexture("shaderSolarSystem/moonMars.jpg");
		moonMarsTexture = jogleMoonMarsTexture.getTextureObject();
		
		jogleAxesRedTexture = loadTexture("shaderSolarSystem/redLine.jpeg");
		axesRedTexture = jogleAxesRedTexture.getTextureObject();
		
		jogleAxesGreenTexture = loadTexture("shaderSolarSystem/greenLine.jpg");
		axesGreenTexture = jogleAxesGreenTexture.getTextureObject();
		
		jogleAxesBlueTexture = loadTexture("shaderSolarSystem/blueLine.jpg");
		axesBlueTexture = jogleAxesBlueTexture.getTextureObject();
		
		joglePrismTexture = loadTexture("shaderSolarSystem/prism.jpg");
		prismTexture = joglePrismTexture.getTextureObject();		
		
		jogleVenusTexture = loadTexture("shaderSolarSystem/venus.jpg");
		venusTexture = jogleVenusTexture.getTextureObject();		
		
		Texture skybox = loadTexture("Skybox3/bikiniBottom2.jpg");
		SkyboxTexID = skybox.getTextureObject();
		
		joglSpongebobTexture = loadTexture("Objects/sponge.png");
		spongebobTexture = joglSpongebobTexture.getTextureObject();
		
		joglPatrickTexture = loadTexture("Objects/patrick.png");
		patrickTexture = joglPatrickTexture.getTextureObject();
		
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
	
		float[] axesX = {8.0f, 0.0f, 0.0f, -8.0f, 0.0f, 0.0f};
		float[] axesY = {0.0f, 8.0f, 0.0f, 0.0f, -8.0f, 0.0f};
		float[] axesZ = {0.0f, 0.0f, 8.0f, 0.0f, 0.0f, -8.0f};
		
		float[] axesTex = { 1.0f, 1.0f, -1.0f, 1.0f };
		
		float[] prism_positions = pentagon.getVertexPositions();
		
		Vertex3D[] vertices = sphere.getVertices();
		int[] indices = sphere.getIndices();
		
		float[] pvalues = new float[indices.length*3];
		float[] tvalues = new float[indices.length*2];
		float[] nvalues = new float[indices.length*3];
		
		for (int i = 0; i < indices.length; i++) {
			pvalues[i*3] = (float) (vertices[indices[i]]).getX();
			pvalues[i*3+1] = (float) (vertices[indices[i]]).getY();
			pvalues[i*3+2] = (float) (vertices[indices[i]]).getZ();
			
			tvalues[i*2] = (float) (vertices[indices[i]]).getS();
			tvalues[i*2+1] = (float) (vertices[indices[i]]).getT();
			
			nvalues[i*3] = (float) (vertices[indices[i]]).getNormalX();
			nvalues[i*3+1] = (float) (vertices[indices[i]]).getNormalY();
			nvalues[i*3+2] = (float) (vertices[indices[i]]).getNormalZ();
		}
		
		float[] prismTex = pentagon.getTexturePositions();
		
		//Imported Object
		Vertex3D[] verticesObj = myObj.getVertices();
		numObjVertices = myObj.getNumVertices();
		
		float[] pvaluesObj = new float[numObjVertices*3];
		float[] tvaluesObj = new float[numObjVertices*2];
		float[] nvaluesObj = new float[numObjVertices*3];
		
		for (int i=0; i<numObjVertices; i++)
		{	pvaluesObj[i*3]   = (float) (verticesObj[i]).getX();
			pvaluesObj[i*3+1] = (float) (verticesObj[i]).getY();
			pvaluesObj[i*3+2] = (float) (verticesObj[i]).getZ();
			tvaluesObj[i*2]   = (float) (verticesObj[i]).getS();
			tvaluesObj[i*2+1] = (float) (verticesObj[i]).getT();
			nvaluesObj[i*3]   = (float) (verticesObj[i]).getNormalX();
			nvaluesObj[i*3+1] = (float) (verticesObj[i]).getNormalY();
			nvaluesObj[i*3+2] = (float) (verticesObj[i]).getNormalZ();
		}
		
		Vertex3D[] verticesObjStar = myObjStar.getVertices();
		numObjVerticesStar = myObjStar.getNumVertices();
		
		float[] pvaluesStar = new float[numObjVerticesStar*3];
		float[] tvaluesStar = new float[numObjVerticesStar*2];
		float[] nvaluesStar = new float[numObjVerticesStar*3];
		
		for (int i=0; i<numObjVertices; i++)
		{	pvaluesStar[i*3]   = (float) (verticesObjStar[i]).getX();
			pvaluesStar[i*3+1] = (float) (verticesObjStar[i]).getY();
			pvaluesStar[i*3+2] = (float) (verticesObjStar[i]).getZ();
			tvaluesStar[i*2]   = (float) (verticesObjStar[i]).getS();
			tvaluesStar[i*2+1] = (float) (verticesObjStar[i]).getT();
			nvaluesStar[i*3]   = (float) (verticesObjStar[i]).getNormalX();
			nvaluesStar[i*3+1] = (float) (verticesObjStar[i]).getNormalY();
			nvaluesStar[i*3+2] = (float) (verticesObjStar[i]).getNormalZ();
		}
		

		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer pentBuf = Buffers.newDirectFloatBuffer(prism_positions);
		gl.glBufferData(GL_ARRAY_BUFFER, pentBuf.limit()*4, pentBuf, GL_STATIC_DRAW); 
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL_STATIC_DRAW); 
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer axesXBuf = Buffers.newDirectFloatBuffer(axesX);
		gl.glBufferData(GL_ARRAY_BUFFER, axesXBuf.limit()*4, axesXBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		FloatBuffer axesTexBuf = Buffers.newDirectFloatBuffer(axesTex);
		gl.glBufferData(GL_ARRAY_BUFFER, axesTexBuf.limit()*4, axesTexBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer prismTexBuf = Buffers.newDirectFloatBuffer(prismTex);
		gl.glBufferData(GL_ARRAY_BUFFER, prismTexBuf.limit()*4, prismTexBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		FloatBuffer axesYBuf = Buffers.newDirectFloatBuffer(axesY);
		gl.glBufferData(GL_ARRAY_BUFFER, axesYBuf.limit()*4, axesYBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		FloatBuffer axesZBuf = Buffers.newDirectFloatBuffer(axesZ);
		gl.glBufferData(GL_ARRAY_BUFFER, axesZBuf.limit()*4, axesZBuf, GL_STATIC_DRAW);
		
		//Skybox
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		FloatBuffer cubeVertBuf = Buffers.newDirectFloatBuffer(cube_vertices);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeVertBuf.limit()*4, cubeVertBuf, GL_STATIC_DRAW);
	
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		FloatBuffer cubeTexBuf = Buffers.newDirectFloatBuffer(cube_texture_coord);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeTexBuf.limit()*4, cubeTexBuf, GL_STATIC_DRAW);
		
		//Imported Object
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		FloatBuffer vertBufObj = Buffers.newDirectFloatBuffer(pvaluesObj);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBufObj.limit()*4, vertBufObj, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		FloatBuffer texBufObj = Buffers.newDirectFloatBuffer(tvaluesObj);
		gl.glBufferData(GL_ARRAY_BUFFER, texBufObj.limit()*4, texBufObj, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[12]);
		FloatBuffer norBufObj = Buffers.newDirectFloatBuffer(nvaluesObj);
		gl.glBufferData(GL_ARRAY_BUFFER, norBufObj.limit()*4,norBufObj, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[13]);
		FloatBuffer vertBufStar = Buffers.newDirectFloatBuffer(pvaluesStar);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBufStar.limit()*4, vertBufStar, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[14]);
		FloatBuffer texBufStar = Buffers.newDirectFloatBuffer(tvaluesStar);
		gl.glBufferData(GL_ARRAY_BUFFER, texBufStar.limit()*4, texBufStar, GL_STATIC_DRAW);
	}

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
		return r;
	}

	public static void main(String[] args) { new SolarSystem(); }
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
	public void dispose(GLAutoDrawable drawable) {}

	private int createShaderProgram()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();

		String vshaderSource[] = util.readShaderSource("shaderSolarSystem/vert2.shader");
		String fshaderSource[] = util.readShaderSource("shaderSolarSystem/frag2.shader");

		int vShader = gl.glCreateShader(GL_VERTEX_SHADER);
		int fShader = gl.glCreateShader(GL_FRAGMENT_SHADER);

		gl.glShaderSource(vShader, vshaderSource.length, vshaderSource, null, 0);
		gl.glShaderSource(fShader, fshaderSource.length, fshaderSource, null, 0);

		gl.glCompileShader(vShader);
		gl.glCompileShader(fShader);

		int vfprogram = gl.glCreateProgram();
		gl.glAttachShader(vfprogram, vShader);
		gl.glAttachShader(vfprogram, fShader);
		
		gl.glLinkProgram(vfprogram);
		return vfprogram;
	}
	
	public Texture loadTexture(String textureFileName)
	{	Texture tex = null;
		try { tex = TextureIO.newTexture(new File(textureFileName), false); }
		catch (Exception e) { e.printStackTrace(); }
		return tex;
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
	
	public void keyInput() {
		double movement = 0.3f;
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
	}

	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub	
	}

	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		//pressed(e.getKeyCode());
		int keyCode = e.getKeyCode();
		if (keyCode == KeyEvent.VK_W) {
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
		} else if(keyCode == KeyEvent.VK_SPACE) {
			toggle = !toggle;
		}
	}

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