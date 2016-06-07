package diverse;

import drone.IDroneControl;

public class GradTest implements Runnable{
	
	boolean OPGAVE_DEBUG = false;
	private IDroneControl dc;
	protected boolean doStop = false;
	private boolean flying = false;

	
	public GradTest(IDroneControl dc) {
		this.dc = dc;
	}
	
	@Override
	public void run() {
		try {
			this.startTest();
		} catch (InterruptedException e) {
			this.destroy();
			return;
		}		
	}
	
	private void destroy() {
		doStop = true;
		flying = true;
		System.err.println("*** SKYNET DESTROYED");
		Log.writeLog("*** OpgaveAlgoritme afsluttes. Dronen forsøger at lande.");
		try {			
			dc.land();
			dc.setTimeMode(false);
		} catch (NullPointerException e){
			if(OPGAVE_DEBUG)
				System.err.println("OpgaveAlgoritme.dc er null. Der er ikke forbindelse til dronen.");
		}
		return;
	}
	
	public void startTest() throws InterruptedException {
		dc.strafeRight(700);
		Thread.sleep(500);
		dc.hover();
	}
	
}
