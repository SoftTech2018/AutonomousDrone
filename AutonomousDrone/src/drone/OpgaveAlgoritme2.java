package drone;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.opencv.core.Mat;

import billedanalyse.IBilledAnalyse;
import billedanalyse.QRCodeScanner;
import billedanalyse.RetningsVektor;
import billedanalyse.Squares;
import billedanalyse.Squares.FARVE;
import diverse.Log;
import diverse.PunktNavigering;
import diverse.QrFirkant;
import diverse.circleCalc.Vector2;
import diverse.koordinat.Genstand;
import diverse.koordinat.Genstand.GENSTAND_FARVE;
import diverse.koordinat.Koordinat;
import diverse.koordinat.OpgaveRum;
import drone.DroneHelper.DIRECTION;

public class OpgaveAlgoritme2 implements Runnable {

	/*
	 * Markør hvor der kan udskrives debug-beskeder i konsollen.
	 */
	protected final boolean OPGAVE_DEBUG = true;

	private IDroneControl dc;
	private IBilledAnalyse ba;
	private OpgaveRum opgrum;
	private DroneHelper dh;
	private PunktNavigering punktNav;
	private QRCodeScanner qrcs;
	private PunktNavigering pn;
	private RetningsVektor rv;
	private boolean doStop = false;
	private boolean flying = false;
	private ArrayList<Koordinat> searchPoints, objectCoords;
	private ArrayList<Squares> squarePoints;
	private Koordinat landingsPlads, baneStart, baneSlut;
	private long stopTid, startTid;
	private int yaw;
	private int xOffset = 0;
	private int yOffset = 0;


	public OpgaveAlgoritme2(IDroneControl dc, IBilledAnalyse ba, OpgaveRum opgrum){
		Log.writeLog("Opgavealgoritme oprettet");
		this.dc = dc;
		this.ba = ba;
		this.punktNav = new PunktNavigering();
		this.createPoints();
		this.dh = new DroneHelper(dc);
		this.opgrum = opgrum;
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


			//			this.findPapkasse();
			this.opgave2();
			if(true)
				return;

			//			dc.up(); //  Juster højde på dronen vha. QR-firkanter

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
			//			this.opgrum.setObstacleCenter(papkasse); // Tilføj papkassen på GUI

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
		Koordinat dronePos = baneStart;

		//		double dist = baneStart.dist(baneSlut); // Hvor langt har dronen bevæget sig?

		// Opdater dronepositionen med tiden og retningen siden dronen sidst opdaterede sin position
		for(Squares item: squares) {
			//			long squaresdif = startTid - item.getTid();
			//			int afstand = (int) ((stopTid - startTid)/(squaresdif*dist)); // (DeltaTid / tid) * distance

			//			int[] data = dc.getFlightData();
			Vector2 vector = rv.getVector(ba.getVektorArray(), yaw);
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

		try{
			Thread.sleep(3000);

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
				yaw = dc.getFlightData()[2];
				// Strafe 90%
				dh.strafePunkt(searchPoints.get(2*i), searchPoints.get(2*i+1));				
				ba.setObjTrack(false);
				stopTid = System.currentTimeMillis();
				Log.writeLog("ObjektTracking stoppes: " + stopTid);
				Log.writeLog("Objekter fundet: " + ba.getColorSquares().size());

				//Skift kamera (fremad)
				dc.toggleCamera();
				Thread.sleep(3000);
				// Tjek pos
				dronePos = findDronePos();
				baneSlut = dronePos;
				Log.writeLog("Tegner objekter på GUI.");
				getSquaresPositioner(ba.getColorSquares());
				if(dronePos.dist(searchPoints.get(2*i+1)) > ACCEPT_DIST){
					// Finjuster til næste startbane
					if(i!=7){					
						dh.adjust(dronePos, searchPoints.get(2*i+2));
					}
				}
			}
		} catch (NullPointerException e){
			Log.writeLog("Drone position ej fundet. Lander.");
			destroy();
			return;
		}
	}

	private Koordinat findDronePos() throws InterruptedException{
		Thread.sleep(2000);
		boolean posUpdated = false;
		long start = System.currentTimeMillis();
		Koordinat drone;

		//		setDroneHeight(ba.getFirkant());



		if((drone = ba.getDroneKoordinat()) != null){
			Log.writeLog("Drone position fundet: " + drone.toString());
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
					Log.writeLog("Drone position fundet: " + drone.toString());
					return drone;
					//					dc.turnDroneTo(startYaw); // Drej dronen tilbage til startpositionen
				}
			}
		}
		return drone;
	}

	/**
	 * Udfører en opgaveløsning ved at dreje dronen så lidt som muligt.
	 * Der benyttes primært strafe, fremad og bagud og bevæges i et mønster defineret af dronens position
	 * @throws InterruptedException
	 */
	private void opgave2() throws InterruptedException{
		dc.up2(); // Flyv lidt op så vi kan se QR-koder. Skal måske fjernes!
		dc.up2();
		dc.up2();
		Thread.sleep(1000);

		// Start QR scanning
		ba.setDroneLocator(true);
		Thread.sleep(3000); // Vent på stabilt billede

		// Juster højden på dronen
		QrFirkant firkant = ba.getFirkant();
		long startTime = System.currentTimeMillis();
		int backwardMoves = 0;
		while(firkant==null){
			if(System.currentTimeMillis() - startTime > 5000){						
				// TODO - Juster dronens position ud fra sidst kendte position. Bevæg mod midten af rummet
			}
			Log.writeLog("Ingen firkant fundet - flyver lidt baglæns");
			dc.backward();
			backwardMoves++;
			Thread.sleep(4000); // Stabiliser billedet
			firkant = ba.getFirkant();
		}
		if(firkant!=null){
			Log.writeLog("Firkant centrum: " +firkant.getCentrum());
		}
		while(firkant.getCentrum().getY() < 200 || firkant.getCentrum().getY() > 520){	
			Log.writeLog("Justerer drone højde...");
			setDroneHeight(firkant);
			Thread.sleep(1500);
			firkant = ba.getFirkant();
			if(firkant==null){
				Log.writeLog("Drone flyver lidt baglæns og stabiliserer");
				dc.backward();
				Thread.sleep(3000);
			}
		}

		// Find droneposition (se fremad)
		Log.writeLog("Finder drone position...");
		Koordinat drone = findDronePos3();
		if(drone==null){
			Log.writeLog("*** Drone position IKKE fundet. Programmet afsluttes.");
			destroy();
			return;
		} else {
			Log.writeLog("Drone position fundet: " + drone.toString());
		}
		// Noter hvor vi er startet
		landingsPlads = drone;
		// Juster position i forhold til hvor meget dronen har bevæget sig for at finde sin første position
		landingsPlads.setX(landingsPlads.getX() - backwardMoves * 75 + xOffset); 
		landingsPlads.setY(landingsPlads.getY() + yOffset); 

		// Find papkasse (drej)
		//		ba.setPapkasseLocator(true);
		//		Koordinat papKasse = findPapkassePos();
		//		if(papKasse != null){
		//			Log.writeLog("Papkasse position fundet: " + papKasse.toString());
		//			this.opgrum.setObstacleCenter(papkasse); // Tilføj papkassen på GUI
		//		dh.setPapKasse(papKasse);
		//		} else {
		//			Log.writeLog("Papkasse er ikke fundet!");
		//		}

		// Drej mod vinduet
		//		Log.writeLog("Drejer drone mod vinduet...");
		//		dc.turnDroneTo(0);

		// Stop QR scanning
		ba.setDroneLocator(false);

		Log.writeLog("*** Starter objektsøgning ***");
		int moves = 0;
		// ** START WHILE LØKKE **
		DIRECTION lastDir = null;
//		while(dh.getCenterAreal() > 10000){ // Svarer til et område på 1 x 1 meter
		while(moves < 4){ // TODO - DEBUG
			// Skift kamera
			ba.setDroneLocator(false);
			dc.toggleCamera();
			Log.writeLog("Skifter til nedafrettede kamera");
			Thread.sleep(2000);
			// Start opticalFlow
			//			ba.setOpticalFlow(true);
			// Start objectTracking
			ba.setObjTrack(true);

			Thread.sleep(2000); // kig efter objekter i 2 sekunder

			// Stop opticalFlow
			//			ba.setOpticalFlow(false);
			// Stop objectTracking
			ba.setObjTrack(false);	
			Thread.sleep(200); // Sikrer at billedanalysen ikke er ved at finde yderligere objekter

			// Find objekter hvor vi er nu
			try{
				this.logSquares(drone);
			} catch (Exception e){
				Log.writeLog(e.getMessage());
			}

			// Skift kamera og stat QR scanning
			dc.toggleCamera();
			Log.writeLog("Skifter til fremadrettet kamera");
			Thread.sleep(1000); // vent på kameraet har skiftet billede	

			// Flyv baseret på position og retning
			lastDir = dh.moveDrone(drone, lastDir);

			ba.setDroneLocator(true);
			// Estimer dronens nye position
			int xKoor = drone.getX();
			int yKoor = drone.getY();
			switch(lastDir){
			case UP:
				yKoor += 75;
				break;
			case DOWN:
				yKoor -= 75;
				break;
			case LEFT:
				xKoor -= 75;
				break;
			case RIGHT:
				xKoor += 75;
				break;
			case STOPPED:
				break;
			}
			Koordinat newDronePos = new Koordinat(xKoor, yKoor); // Estimeret position

			// Find papKasse - vi kan kun se den når vi bevæger os op af y-aksen og kigger mod vinduet
			if(lastDir.equals(DIRECTION.UP) && dh.getPapkasse()==null){
				ba.setPapKasseLocator(true);
			} 
			
			// Find droneposition (se fremad)
			drone = this.findDronePos3();
			// Stop papkasse-søgning
			ba.setPapKasseLocator(false);
			if(drone == null){
				drone = newDronePos;
				Log.writeLog("*** Drone position IKKE fundet. Estimerer position: " + drone);
			} else {
				if(Math.abs(dc.getFlightData()[2]) > 10){
					dc.turnDroneTo(0);
				}
			}

			// Papkassen er fundet
			int dist;
			if(dh.getPapkasse()== null && (dist = ba.getPapKasse()) != -1){
				Koordinat papkasse = this.findPapkasse(drone, dist);
				// Logisk tjek for om papkassen er nogenlunde i midten af rummet
				if (papkasse.getX() < 400 || papkasse.getX() > 800 || papkasse.getY() < 200 || papkasse.getY() > 700){
					dh.setPapKasse(papkasse);
				}
			}
			
			// ** SLUT WHILE LØKKE **
			moves++;
		}

		// TODO - Find tilbage til landingsplads vha. frem/tilbage/strafe
		dh.flyTo(drone, landingsPlads); // YOLO - Drej dronen og charge!

		Log.writeLog("Opgaveløsning er afsluttet. Dronen lander.");
		destroy();
	}

	/**
	 * Let, Drej, Drej tilbage, Land, vent, Let, Hover, Land
	 * @throws InterruptedException
	 */
	private Koordinat findPapkasse(Koordinat dronePos, int dist) throws InterruptedException{		
		Koordinat papKasse = new Koordinat(0, 0);
		//Udregn papkassens position i forhold til dronens position og afstand
		int[] data = dc.getFlightData(); //vinkel mellem QR kode og papkasse
		// enhedsvektor = ex = cos(yaw), ey = sin(yaw)
		papKasse.setX((int) Math.cos(data[2]) * dist); // vektor x = ex * dist
		papKasse.setY((int) Math.sin(data[2]) * dist); // vektor y = ey * dist
		papKasse.setX(papKasse.getX() + dronePos.getX());
		papKasse.setY(papKasse.getY() + dronePos.getY());
		Log.writeLog("Papkassens koordinater er: " + papKasse);
		return papKasse;
	}

	/**
	 * Finder drone position kun ved at kigge fremad. Dronen roteres IKKE.
	 * @return
	 * @throws InterruptedException
	 */
	private Koordinat findDronePos3() throws InterruptedException{
		Thread.sleep(1000); // Vent på billedet stabiliserer sig
		long start = System.currentTimeMillis();
		Koordinat drone;

		// Kør løkken indtil der er fundet en position. Max 10000ms (10 sek)
		while((drone = ba.getDroneKoordinat()) == null){
			QrFirkant firkanten = ba.getFirkant();
			if(firkanten==null){
				Log.writeLog("Ingen firkant fundet. Flyver baglæns.");
				dc.backward();
				xOffset = xOffset - 75;
				Thread.sleep(3000);
				continue;
			}

			// Find firkant og bevæg dronen mod/væk fra den afhængig af afstand
			if((System.currentTimeMillis() - start) < 20000){ 
				if(firkanten.getHeight() > 500){ // TODO - korrekte pixelværdier for grænsetilfælde
					// bagud
					Log.writeLog("Justerer drone position i forhold til QR-kode. BAGLÆNS");
					dc.backward();
					xOffset = xOffset - 75;
				} else if (firkanten.getHeight() < 50){ // TODO - korrekte pixelværdier for grænsetilfælde
					// fremad
					Log.writeLog("Justerer drone position i forhold til QR-kode. FORLÆNS");
					dc.forward();
					xOffset = xOffset + 75;
				}
				if (firkanten.getCentrum().getX() > 950){ // TODO - korrekte pixelværdier for grænsetilfælde
					// strafe højre
					Log.writeLog("Justerer drone position i forhold til QR-kode. HØJRE");
					dc.right();
					yOffset = yOffset - 75;
				} else if (firkanten.getCentrum().getX() < 250){ // TODO - korrekte pixelværdier for grænsetilfælde
					// strafe venstre
					Log.writeLog("Justerer drone position i forhold til QR-kode. VENSTRE");
					dc.left();
					yOffset = yOffset + 75;
				}
				dc.hover();
				Thread.sleep(1500); // Vent på billedet har opdateret og stabiliseret sig
			}
			Thread.sleep(500);
		}

		if(Math.abs(dc.getFlightData()[2]) > 10){ // Drej dronen mod vinduet. YAW er netop blevet opdateret
			Log.writeLog("Drejer dronen mod YAW = 0");
			dc.turnDroneTo(0);
		}
		return drone;
	}

	private void testFlight() throws InterruptedException{
		dc.hover();
		Thread.sleep(3500);
		dc.strafeLeft(318);
		dc.hover();
		Thread.sleep(3500);
		dc.strafeRight(318);
		dc.hover();
		Thread.sleep(3500);
		dc.backward();
		dc.hover();
		Thread.sleep(3500);
		dc.forward();
		dc.hover();
		Thread.sleep(3500);
		destroy();
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
		int imgAngle = (int) punktNav.getAngle(readQr.getCentrum().getX(), bufFrame.getWidth()/2); // DeltaX fra centrum af billedet til centrum af QR koden/firkanten
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

	private void setDroneHeight(QrFirkant firkant) throws InterruptedException{
		int dif = 0; 
		Koordinat firkantCentrum = firkant.getCentrum();
		int billedCentrum = 360;

		if(firkantCentrum.getY() > billedCentrum){
			dif = firkantCentrum.getY()-billedCentrum;
			if(dif > 180 ){
				//her skal dronen navigere nedad 
				dc.down2();
			}
		} else if(firkantCentrum.getY() < billedCentrum){
			dif = billedCentrum-firkantCentrum.getY();
			if(dif > 180 ){
				//her skal dronen navigere opad. 
				dc.up2();
			}
		}
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

	private void logSquares(Koordinat drone){
		ArrayList<Squares> squares = ba.getColorSquares();
		if(squares!=null && !squares.isEmpty()){
			Log.writeLog("Fundet " + squares.size() + " genstande inden frasortering.");// TODO - DEBUG
			for(Squares sq : squares){
				Log.writeLog("FOR-løkke startet.");// TODO - DEBUG
				Genstand g;
				if(sq.getFarve().equals(FARVE.GRØN)){
					g = new Genstand(GENSTAND_FARVE.GRØN);
				} else {
					g = new Genstand(GENSTAND_FARVE.RØD);
				}	
				Log.writeLog("Genstand objekt lavet.");// TODO - DEBUG
				Koordinat k = opgrum.rotateCoordinate(sq, drone);
				Log.writeLog("RotateCoordinate udført. Drone Koordinat: " + drone);// TODO - DEBUG
				opgrum.addGenstandTilKoordinat(k, g);
				Log.writeLog("Genstand tilføjet til OpgaveRum: " +k + "\tBilledKoordinat: (" + sq.x + "," + sq.y + ")"); // TODO - DEBUG
			}
		} else {
			Log.writeLog("Ingen objekter fundet!");
		}
	}
}
