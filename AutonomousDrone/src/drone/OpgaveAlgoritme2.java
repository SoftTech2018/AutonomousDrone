package drone;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import billedanalyse.ColorTracker;
import billedanalyse.IBilledAnalyse;
import billedanalyse.QRCodeScanner;
import billedanalyse.RetningsVektor;
import billedanalyse.Squares;
import billedanalyse.Squares.FARVE;
import de.yadrone.base.command.LEDAnimation;
import diverse.koordinat.Genstand;
import diverse.koordinat.Genstand.GENSTAND_FARVE;
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
	private RetningsVektor rv;
	protected boolean doStop = false;
	private boolean flying = false;
	private Vector2 dronePunkt = null;
	private ArrayList<Koordinat> searchPoints, objectCoords;
	private ArrayList<Squares> squarePoints;
	private Koordinat landingsPlads, papKasse, baneStart, baneSlut;
	long stopTid, startTid;
	private double mspercm = 10.8; //Forholdet mellem milisekunder og afstand tilbagelagt


	public OpgaveAlgoritme2(IDroneControl dc, IBilledAnalyse ba){
		Log.writeLog("Opgavealgoritme oprettet");
		this.dc = dc;
		this.ba = ba;
		this.punktNav = new PunktNavigering();
		this.createPoints();
		this.dh = new DroneHelper(dc, papKasse);
	}

	/**
	 * WARNING - USE AT OWN RISK
	 * This method will attempt to start SKYNET
	 * WARNING - USE AT OWN RISK
	 * @throws InterruptedException 
	 */
	public void startOpgaveAlgoritme() throws InterruptedException {
		Log.writeLog("*** OpgaveAlgoritmen startes. ***");
		dc.setTimeMode(true);
		dc.setFps(15);
		while (!doStop) {
			if(Thread.interrupted()){
				destroy();
				return;
			}
			boolean img = false;
			while(!flying){
				if(Thread.interrupted()){
					destroy();
					return;
				}
				// Hvis dronen ikke er klar og videostream ikke er tilgængeligt, venter vi 500 ms mere
				if(!dc.isReady() || (img = ba.getImages() == null)){
					if(ba.getImages()==null){
						img = false;						
					}
					if(OPGAVE_DEBUG){
						System.out.println("Drone klar: " + dc.isReady()+ ", Billeder modtages: " + img);					
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						destroy();
						return;
					}
					continue; // start forfra i while-løkke
				} else {// Dronen er klar til at letter
					System.err.println("*** WARNING - SKYNET IS ONLINE");
					Log.writeLog("*Dronen letter autonomt.");
					flying = true;
					dc.takeoff();

					if(OPGAVE_DEBUG){
						System.err.println("*** Dronen letter og starter opgaveløsning ***");
					}
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						destroy();
						return;
					}
				}
			}

			dc.up(); // Juster højde på dronen vha. QR-firkanter
			
			// Find position og gem position som landingsplads
			landingsPlads = findDronePos();
			
			if(landingsPlads!=null){
				int yaw = dc.getFlightData()[2];
				Log.writeLog("StartPlacering fundet: \t(" + landingsPlads.getX() + "," + landingsPlads.getY()+ ")");
				Log.writeLog("YAW: \t" + yaw);
				
//				baneStart = landingsPlads; // Noter hvor i rummet vi starter med at afsøge
//				dc.toggleCamera();
//				Thread.sleep(3000);
//				Log.writeLog("Kamera skiftet. Påbegynder flyvning.");
//				dh.flyTo(landingsPlads, new Koordinat(500,500)); // Flyv til midten af rummet (ca)
//				
//				if(yaw > 0 && yaw < 45){
//					// baglæns
//					dc.backward();
//				} else if (yaw > 45 && yaw < 90){
//					// baglæns
//					dc.backward();
//				} else if (yaw > 90 && yaw < 135){
//					// strafe højre
//					dc.right();
//				} else if (yaw > 135 && yaw < 180){
//					// strafe højre
//					dc.right();
//				} else if (yaw > -45 && yaw < 0){
//					// strafe venstra
//					dc.left();
//				} else if (yaw > -90 && yaw < -45){
//					// strafe venstra
//					dc.left();
//				} else if (yaw > -135 && yaw < -90){
//					// lige ud
//					dc.forward();
//				} else if (yaw > -180 && yaw < -135){
//					// lige ud
//					dc.forward();
//				}
//				dc.toggleCamera();
//				Thread.sleep(3000);
//				landingsPlads = this.findDronePos();
//				if(landingsPlads!=null){
//					Log.writeLog("SlutPlacering fundet: (" + landingsPlads.getX() + "," + landingsPlads.getY() + ")");	
//				}
			}
//			dc.turnDroneTo(0); // Drej dronen så den peger mod vinduet
//			dc.land(); // DEBUG
//			if(true)
//				return;

			
			// TODO - Find papkasse-position!
//			Koordinat papkasse = new Koordinat(x, y);
//			dh.setPapKasse(papkasse);

			// Flyv til start
			Log.writeLog("Flyver til første søgepunkt");
			dh.flyTo(landingsPlads, this.searchPoints.get(0));


//			dh.adjust(findDronePos(), searchPoints.get(0));

			// Start objektsøgning
			objectSearch();
//			squarePoints = ba.getColorSquares();
//			getSquaresPositioner(squarePoints);

			// Flyv til landingsplads
//			dc.turnDrone(0-dc.getFlightData()[2]); // Drej dronen mod tavlevæggen 
			Log.writeLog("Flyver til landingsplads for at afslutte programmet");
			dh.flyTo(findDronePos(), landingsPlads); // Flyv fra opdateret position til landingsplads

			// Land
//			dc.turnDrone(-90-dc.getFlightData()[2]); // Drej dronen mod fjerneste væg
//			dh.adjust(findDronePos(), landingsPlads);
			destroy();
		}
	}
	
	/** Modtager et array af squares der gennemløbes og sendes videre til behandling i OpgaveRum,
	 *  som returnerer et koordinat for hver square der gennemløbes 
	 *  i square-arrayet. Herefter gennemløbes koordinat-arrayet og tjek for objekternes position udføres 
	 *  og sendes til OpgaveRum så de kan ses i GUI'en
	 * @param squares
	 */
	private void getSquaresPositioner(ArrayList<Squares> squares) {
		stopTid = System.currentTimeMillis();
		try {
			baneStart = findDronePos();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Koordinat dronePos = baneStart;
		
//		double dist = baneStart.dist(baneSlut); // Hvor langt har dronen bevæget sig?
		
		// Opdater dronepositionen med tiden og retningen siden dronen sidst opdaterede sin position
		for(Squares item: squares) {
//			long squaresdif = startTid - item.getTid();
//			int afstand = (int) ((stopTid - startTid)/(squaresdif*dist)); // (DeltaTid / tid) * distance
			
			int[] data = dc.getFlightData();
			Vector2 vector = rv.getVector(ba.getVektorArray(), data[2]);
//			int afstand2 = dronePos.dist(rv.vectorFromStart(dronePos, vector));
			
			if (baneStart.getY() < 500) {
//				dronePos.setY(baneStart.getY() + afstand2);
				dronePos = rv.vectorFromStart(baneStart, dronePos.getVector().add(vector));
			} else if (baneStart.getY() > 500) {
//				dronePos.setY(baneStart.getY() - afstand2);
				dronePos = rv.vectorFromStart(baneStart, dronePos.getVector().add(vector));
			}

			rv.vectorFromStart(baneStart, dronePos.getVector().add(vector));
			
			Koordinat objectcoord = opgrum.rotateCoordinate(item, dronePos);
			objectCoords.add(objectcoord);
		}	
		
		// Tilføj de fundne objekter i rummets koordinater til opgaverummet
		for (int i = 0; i < objectCoords.size(); i++) {
			Koordinat item = objectCoords.get(i);
			for(Koordinat k : objectCoords){
				//Tjek for om objektet ikke ligger for tæt på objektet før
				if(!k.equals(item) && item.dist(k) > 10){
					Genstand genstand;
					if(squares.get(i).getFarve() == FARVE.RØD){
						genstand = new Genstand(GENSTAND_FARVE.RØD);
					} else {
						genstand = new Genstand(GENSTAND_FARVE.GRØN);
					}
					opgrum.addGenstandTilKoordinat(item, genstand);						
				}
			}
		} 
	}

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

	private void objectSearch() throws InterruptedException{	
		final int ACCEPT_DIST = 100; // Acceptabel fejlmargin i position (cm)

		// Find dronepos
		Koordinat dronePos = findDronePos();
		baneStart = dronePos;
		
		// Roter drone (mod vinduet)
		Log.writeLog("Drejer dronen til YAW = 0");
		dc.turnDroneTo(0);
		
		// Strafe højre/venstre
		for(int i=0; i<1; i++){ // TODO antal baner
			//Skift kamera (nedaf)
			dc.toggleCamera();
			Thread.sleep(3000);

			startTid = System.currentTimeMillis();
			ba.setObjTrack(true); // Track objekter
			Log.writeLog("ObjektTracking startes: " + startTid);
			// Strafe 90%
			dh.strafePunkt(searchPoints.get(2*i), searchPoints.get(2*i+1));				
			ba.setObjTrack(false);
			stopTid = System.currentTimeMillis();
			Log.writeLog("ObjektTracking stoppes: " + stopTid);

			//Skift kamera (fremad)
			dc.toggleCamera();
			Thread.sleep(3000);
			// Tjek pos
			dronePos = findDronePos();
			baneSlut = dronePos;
			if(dronePos.dist(searchPoints.get(2*i+1)) > ACCEPT_DIST){
				// Finjuster til næste startbane
				if(i!=7){					
					dh.adjust(dronePos, searchPoints.get(2*i+2));
				}
			}
		}
	}

	private Koordinat findDronePos() throws InterruptedException{
		Thread.sleep(2000);
		boolean posUpdated = false;
		long start = System.currentTimeMillis();
		Koordinat drone;
		if((drone = ba.getDroneKoordinat()) != null){
			System.err.println("Drone position fundet!");
			return drone;
		} else { 
			int startYaw = dc.getFlightData()[2];
			int turns = 0;
			while(!posUpdated && turns < 4 ){
				Log.writeLog("Drejer dronen: " + (System.currentTimeMillis() - start)/1000);
				dc.turnLeft();
				turns++;
				dc.hover();
				Thread.sleep(5000); // Vent på dronen udfører kommandoen og vi får et rent billede
				if((drone = ba.getDroneKoordinat()) != null){
					posUpdated = true;
					Log.writeLog("Drone position fundet: " + drone);
					return drone;
//					dc.turnDroneTo(startYaw); // Drej dronen tilbage til startpositionen
				}
			}
		}
		return drone;
	}

	private Koordinat findDronePos2(){
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

//	/**
//	 * Afsøg rummet indtil der findes en vægmarkering
//	 * @throws InterruptedException 
//	 */
//	private boolean findMark() throws InterruptedException {
//		if(OPGAVE_DEBUG){
//			System.err.println("Målsøgning startes.");
//		}
//		Log.writeLog("** Målsøgning startes.");
//		int yaw = 0;
//		int degrees = 15;
//		int turns = 0;
//		long targetStartTime = System.currentTimeMillis();
//		while((System.currentTimeMillis() - targetStartTime) < searchTime){ // Der søges i max 30 sek
//
//			if(Thread.interrupted()){
//				destroy();
//				return false;
//			}
//			dc.setLedAnim(LEDAnimation.BLINK_ORANGE, 3, 5); // Blink dronens lys orange mens der søges
//			yaw = dc.getFlightData()[2];
//			Log.writeLog("Yaw er: " + yaw);
//			while(Math.abs(yaw - dc.getFlightData()[2]) < degrees){ // drej x grader, søg efter targets
//				if(Thread.interrupted()){
//					destroy();
//					return false;
//				}
//				if(OPGAVE_DEBUG){
//					System.err.println("Intet mål fundet. Drejer dronen.");
//				}
//				//				getPossibleManeuvers();
//				dc.turnLeft();	
//				Log.writeLog("DREJER VENSTRE");
//			}
//			turns++;
//			if(turns > 250/degrees && (Math.abs(yaw - dc.getFlightData()[2]) < 30)){ // Hvis der er drejet tæt på en fuld omgang, så flyves til nyt sted og søges på ny
//				if(OPGAVE_DEBUG){
//					System.err.println("* Intet mål fundet. Dronen skal flyttes.");
//				}
//				Log.writeLog("Intet mål fundet. Dronen skal flyttes.");
//				//Her skal metode implementeres til at flytte dronen til et nyt koordinat i forhold til sidste fundne vægmarkering
//				//Det antages at dronen ved hvor den er.
//				if (dronePunkt != null) {
//					newLocation(dronePunkt);					
//				}
//
//			}
//			if(qrcs.getQrt() != ""){
//				Log.writeLog("**Vægmarkering " + qrcs.getQrt() +" fundet.");
//
//				Vector2 punkter[] = opgrum.getMultiMarkings(qrcs.getQrt());
//
//				//Metode til at udregne vinkel imellem to punkter og dronen skal tilføjes her
//
//				//				dronePunkt = pn.udregnDronePunkt(punkter[0], punkter[1], punkter[2], alpha, beta);
//				Log.writeLog("Dronepunkt "+ dronePunkt.x +  " , " + dronePunkt.y +"fundet.");
//
//				//				objektSøgning();
//
//				return true;
//			}
//			if(OPGAVE_DEBUG){
//				System.err.println("Intet mål fundet indenfor tidsinterval. Dronen lander.");			
//			}
//			Log.writeLog("** Målsøgning afsluttes. Mål IKKE fundet.");
//			return false;
//		}
//		return false;
//	}


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

	@Override
	public void run() {
		//		try {
			try {
				this.startOpgaveAlgoritme();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		
		//		} catch (InterruptedException e) {
		//			this.destroy();
		//			return;
		//		}		
	}
}
