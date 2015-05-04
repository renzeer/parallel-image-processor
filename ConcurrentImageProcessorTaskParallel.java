import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;

import com.jhlabs.image.InvertFilter;
import com.jhlabs.image.OilFilter;
import com.jhlabs.image.SmearFilter;


public class ConcurrentImageProcessorTaskParallel {
	private MyThread reader, writer;
	private CIPSemaphore semaphore;
	private ArrayList<BufferedImage> rpList, pwList;
	private static final String[] FILTERS = {"smear", "oil6", "oil1", "invert", "weird"};
	
	private JFrame mainFrame;
    private JPanel controlPanel;
    private JProgressBar progressBar;
    
    private final int RP_PERMIT = 0;
    private final int PW_PERMIT = 1;
    private final int RP_LOCK = 2;
    private final int PW_LOCK = 3;
    
    private volatile boolean finished;
    
	
	public ConcurrentImageProcessorTaskParallel(String f, File td, long ot, int threads) {
		showProgressBar();
		semaphore = new CIPSemaphore();
		rpList = new ArrayList<BufferedImage>();
		pwList = new ArrayList<BufferedImage>();
	    reader = new MyThread(semaphore, null, rpList, f, td, ot, 0);
	    writer = new MyThread(semaphore, pwList, null, f, td, ot, 2);
	    reader.start();
	    writer.start();
	    for (int i = 0; i < threads; i++) {
	    	new MyThread(semaphore, rpList, pwList, f, td, ot, 1).start();
	    }
	    finished = false;
	    
	}
	
	private class CIPSemaphore {
		private int[] resources;

		public CIPSemaphore() {
			resources = new int[] {0, 0, 1, 1};
		}
		
		public synchronized void P(int type) {
			while (resources[type] == 0) {
				try {
					wait();
				} catch (InterruptedException e) {
					
				}
			}
			resources[type]--;

		}
		
		public synchronized void V(int type) {
			resources[type]++;
			this.notify();
		}
	}

	private class MyThread extends Thread {
		private CIPSemaphore semaphore;
		private ArrayList<BufferedImage> inList, outList;
		private String filter;
		private File targetDir;
		private int type;
		long cReadTime, readTime, cProcessTime, processTime, cWriteTime, writeTime, overallTime;

		public MyThread(CIPSemaphore s, ArrayList<BufferedImage> il, ArrayList<BufferedImage> ol, String f, File td, long ot, int t) {
			semaphore = s;
			inList = il;
			outList = ol;
			filter = f;
			targetDir = td;
			type = t;
			cReadTime = readTime = cProcessTime = processTime = cWriteTime = writeTime = 0;
			overallTime = ot;
		}

		@Override
		public void run() {
			int counter = 0;
			File[] files = targetDir.listFiles(myFilter);
			progressBar.setMaximum(Math.max(files.length-1,1));
			for (File f : files) {
				if (type == 0) {
					cReadTime = System.currentTimeMillis();
                    try {
                    	semaphore.P(RP_LOCK);
						outList.add(ImageIO.read(f));
						semaphore.V(RP_LOCK);
					} catch (IOException e) {
						e.printStackTrace();
					}
                    readTime += System.currentTimeMillis() - cReadTime;
                    while (outList.size() > 7) {
                    	try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
                    }
                    semaphore.V(RP_PERMIT);
                    System.out.print("r");
                    
				} else if (type == 1) {
					if (inList.isEmpty() && finished) {
						break;
					}
					semaphore.P(RP_PERMIT);
					cProcessTime = System.currentTimeMillis();
					semaphore.P(RP_LOCK);
					BufferedImage input = inList.remove(inList.size()-1);
					semaphore.V(RP_LOCK);
					BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
					BufferedImageOp imageFilter = new OilFilter();
					switch (filter) {
						case "oil1":
							((OilFilter)imageFilter).setRange(1);
							break;
						case "oil6":
							((OilFilter)imageFilter).setRange(6);
							break;
						case "smear":
							imageFilter = new SmearFilter();
							((SmearFilter)imageFilter).setShape(0);
							break;
						case "invert":
							imageFilter = new InvertFilter();
							break;
						case "weird":
							imageFilter = new WeirdFilter();
							break;
					}
                    imageFilter.filter(input,output);
                    semaphore.P(PW_LOCK);
                    outList.add(output);
                    semaphore.V(PW_LOCK);
                    processTime += System.currentTimeMillis() - cProcessTime;
                    semaphore.V(PW_PERMIT);
                    System.out.print("p");
	
				} else {
					semaphore.P(PW_PERMIT);
					cWriteTime = System.currentTimeMillis();
					semaphore.P(PW_LOCK);
					BufferedImage output = inList.remove(inList.size()-1);
					semaphore.V(PW_LOCK);
                    saveImage(output, targetDir + "/" + filter + "_" + f.getName());
                    writeTime += System.currentTimeMillis() - cWriteTime;
                    System.out.print("w");
                    progressBar.setValue(progressBar.getValue()+1);

					
				}
			}
			if (type == 0) {
				finished = true;
				System.out.println("\nTime spent reading: " + readTime/1000.0 + "sec.");
			} else if (type == 1) {
				System.out.println("Time spent processing: " + processTime/1000.0 + "sec.");
			} else {
				System.out.println("Time spent writing: " + writeTime/1000.0 + "sec.");
				overallTime = System.currentTimeMillis() - overallTime;
				System.out.println("Overall execution time: " + overallTime/1000.0 + "sec.");
			}
		}
	}
	
	private static void saveImage(BufferedImage image, String filename){
		try {
			ImageIO.write(image, "jpg", new File(filename));
		} catch (IOException e) {
			System.out.println("Cannot write file "+filename);
			System.exit(1);
		}
	}
	
	private static FilenameFilter myFilter = new FilenameFilter() {

        @Override
        public boolean accept(final File dir, final String name) {
            if (name.matches("image_(.*).jpg")) {
                return (true);
            }
            return (false);
        }
    };
     
     private void showProgressBar(){

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        
        mainFrame = new JFrame("Concurrent Image Processor");
        mainFrame.setSize(350,100);
        mainFrame.setLayout(new GridLayout(3, 1));
        mainFrame.addWindowListener(new WindowAdapter() {
           public void windowClosing(WindowEvent windowEvent){
              System.exit(0);
           }        
        });             

        controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());
        controlPanel.add(progressBar);

        mainFrame.add(controlPanel);
        mainFrame.setVisible(true); 
        mainFrame.setVisible(true);  
     }
	
	public static void main(String args[]) {
		long overallTime = System.currentTimeMillis();
		File dir = null;
		String filter = null;
		int threads = 1;
		
		if (args.length == 2) {
			filter = args[0];
			dir = new File(args[1]);
		} else if (args.length == 3) {
			threads = Integer.parseInt(args[0]);
			filter = args[1];
			dir = new File(args[2]);
		} else {
			System.err.println("Usage: java ConcurrentImageProcessorTaskParallel [# of threads] <filter> <list of filenames>");
			System.exit(1);
		}
		
		
		if (dir.isDirectory() && Arrays.asList(FILTERS).contains(filter)) {
			final ConcurrentImageProcessorTaskParallel cip = new ConcurrentImageProcessorTaskParallel(args[1], dir, overallTime, threads);
			/* Didn't work with invokeLater()
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					cip.showProgressBar();
			    }
			});*/
		}
		
	}
}
