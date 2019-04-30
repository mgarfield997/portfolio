package allVersions;

public class PentagonalPrism {

	float topX, goldenRatio, leftX, upRightX, downRightX;
	
	public PentagonalPrism() {
		topX = (float)Math.sqrt((5-Math.sqrt(5))/10); 
		
		goldenRatio = (float)(1+Math.sqrt(5))/2;
		
		leftX = -(float)Math.sqrt((5+2*Math.sqrt(5))/5);
		
		upRightX = (float) Math.sqrt((10+2*Math.sqrt(5))/5);
		
		downRightX = (float)Math.sqrt((5-Math.sqrt(5))/10);
	}
	
	public float[] getVertexPositions() {
		float[] vertexPositions = {
			topX, goldenRatio, 1.0f, leftX, 1.0f, 1.0f, leftX, -1.0f, 1.0f, //left triangle top pentagon
			topX, goldenRatio, 1.0f, leftX, -1.0f, 1.0f, downRightX, -goldenRatio, 1.0f, //middle triangle top pentagon
			topX, goldenRatio, 1.0f, downRightX, -goldenRatio, 1.0f, upRightX, 0.0f, 1.0f, //right triangle top pentagon
			
			topX, goldenRatio, 1.0f, leftX, 1.0f, 1.0f, topX, goldenRatio, -1.0f, //side 1 - top and upLeft triangle 1
			leftX, 1.0f, 1.0f, leftX, 1.0f, -1.0f, topX, goldenRatio, -1.0f, //side 1 - top and upLeft triangle 2
			
			leftX, 1.0f, 1.0f, leftX, -1.0f, 1.0f, leftX, 1.0f, -1.0f, //side 2 - upLeft and bottomLeft triangle 1
			leftX, -1.0f, 1.0f, leftX, -1.0f, -1.0f, leftX, 1.0f, -1.0f, //side 2 - upLeft and bottomLeft triangle 2
			
			leftX, -1.0f, 1.0f, downRightX, -goldenRatio, 1.0f, leftX, -1.0f, -1.0f, //side 3 - bottom triangle 1
			downRightX, -goldenRatio, 1.0f, downRightX, -goldenRatio, -1.0f, leftX, -1.0f, -1.0f, //side 3 - bottom triangle 2
			
			downRightX, -goldenRatio, 1.0f, upRightX, 0.0f, 1.0f, downRightX, -goldenRatio, -1.0f, //side 4 - upRight and bottomRight triangle 1
			upRightX, 0.0f, 1.0f, upRightX, 0.0f, -1.0f, downRightX, -goldenRatio, -1.0f, //side 4 upRight an bottomRight triangle 2
			
			upRightX, 0.0f, 1.0f, topX, goldenRatio, 1.0f, upRightX, 0.0f, -1.0f, //side 5 - top and upRight triangle 1
			topX, goldenRatio, 1.0f, topX, goldenRatio, -1.0f, upRightX, 0.0f, -1.0f, //side 5 - top and upRight triangle 2
			
			topX, goldenRatio, -1.0f, leftX, 1.0f, -1.0f, leftX, -1.0f, -1.0f, //left triangle bottom pentagon
			topX, goldenRatio, -1.0f, leftX, -1.0f, -1.0f, downRightX, -goldenRatio, -1.0f, //middle triangle bottom pentagon
			topX, goldenRatio, -1.0f, downRightX, -goldenRatio, -1.0f, upRightX, 0.0f, -1.0f, //right triangle bottom pentagon
		};
		
		return vertexPositions;
	}
	
	public float[] getTexturePositions() {
		float[] texels = {
			-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f,
			1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f,
			-1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 
			
			-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f,
			1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f,
			
			-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f,
			1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f,
			
			-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f,
			1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f,
			
			-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f,
			1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f,
			
			-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f,
			1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f,
			
			-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f,
			1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f,
			-1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f
		};
		
		return texels;
	}
}
