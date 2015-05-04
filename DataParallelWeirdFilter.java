import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.util.ArrayList;


public class DataParallelWeirdFilter implements BufferedImageOp {
	private int numThreads;
	int[][] NEIGHBORS = new int[][]{{-1,-1},{-1,0},{-1,1},{0,1},{1,1},{1,0},{1,-1},{0,-1}}; 
	
	public class FilterThread extends Thread {
		int startIndex;
		BufferedImage src, dest;
		
		public FilterThread(int startIndex, BufferedImage src, BufferedImage dest) {
			this.startIndex = startIndex;
			this.src = src;
			this.dest = dest;
		}
		
		public void run() {
			int endIndex = (startIndex + (src.getWidth()/4) > src.getWidth()-1) ? src.getWidth()-1 : startIndex + (src.getWidth()/4);
			for (int x = startIndex; x <= endIndex; x++) {
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
							//System.out.println("(" + x + ", " + y + ")");
						}
					}
					
					rgbBytes[0] /= neighborCount;
					rgbBytes[1] /= neighborCount;
					rgbBytes[2] /= neighborCount;
					
					//System.out.println("(" + x + ", " + y + ")");
					
					try {
						dest.setRGB(x, y, RGB.bytesToInt(rgbBytes));
					} catch (ArrayIndexOutOfBoundsException e) {
						//System.out.println("(" + x + ", " + y + ")");
					}
				}
			}
		}
	}
	
	public DataParallelWeirdFilter(int numThreads) {
		this.numThreads = numThreads;
	}
	
	public BufferedImage filter(BufferedImage src, BufferedImage dest) {
		ArrayList<FilterThread> ftList = new ArrayList<FilterThread>();
		for (int i = 0; i < numThreads; i++) {
			int startIndex = ((src.getWidth()/numThreads)*i)+i;
			ftList.add(new FilterThread(startIndex, src, dest));
			ftList.get(ftList.size()-1).start();
		}
		
		for (int i = 0; i < ftList.size(); i++) {
			try {
				ftList.get(i).join();
				//System.out.println("Thread" + i + " done!");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
