import java.util.concurrent.locks.Lock;

public class Semaphore {
	private int permits;

	public Semaphore(int permits) {
		this.permits = permits;
	}
	
	public synchronized void P() {
		while (permits == 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				
			}
		}
		permits--;
	}
	
	public synchronized void V() {
		permits++;
		this.notify();
	}
}
