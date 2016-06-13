package diverse;

import java.util.ArrayList;

import billedanalyse.ColorTracker;
import billedanalyse.Squares;
import diverse.koordinat.Koordinat;
import diverse.koordinat.OpgaveRum;
import drone.IDroneControl;

public class GradTest implements Runnable{
	
	boolean OPGAVE_DEBUG = false;
	private IDroneControl dc;
	private ColorTracker ct;
	private OpgaveRum opgrum;
	protected boolean doStop = false;
	private boolean flying = false;
	private ArrayList<Koordinat> searchPoints, objectCoords;
	private ArrayList<Squares> squarePoints;
	private Koordinat landingsPlads, papKasse, baneStart;
	long stopTid, startTid;
	private double mspercm = 10.8;

	
	public GradTest(IDroneControl dc) {
		this.dc = dc;
	}
	
	@Override
	public void run() {
		squarePoints = ct.getSquares();
		this.getSquaresPositioner(squarePoints);		
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
	
//	public void startTest() throws InterruptedException {
//		dc.strafeLeft(700);
//		Thread.sleep(500);
//		dc.hover();
//	}
	
	/** Modtager et array af squares der gennemløbes og sendes videre til behandling i OpgaveRum,
	 *  som returnerer et koordinat for hver square der gennemløbes 
	 *  i square-arrayet. Herefter gennemløbes koordinat-arrayet og tjek for objekternes position udføres 
	 *  og sendes til OpgaveRum så de kan ses i GUI'en
	 * @param squares
	 */
	private void getSquaresPositioner(ArrayList<Squares> squares) {
		
		stopTid = System.currentTimeMillis();
		Koordinat dronePos = baneStart;
		
		// Opdater dronepositionen med tiden og retningen siden dronen sidst opdaterede sin position
		for(Squares item: squares) {
			long squaresdif = startTid - item.getTid();
			int afstand = (int) (squaresdif/mspercm); 

			if (baneStart.getY() < 500) {
				dronePos.setY(baneStart.getY() + afstand);
			} else if (baneStart.getY() > 500) {
				dronePos.setY(baneStart.getY() - afstand);
			}

			Koordinat objectcoord = opgrum.rotateCoordinate(item, dronePos);
			objectCoords.add(objectcoord);
		}	
	}
}
