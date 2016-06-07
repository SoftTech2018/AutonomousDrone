package drone;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import billedanalyse.IBilledAnalyse;
import billedanalyse.QRCodeScanner;
import billedanalyse.Squares.FARVE;
import de.yadrone.base.command.LEDAnimation;
import diverse.koordinat.Genstand;
import diverse.koordinat.Koordinat;
import diverse.Log;
import diverse.koordinat.OpgaveRum;
import diverse.PunktNavigering;
import diverse.QrFirkant;
import diverse.circleCalc.Vector2;

public class OpgaveAlgoritme2 implements Runnable {

	/*
	 * Markør hvor der kan udskrives debug-beskeder i konsollen.
	 */
	protected final boolean OPGAVE_DEBUG = true;
	private int searchTime = 30000; // Max søgetid i ms når der ikke kan findes et target. Eks: 60000 = 60 sek.

	private IDroneControl dc;
	private IBilledAnalyse ba;
	private OpgaveRum opgrum;
	private DroneHelper dh;
	private PunktNavigering punktNav;
	private QRCodeScanner qrcs;
	private PunktNavigering pn;
	protected boolean doStop = false;
	private boolean flying = false;
	private Vector2 dronePunkt = null;
	private ArrayList<Koordinat> searchPoints;
	private Koordinat landingsPlads, papKasse;


	public OpgaveAlgoritme2(IDroneControl dc, IBilledAnalyse ba){
		this.dc = dc;
		this.ba = ba;
		this.punktNav = new PunktNavigering();
		this.createPoints();
	}

	/**
	 * WARNING - USE AT OWN RISK
	 * This method will attempt to start SKYNET
	 * WARNING - USE AT OWN RISK
	 * @throws InterruptedException 
	 */
//	public void startOpgaveAlgoritme() throws InterruptedException {
//		Log.writeLog("*** OpgaveAlgoritmen startes. ***");
//		dc.setTimeMode(true);
//		dc.setFps(15);
//		while (!doStop) {
//			if(Thread.interrupted()){
//				destroy();
//				return;
//			}
//			boolean img = false;
//			while(!flying){
//				if(Thread.interrupted()){
//					destroy();
//					return;
//				}
//				// Hvis dronen ikke er klar og videostream ikke er tilgængeligt, venter vi 500 ms mere
//				if(!dc.isReady() || (img = ba.getImages() == null)){
//					if(ba.getImages()==null){
//						img = false;						
//					}
//					if(OPGAVE_DEBUG){
//						System.out.println("Drone klar: " + dc.isReady()+ ", Billeder modtages: " + img);					
//					}
//					try {
//						Thread.sleep(500);
//					} catch (InterruptedException e) {
//						destroy();
//						return;
//					}
//					continue; // start forfra i while-løkke
//				} else {// Dronen er klar til at letter
//					System.err.println("*** WARNING - SKYNET IS ONLINE");
//					Log.writeLog("*Dronen letter autonomt.");
//					flying = true;
//					dc.takeoff();
//
//					if(OPGAVE_DEBUG){
//						System.err.println("*** Dronen letter og starter opgaveløsning ***");
//					}
//					try {
//						Thread.sleep(5000);
//					} catch (InterruptedException e) {
//						destroy();
//						return;
//					}
//				}
//			}
//
//			// Find position og gem position som landingsplads
//			landingsPlads = findDronePos();
//
//			// Find papkasse-position
//
//			this.dh = new DroneHelper(dc, papKasse);
//
//			// Flyv til start
//			dh.flyTo(landingsPlads, this.searchPoints.get(0));
//
//			// Roter drone (mod vinduet)
//			dc.turnDrone(90-dc.getFlightData()[2]);
//			dh.adjust(findDronePos(), searchPoints.get(0));
//
//			// Start objektsøgning
//			objectSearch();
//
//			// Flyv til landingsplads
//			dc.turnDrone(0-dc.getFlightData()[2]); // Drej dronen mod tavlevæggen 
//			dh.flyTo(findDronePos(), landingsPlads); // Flyv fra opdateret position til landingsplads
//
//			// Land
//			dc.turnDrone(-90-dc.getFlightData()[2]); // Drej dronen mod fjerneste væg
//			dh.adjust(findDronePos(), landingsPlads);
//			destroy();
//		}
//	}

	private void createPoints(){
		int yMax = 900; // Max Y værdi i koordinatsættet som dronen skal besøge
		int yMin = 0; // Min Y værdi i koordinatsættet som dronen skal besøge
		int xMin = 478; // Min X værdi i koordinatsættet som dronen skal besøge
		int step = 75; // Bredden af en søgebane
		searchPoints = new ArrayList<Koordinat>();
		for(int i=0; i<8; i++){
			if(i%2==0){
				searchPoints.add(new Koordinat(xMin + i*step, yMin));
				searchPoints.add(new Koordinat(xMin + i*step, yMax));
			}else {
				searchPoints.add(new Koordinat(xMin + i*step, yMax));
				searchPoints.add(new Koordinat(xMin + i*step, yMin));
			}
		}
	}

//	private void objectSearch() throws InterruptedException{	
//		final int ACCEPT_DIST = 50; // Acceptabel fejlmargin i position (cm)
//
//		// Find dronepos
//		Koordinat dronePos = findDronePos();
//
//		// Strafe højre/venstre
//		for(int i=0; i<8; i++){
//			//Skift kamera (nedaf)
//			dc.toggleCamera();
//			Thread.sleep(500);
//
//			// Strafe 90%
//			dh.strafePunkt(searchPoints.get(2*i), searchPoints.get(2*i+1));				
//			// Tjek pos
//
//			//Skift kamera (fremad)
//			dc.toggleCamera();
//			Thread.sleep(500);
//			dronePos = findDronePos();
//			if(dronePos.dist(searchPoints.get(2*i+1)) > ACCEPT_DIST){
//				// Finjuster til næste startbane
//				if(i!=7){					
//					dh.adjust(dronePos, searchPoints.get(2*i+2));
//				}
//			}
//		}
//	}

//	private Koordinat findDronePos(){
//		Vector2 dp;
//		boolean posUpdated = false;
//		while(!posUpdated){
//			if(qrcs.getQrt() != ""){
//				Log.writeLog("**Vægmarkering " + qrcs.getQrt() +" fundet.");
//				Vector2 punkter[] = opgrum.getMultiMarkings(qrcs.getQrt());
//
//				//Metode til at udregne vinkel imellem to punkter og dronen skal tilføjes her
//				Koordinat p1 = new Koordinat((int) punkter[0].x, (int) punkter[0].y);
//				Koordinat p2 = new Koordinat((int) punkter[1].x, (int) punkter[1].y);
//				Koordinat p3 = new Koordinat((int) punkter[2].x, (int) punkter[2].y);
//				double alpha = pn.getAngle(px); // Pixels mellem p1 og p2
//				double beta = pn.getAngle(px); // Pixels mellem p3 og p2
//				dp = pn.udregnDronePunkt(punkter[0], punkter[1], punkter[2], alpha, beta);
//				Log.writeLog("Dronepunkt: ("+ dp.x +  "," + dp.y +") fundet.");
//				posUpdated = true;
//			} else {
//				// TODO
//				
//			}
//		}
//
//		return new Koordinat((int) dp.x, (int) dp.y);
//	}
	
	public Koordinat findDronePos2(){
		// Find højden på firkanten rundt om QR koden
		BufferedImage bufFrame = dc.getbufImg();
		Mat frame = ba.getMatFrame(); // ba.bufferedImageToMat(bufFrame);
		ArrayList<QrFirkant> qrFirkanter = punktNav.findQR(frame);
		
		// TODO Læs QR kode og sammenhold position med qrFirkanter objekter
		String qrText = qrcs.imageUpdated(bufFrame);

		QrFirkant readQr = qrFirkanter.get(0); // TODO
		readQr.setText(qrText);
		Vector2 v = this.opgrum.getMultiMarkings(qrText)[1];
		readQr.setPlacering(new Koordinat((int) v.x, (int) v.y));
		
		// Beregn distancen til QR koden
		double dist = punktNav.calcDist(readQr.getHeight(), 420);
		
		// Find vinklen til QR koden
		// Dronens YAW + vinklen i kameraet til QR-koden
		int yaw = dc.getFlightData()[2];
		int imgAngle = punktNav.getAngle(readQr.deltaX()); // DeltaX fra centrum af billedet til centrum af QR koden/firkanten
		int totalAngle = yaw + imgAngle;
		
		Koordinat qrPlacering = readQr.getPlacering();
		// Beregn dronens koordinat
		Koordinat dp = new Koordinat((int) (dist*Math.sin(totalAngle)), (int) (dist*Math.cos(totalAngle)));
		dp.setX(qrPlacering.getX() - dp.getX()); //Forskyder i forhold til QR-kodens rigtige markering
		dp.setY(qrPlacering.getX() - dp.getX());
		System.err.println("DroneKoordinat: (" + dp.getX() + "," + dp.getY() + ")");
		System.err.println(qrText);
//		this.opgrum.addGenstandTilKoordinat(dp, new Genstand(COLOR.RØD));
		return dp;
	}

	/**
	 * Afsøg rummet indtil der findes en vægmarkering
	 * @throws InterruptedException 
	 */
	private boolean findMark() throws InterruptedException {
		if(OPGAVE_DEBUG){
			System.err.println("Målsøgning startes.");
		}
		Log.writeLog("** Målsøgning startes.");
		int yaw = 0;
		int degrees = 15;
		int turns = 0;
		long targetStartTime = System.currentTimeMillis();
		while((System.currentTimeMillis() - targetStartTime) < searchTime){ // Der søges i max 30 sek

			if(Thread.interrupted()){
				destroy();
				return false;
			}
			dc.setLedAnim(LEDAnimation.BLINK_ORANGE, 3, 5); // Blink dronens lys orange mens der søges
			yaw = dc.getFlightData()[2];
			Log.writeLog("Yaw er: " + yaw);
			while(Math.abs(yaw - dc.getFlightData()[2]) < degrees){ // drej x grader, søg efter targets
				if(Thread.interrupted()){
					destroy();
					return false;
				}
				if(OPGAVE_DEBUG){
					System.err.println("Intet mål fundet. Drejer dronen.");
				}
				//				getPossibleManeuvers();
				dc.turnLeft();	
				Log.writeLog("DREJER VENSTRE");
			}
			turns++;
			if(turns > 250/degrees && (Math.abs(yaw - dc.getFlightData()[2]) < 30)){ // Hvis der er drejet tæt på en fuld omgang, så flyves til nyt sted og søges på ny
				if(OPGAVE_DEBUG){
					System.err.println("* Intet mål fundet. Dronen skal flyttes.");
				}
				Log.writeLog("Intet mål fundet. Dronen skal flyttes.");
				//Her skal metode implementeres til at flytte dronen til et nyt koordinat i forhold til sidste fundne vægmarkering
				//Det antages at dronen ved hvor den er.
				if (dronePunkt != null) {
					newLocation(dronePunkt);					
				}

			}
			if(qrcs.getQrt() != ""){
				Log.writeLog("**Vægmarkering " + qrcs.getQrt() +" fundet.");

				Vector2 punkter[] = opgrum.getMultiMarkings(qrcs.getQrt());

				//Metode til at udregne vinkel imellem to punkter og dronen skal tilføjes her

//				dronePunkt = pn.udregnDronePunkt(punkter[0], punkter[1], punkter[2], alpha, beta);
				Log.writeLog("Dronepunkt "+ dronePunkt.x +  " , " + dronePunkt.y +"fundet.");

//				objektSøgning();

				return true;
			}
			if(OPGAVE_DEBUG){
				System.err.println("Intet mål fundet indenfor tidsinterval. Dronen lander.");			
			}
			Log.writeLog("** Målsøgning afsluttes. Mål IKKE fundet.");
			return false;
		}
		return false;
	}


	/**
	 * Hvis opgaven afbrydes af brugeren kaldes denne metode. Dronen lander øjeblikkeligt.
	 */
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

	private Vector2 markFound(String getqrt){
		//Her skal der hentes koordinater via en getMark metode

		if(getqrt != "") {
			Vector2 punkter[] = opgrum.getMultiMarkings(getqrt);

			//Metode til at udregne vinkel imellem to punkter og dronen skal tilføjes her

//			Vector2 dronePunkt = pn.udregnDronePunkt(punkter[0], punkter[1], punkter[2], alpha, beta);
			return null;
		} else {
			return null;
		}
	}

	private boolean newLocation(Vector2 dronePunkt) {



		return true;
	}



	@Override
	public void run() {
//		try {
		while(true){
			this.findDronePos2();			
		}
//		} catch (InterruptedException e) {
//			this.destroy();
//			return;
//		}		
	}
}
