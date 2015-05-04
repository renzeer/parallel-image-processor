import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;


public class WeirdFilter implements BufferedImageOp {

	
	public BufferedImage filter(BufferedImage src, BufferedImage dest) {
		int[][] NEIGHBORS = new int[][]{{-1,-1},{-1,0},{-1,1},{0,1},{1,1},{1,0},{1,-1},{0,-1}}; 
		
		for (int x = 0; x < src.getWidth(); x++) {
			for (int y = 0; y < src.getHeight(); y++) {
				byte[] rgbBytes = new byte[3];
				rgbBytes[0] = 0;
				rgbBytes[1] = 0;
				rgbBytes[2] = 0;
				
				int neighborCount = 0;
				for (int i = 0; i < NEIGHBORS.length; i++) {
					try {
						byte[] neighborBytes = RGB.intToBytes(src.getRGB(x+NEIGHBORS[i][0], y+NEIGHBORS[i][1]));
						neighborCount++;
						
						rgbBytes[0] += Math.max(Math.exp(neighborBytes[0]), 20) + 10*Math.cos(neighborBytes[0]);
						rgbBytes[1] += Math.min(Math.exp(neighborBytes[1]), 50);
						rgbBytes[2] += Math.min(Math.exp(neighborBytes[2]), 20);
					} catch (ArrayIndexOutOfBoundsException e) {
					}
				}
				
				rgbBytes[0] /= neighborCount;
				rgbBytes[1] /= neighborCount;
				rgbBytes[2] /= neighborCount;
				
				dest.setRGB(x, y, RGB.bytesToInt(rgbBytes));
			}
		}
		return dest;
	}


	public Rectangle2D getBounds2D(BufferedImage src) {
		return null;
	}

	public BufferedImage createCompatibleDestImage(BufferedImage src,
			ColorModel destCM) {
		return null;
	}

	public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
		return null;
	}

	public RenderingHints getRenderingHints() {
		return null;
	}

}
