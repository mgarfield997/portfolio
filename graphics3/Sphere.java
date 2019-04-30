package allVersions;

import static java.lang.Math.*;

import graphicslib3D.Point3D;
import graphicslib3D.Vector3D;
import graphicslib3D.Vertex3D;

public class Sphere {

	private int numVertices, numIndices, prec;
	private int[] indices;
	private Vertex3D[] vertices;
	
	public Sphere(int p) {
		prec = p;
		initSphere();
	}
	
	private void initSphere() {
		numVertices = (prec+1) * (prec+1);
		numIndices = prec * prec * 6;
		vertices = new Vertex3D[numVertices];
		indices = new int[numIndices];
		
		for (int i = 0; i < numVertices; i++) {
			vertices[i] = new Vertex3D();
		} //end for
		
		//calculate triangle vertices
		for (int i = 0; i <= prec; i++) {
			for (int j = 0; j <= prec; j++) {
				float y = (float) cos(toRadians(180-i*180/prec));
				float x = -(float) cos(toRadians(j*360/prec)) * (float) abs(cos(asin(y)));
				float z = (float) sin(toRadians(j*360/prec)) * (float) abs(cos(asin(y)));
				
				vertices[i*(prec+1)+j].setLocation(new Point3D(x, y, z));
				vertices[i*(prec+1)+j].setS((float)j/prec);  //texture coordinates (s,t)
				vertices[i*(prec+1)+j].setT((float)i/prec);
				vertices[i*(prec+1)+j].setNormal(new Vector3D(vertices[i*(prec+1)+j].getLocation()));
			} //end j for
		} //end i for
		
		//calulate triangle indices
		for(int i = 0; i < prec; i++) {
			for (int j = 0; j < prec; j++) {
				indices[6*(i*prec+j)+0] = i*(prec+1)+j;
				indices[6*(i*prec+j)+1] = i*(prec+1)+j+1;
				indices[6*(i*prec+j)+2] = (i+1)*(prec+1)+j;
				indices[6*(i*prec+j)+3] = i*(prec+1)+j+1;
				indices[6*(i*prec+j)+4] = (i+1)*(prec+1)+j+1;
				indices[6*(i*prec+j)+5] = (i+1)*(prec+1)+j;
			} //end j for
		} //end i for
		
	} //end init
	
	public int[] getIndices() {
		return indices;
	}
	
	public Vertex3D[] getVertices() {
		return vertices;
	}
	
} //end class
