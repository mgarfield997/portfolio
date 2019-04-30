package allVersions;


import graphicslib3D.*;
import graphicslib3D.shape.*;
import graphicslib3D.light.*;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.nio.*;
import javax.swing.*;

import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;

public class Lighting extends JFrame implements GLEventListener, KeyListener
{	/**
	 * 
	 */
	private boolean toggleOn = true;
	
	private static final long serialVersionUID = 1L;
	private GLCanvas myCanvas;
	private int rendering_programLight, rendering_programTex;
	private int vao[] = new int[1];
	private int vbo[] = new int[5];
	private Point3D torusLoc = new Point3D(0,0,-1);
	private Point3D cameraLoc = new Point3D(0,0,1);
	private float torLocX, torLocY, torLocZ;
	private GLSLUtils util = new GLSLUtils();
	
	private Torus myTorus = new Torus(0.5f, 0.2f, 48);
	private int numTorusVertices;
	private Matrix3D m_matrix = new Matrix3D();
	private Matrix3D v_matrix = new Matrix3D();
	private Matrix3D mv_matrix = new Matrix3D();
	private Matrix3D proj_matrix = new Matrix3D();
	private Matrix3D l_matrix = new Matrix3D();  // for moving the light
	
	private Material thisMaterial = Material.GOLD;
	private PositionalLight currentLight = new PositionalLight();
	private Point3D lightLoc = new Point3D(5.0f, 2.0f, 2.0f);
	private float [] globalAmbient = new float[] { 0.7f, 0.7f, 0.7f, 1.0f };
	private float amt = 0.0f;
	
	private float[] currAmbient = currentLight.getAmbient();
	private float[] currSpecular = currentLight.getSpecular();
	private float[] currDiffuse = currentLight.getDiffuse();
	
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
	
	private int objTexture;
	private Texture joglObjTexture;
	private int numObjVertices;
	private ImportedModel myObj;
	

	public Lighting()
	{	setTitle("Chapter7 - program3");
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
	
		float bkg[] = { 0.0f, 0.0f, 0.0f, 1.0f };
		FloatBuffer bkgBuffer = Buffers.newDirectFloatBuffer(bkg);
		gl.glClearBufferfv(GL_COLOR, 0, bkgBuffer);

		gl.glUseProgram(rendering_programLight);

		int mv_location = gl.glGetUniformLocation(rendering_programLight, "mv_matrix");
		int proj_location = gl.glGetUniformLocation(rendering_programLight, "proj_matrix");
		int n_location = gl.glGetUniformLocation(rendering_programLight, "norm_matrix");

		float aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		proj_matrix = perspective(50.0f, aspect, 0.1f, 1000.0f);
		
		m_matrix.setToIdentity();
		m_matrix.translate(torusLoc.getX()-0.4, torusLoc.getY(), torusLoc.getZ());
		m_matrix.rotateX(35.0f);

		v_matrix.setToIdentity();
		v_matrix.translate(-cameraLoc.getX(), -cameraLoc.getY(), -cameraLoc.getZ());
		
		keyInput();
		
		if (toggleOn) {
			
			currentLight.setAmbient(currAmbient);
			currentLight.setDiffuse(currDiffuse);
			currentLight.setSpecular(currSpecular);
			
			currentLight.setPosition(lightLoc);
			//amt += 0.5f;
			//l_matrix.setToIdentity();
			//l_matrix.rotateZ(amt);
			currentLight.setPosition(currentLight.getPosition().mult(l_matrix));
			
			installLights(v_matrix);
		} else {
			float[] off = {0.0f, 0.0f, 0.0f, 0.0f};
			
			currentLight.setAmbient(off);
			currentLight.setDiffuse(off);
			currentLight.setSpecular(off);
			
			installLights(v_matrix);
		}
		
		mv_matrix.setToIdentity();
		mv_matrix.concatenate(v_matrix);
		mv_matrix.concatenate(m_matrix);

		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_location, 1, false, proj_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(),0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, numTorusVertices);
		
		
		gl.glUseProgram(rendering_programTex);

		mv_location = gl.glGetUniformLocation(rendering_programTex, "mv_matrix");
		proj_location = gl.glGetUniformLocation(rendering_programTex, "proj_matrix");
		n_location = gl.glGetUniformLocation(rendering_programTex, "norm_matrix");
		
		m_matrix.setToIdentity();
		m_matrix.translate(torusLoc.getX()+1, torusLoc.getY(), torusLoc.getZ());
		m_matrix.rotateX(35.0f);
		
		mv_matrix.setToIdentity();
		mv_matrix.concatenate(v_matrix);
		mv_matrix.concatenate(m_matrix);
		
		gl.glUniformMatrix4fv(mv_location, 1, false, mv_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_location, 1, false, proj_matrix.getFloatValues(), 0);
		gl.glUniformMatrix4fv(n_location, 1, false, (mv_matrix.inverse()).transpose().getFloatValues(),0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, objTexture);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		int numVerts = myObj.getVertices().length;
		gl.glDrawArrays(GL_TRIANGLES, 0, numVerts);
	}

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		//setupVertices();
		//rendering_program = createShaderProgram();
		myObj = new ImportedModel("../shuttle.obj");
	
		createShaderProgram();
		setupVertices();
		
		joglObjTexture = loadTexture("Objects/spstob_1.jpg");
		objTexture = joglObjTexture.getTextureObject();
		
	}
	
	private void installLights(Matrix3D v_matrix)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		Material currentMaterial = thisMaterial;
		
		Point3D lightP = currentLight.getPosition();
		Point3D lightPv = lightP.mult(v_matrix);
		float [] viewspaceLightPos = new float[] { (float) lightPv.getX(), (float) lightPv.getY(), (float) lightPv.getZ() };

		// set the current globalAmbient settings
		int globalAmbLoc = gl.glGetUniformLocation(rendering_programLight, "globalAmbient");
		gl.glProgramUniform4fv(rendering_programLight, globalAmbLoc, 1, globalAmbient, 0);
	
		// get the locations of the light and material fields in the shader
		int ambLoc = gl.glGetUniformLocation(rendering_programLight, "light.ambient");
		int diffLoc = gl.glGetUniformLocation(rendering_programLight, "light.diffuse");
		int specLoc = gl.glGetUniformLocation(rendering_programLight, "light.specular");
		int posLoc = gl.glGetUniformLocation(rendering_programLight, "light.position");
		int MambLoc = gl.glGetUniformLocation(rendering_programLight, "material.ambient");
		int MdiffLoc = gl.glGetUniformLocation(rendering_programLight, "material.diffuse");
		int MspecLoc = gl.glGetUniformLocation(rendering_programLight, "material.specular");
		int MshiLoc = gl.glGetUniformLocation(rendering_programLight, "material.shininess");
	
		//  set the uniform light and material values in the shader
		gl.glProgramUniform4fv(rendering_programLight, ambLoc, 1, currentLight.getAmbient(), 0);
		gl.glProgramUniform4fv(rendering_programLight, diffLoc, 1, currentLight.getDiffuse(), 0);
		gl.glProgramUniform4fv(rendering_programLight, specLoc, 1, currentLight.getSpecular(), 0);
		gl.glProgramUniform3fv(rendering_programLight, posLoc, 1, viewspaceLightPos, 0);
		gl.glProgramUniform4fv(rendering_programLight, MambLoc, 1, currentMaterial.getAmbient(), 0);
		gl.glProgramUniform4fv(rendering_programLight, MdiffLoc, 1, currentMaterial.getDiffuse(), 0);
		gl.glProgramUniform4fv(rendering_programLight, MspecLoc, 1, currentMaterial.getSpecular(), 0);
		gl.glProgramUniform1f(rendering_programLight, MshiLoc, currentMaterial.getShininess());
	}

	private void setupVertices()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		Vertex3D[] vertices = myTorus.getVertices();
		int[] indices = myTorus.getIndices();
		
		float[] fvalues = new float[indices.length*3];
		float[] tvalues = new float[indices.length*2];
		float[] nvalues = new float[indices.length*3];
		
		for (int i=0; i<indices.length; i++)
		{	fvalues[i*3] = (float) (vertices[indices[i]]).getX();
			fvalues[i*3+1] = (float) (vertices[indices[i]]).getY();
			fvalues[i*3+2] = (float) (vertices[indices[i]]).getZ();
			tvalues[i*2] = (float) (vertices[indices[i]]).getS();
			tvalues[i*2+1] = (float) (vertices[indices[i]]).getT();
			nvalues[i*3] = (float) (vertices[indices[i]]).getNormalX();
			nvalues[i*3+1]= (float)(vertices[indices[i]]).getNormalY();
			nvalues[i*3+2]=(float) (vertices[indices[i]]).getNormalZ();
		}
		
		numTorusVertices = indices.length;
		
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
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(fvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit()*4,norBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer vertBufObj = Buffers.newDirectFloatBuffer(pvaluesObj);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBufObj.limit()*4, vertBufObj, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvaluesObj);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer norBufObj = Buffers.newDirectFloatBuffer(nvaluesObj);
		gl.glBufferData(GL_ARRAY_BUFFER, norBufObj.limit()*4,norBufObj, GL_STATIC_DRAW);
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
		r.setElementAt(3,3,0.0f);
		return r;
	}

	public static void main(String[] args) { new Lighting(); }
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
	public void dispose(GLAutoDrawable drawable) {}

	private void createShaderProgram()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		/*String vshaderSource[] = util.readShaderSource("LightingShader/vert.shader");
		String fshaderSource[] = util.readShaderSource("LightingShader/frag.shader");

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
		return vfprogram; */
	
		String vshaderSource[] = util.readShaderSource("Objects/vert2.shader");
		String fshaderSource[] = util.readShaderSource("Objects/frag2.shader");
		String vLightshaderSource[] = util.readShaderSource("LightingShader/vert.shader");
		String fLightshaderSource[] = util.readShaderSource("LightingShader/frag.shader");
		
		int vShader = gl.glCreateShader(GL_VERTEX_SHADER);
		int fShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
		int vLightShader = gl.glCreateShader(GL_VERTEX_SHADER);
		int fLightShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
	
		gl.glShaderSource(vShader, vshaderSource.length, vshaderSource, null, 0);
		gl.glShaderSource(fShader, fshaderSource.length, fshaderSource, null, 0);
		gl.glShaderSource(vLightShader, vLightshaderSource.length, vLightshaderSource, null, 0);
		gl.glShaderSource(fLightShader, fLightshaderSource.length, fLightshaderSource, null, 0);
		
		gl.glCompileShader(vShader);
		gl.glCompileShader(fShader);
		gl.glCompileShader(vLightShader);
		gl.glCompileShader(fLightShader);
	
		rendering_programLight = gl.glCreateProgram();
		rendering_programTex = gl.glCreateProgram();
		
		gl.glAttachShader(rendering_programTex, vShader);
		gl.glAttachShader(rendering_programTex, fShader);
		gl.glAttachShader(rendering_programLight, vLightShader);
		gl.glAttachShader(rendering_programLight, fLightShader);
		
		gl.glLinkProgram(rendering_programLight);
		gl.glLinkProgram(rendering_programTex);
	}
	
	public Texture loadTexture(String textureFileName)
	{	Texture tex = null;
		try { tex = TextureIO.newTexture(new File(textureFileName), false); }
		catch (Exception e) { e.printStackTrace(); }
		return tex;
	}
	
	public void keyInput() {
		double movement = 0.3f;
		if (forward) {
			l_matrix.translate(0, 0, movement);
		} else if (backward) {
			l_matrix.translate(0, 0, -movement);
		} else if (strafeLeft) {
			l_matrix.translate(-movement, 0, 0);
		} else if (strafeRight) {
			l_matrix.translate(movement, 0, 0);
		} else if (moveUp) {
			l_matrix.translate(0, movement, 0);
		} else if (moveDown) {
			l_matrix.translate(0, -movement, 0);
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