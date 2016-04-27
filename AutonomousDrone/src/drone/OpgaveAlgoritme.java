package drone;

import java.util.Random;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import billedanalyse.IBilledAnalyse;
import de.yadrone.base.command.LEDAnimation;
import diverse.Log;
import diverse.koordinat.Genstand;
import diverse.koordinat.Koordinat;
import diverse.koordinat.OpgaveRum;

public class OpgaveAlgoritme implements Runnable {

	/*
	 * Markør hvor der kan udskrives debug-beskeder i konsollen.
	 */
	protected final boolean OPGAVE_DEBUG = true;
	private int searchTime = 30000; // Max søgetid i ms når der ikke kan findes et target. Eks: 60000 = 60 sek.

	private IDroneControl dc;
	private IBilledAnalyse ba;
	private OpgaveRum opgrum;
	protected boolean doStop = false;
	private boolean flying = false;

	public OpgaveAlgoritme(IDroneControl dc, IBilledAnalyse ba){
		this.dc = dc;
		this.ba = ba;
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
					//Her skal tilføjes en metode til at logge landingspladsen
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
			// Find de mulige manøvre vi kan foretage os
			boolean retninger[] = getPossibleManeuvers(); // 0=down, 1=up, 2=goLeft, 3=goRight, 4=forward
			if(!retninger[4]){
				Log.writeLog("Dronen kan ikke flyve forlæns. Hover.");
				dc.hover(); // Vi kan ikke flyve forlæns, ergo må vi stoppe dronen
			}

			// Find de objekter vi leder efter
			boolean targets[][] = getTargets(); // 3x3 array
			int x = -1, y = -1;
			// Hvor er mål objektet i dronens synsfelt
			for(int i=0; i<3; i++){
				for(int o=0; o<3; o++){
					if(targets[i][o]){
						x = i;
						y = o;
						Log.writeLog("**Mål fundet i: " + x + "," + y);
					}
				}
			}
			if(x != -1 && y != -1){ // Der findes et mål, så det finder vi da.
				if(x==0){ // målet ligger til venstre for os
					Log.writeLog("DREJER VENSTRE");
					dc.turnLeft();
				} else if (x==1){ // målet er foran os
					switch(y){
					case 0: 
						if(retninger[1]){
							Log.writeLog("FLYVER OP");
							dc.up();							
						}
						break;
					case 1:
						if(retninger[4]){
							Log.writeLog("FLYVER FREMAD");
							dc.forward();
						}
						break;
					case 2: 
						if(retninger[0]){
							Log.writeLog("FLYVER NED");
							dc.down();
						}
						break;
					}
				} else if(x==2){ // målet er til højre for os
					Log.writeLog("DREJER HØJRE");
					dc.turnRight();
				}
			} else {// Intet mål-objekt fundet, vi starter målsøgningen				
				if (findTarget()){
					// TODO Her skal nok ikke laves noget. Whileløkken kan bare fortsætte
				} else {
					// Der kunne ikke findes et mål objekt på 30 sekunder.
					// TODO Land dronen sikkert
					flying = false;
					dc.land();
				}
			}
		}
		dc.setTimeMode(false);
		Log.writeLog("*** OpgaveAlgoritme afsluttes.");
	}

	/**
	 * Afsøg rummet indtil der findes et target
	 * @throws InterruptedException 
	 */
	private boolean findTarget() throws InterruptedException {
		if(OPGAVE_DEBUG){
			System.err.println("Målsøgning startes.");
		}
		Log.writeLog("** Målsøgning startes.");
		int yaw = 0;
		int degrees = 15;
		int turns = 0;
		long targetStartTime = System.currentTimeMillis();
		while((System.currentTimeMillis() - targetStartTime) < searchTime){ // Der søges i max 30 sek
			if(targetFound()){
				break;
			}
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
				getPossibleManeuvers();
				dc.turnLeft();	
				Log.writeLog("DREJER VENSTRE");
			}
			turns++;
			if(turns > 250/degrees && (Math.abs(yaw - dc.getFlightData()[2]) < 30)){ // Hvis der er drejet tæt på en fuld omgang, så flyves til nyt sted og søges på ny
				if(OPGAVE_DEBUG){
					System.err.println("* Intet mål fundet. Dronen skal flyttes.");
				}
				Log.writeLog("Intet mål fundet. Dronen skal flyttes.");

				long startTime = System.currentTimeMillis();
				while((System.currentTimeMillis() - startTime) < 5000){ // Gør noget i 5000 ms eller indtil et mål findes
					if(targetFound()){
						break;
					}
					if(Thread.interrupted()){
						destroy();
						return false;
					}
					boolean retninger[] = getPossibleManeuvers(); // down, up, goLeft, goRight, forward
					String muligeRetninger = "Retninger modtaget: ";
					for(int i=0; i<retninger.length;i++){
						muligeRetninger += retninger[i] + ", ";
					}
					Log.writeLog(muligeRetninger + " : " + retninger.length);
					boolean flyver = false;
					if(retninger[4]){
						Log.writeLog("FLYVER FREMAD");
						flyver = true;
						dc.forward();
					} else if(retninger[2]){
						Log.writeLog("DREJER VENSTRE");
						flyver = true;
						dc.turnLeft();
					} else if(retninger[3]){
						Log.writeLog("DREJER HØJRE");
						flyver = true;
						dc.turnRight();
					} else if(retninger[1]){
						Log.writeLog("FLYVER OP");
						flyver = true;
						dc.up();
					} else if(retninger[0]){
						Log.writeLog("FLYVER NED");
						flyver = true;
						dc.down();
					}
					if(flyver == false){
						Log.writeLog("DREJER HØJRE");
						flyver = true;
						dc.turnRight();
					}
				}
				turns = 0;
			}
		}
		if(targetFound()){
			Log.writeLog("**Mål fundet.");
			Koordinat koord = new Koordinat(0, 0 /*Der skal udregnes en metode til at finde x og y*/);
			Genstand genstand = new Genstand(null /*Farve skal hentes fra en Optical Flow metode*/);
			opgrum.addGenstandTilKoordinat(koord, genstand);
			return true;
		}
		if(OPGAVE_DEBUG){
			System.err.println("Intet mål fundet indenfor tidsinterval. Dronen lander.");			
		}
		Log.writeLog("** Målsøgning afsluttes. Mål IKKE fundet.");
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

	private boolean targetFound(){
		//		boolean targets[][] = getTargets(); // 3x3 array
		//		// Hvor er mål objektet i dronens synsfelt
		//		for(int i=0; i<3; i++){
		//			for(int o=0; o<3; o++){
		//				if(targets[i][o]){
		//					return true;
		//				}
		//			}
		//		}
		return false;
	}

	/**
	 * Find det target hvor vi skal bevæge os til
	 * @return
	 */
	private boolean[][] getTargets() {
		boolean out[][] = new boolean[3][3];
		for(int i=0; i<3; i++){
			for(int o=0; o<3; o++){
				out[i][o] = false;
			}
		}
		// TODO Auto-generated method stub
		return out;
	}

	/**
	 * Find de mulige manøvremuligheder for dronen. False betyder at vi ikke kan flyve i den retning
	 * @return Array med mulige retninger: down, up, goLeft, goRight, forward
	 */
	public boolean[] getPossibleManeuvers(){
		int size = 3;
		double threshold = 25; // TODO Bestemmer hvor stor magnituden i en firkant må være

		Mat frame = new Mat();
		frame = ba.getMatFrame();

		double magnitudes[][] = ba.calcOptMagnitude(size); // Beregn Magnituden (baseret på Optical Flow vektorer)

		Point center = new Point(640/2, 360/2);
		double x = center.x;
		double y = center.y;
		double hStep = 360/size;
		double vStep = 640/size;

		// Mulige manøvre
		boolean retninger[] = {false,false,false,false,false};// down, up, goLeft, goRight, forward

		// Tegn røde og grønne firkanter der symboliserer mulige manøvre
		Scalar red = new Scalar(0,0,255); // Rød farve til stregen
		Scalar green = new Scalar(0,255,0); // Grøn farve 
		int thickness = 2; // Tykkelse på stregen
		double min = Double.MAX_VALUE;
		int mindsteI = -1;
		int mindsteO = -1;
		for(int i=0; i<size; i++){
			for(int o=0; o<size; o++){
				// Find den mindste magnitude
				if(magnitudes[i][o] < min){
					min = magnitudes[i][o];
					mindsteI = i;
					mindsteO = o;
				}
				// Tegn rød firkant hvis objektet er for tæt på, ellers tegn grøn firkant
				if(magnitudes[i][o] >= threshold){
					Imgproc.rectangle(frame, new Point(vStep*i, hStep*o), new Point(vStep*(i+1)-2, hStep*(o+1)-2), red, thickness);
				} else {
					Imgproc.rectangle(frame, new Point(vStep*i, hStep*o), new Point(vStep*(i+1)-2, hStep*(o+1)-2), green, thickness);
				}
			}
		}
		ba.setImage(frame); // Opdater billedet i BA så det nytegnede billede vises på GUI

		// Balance-modellen - vi bevæger os derhen hvor der er mindst magnitude
		Random r = new Random();
		switch(mindsteI){
		case 0: // Venstre kolonne
			switch(mindsteO){
			case 0: // Flyv venstre eller op
				if(r.nextBoolean()){
					retninger[2] = true;
				} else {
					retninger[2] = true;
				}
				break;
			case 1: // Flyv venstre
				retninger[2] = true;
				break;
			case 2: // Flyv venstre eller ned
				if(r.nextBoolean()){
					retninger[2] = true;
				} else {
					retninger[2] = true;
				}
				break;
			}
			break;
		case 1: // Midterste kolonne
			switch(mindsteO){
			case 0: // Flyv op
				retninger[4] = true;
				break;
			case 1: // Fremad
				retninger[4] = true;
				break;
			case 2: // Flyv ned
				retninger[4] = true;
				break;
			}
			break;
		case 2: // Højre kolonne
			switch(mindsteO){
			case 0: // Flyv højre eller op
				if(r.nextBoolean()){
					retninger[3] = true;
				} else {
					retninger[3] = true;
				}
				break;
			case 1: // Flyv højre
				retninger[3] = true;
				break;
			case 2: // Flyv højre eller ned
				if(r.nextBoolean()){
					retninger[3] = true;
				} else {
					retninger[3] = true;
				}
				break;
			}
			break;
		default:
		}

//		// Baseret på magnitude værdi vs. threshhold
//		if(magnitudes[0][0] < threshold){
//			x = x - vStep;
//			y = y - hStep;
//		}
//		if(magnitudes[0][1] < threshold){
//			y = y - hStep;
//			retninger[1] = true;
//		}
//		if(magnitudes[0][2] < threshold){
//			x = x + vStep;
//			y = y - hStep;
//		}
//		if(magnitudes[1][0] < threshold){
//			x = x - vStep;
//			retninger[2] = true;
//		}
//		if(magnitudes[1][1] < threshold){
//			retninger[4] = true;
//		}
//		if(magnitudes[1][2] < threshold){
//			x = x + vStep;
//			retninger[3] = true;
//		}
//		if(magnitudes[2][0] < threshold){
//			x = x - vStep;
//			y = y + hStep;
//		}
//		if(magnitudes[2][1] < threshold){
//			y = y + hStep;;
//			retninger[0] = true;
//		}
//		if(magnitudes[2][2] < threshold){
//			x = x + vStep;
//			y = y + hStep;
//		}	

		//		Vektor dir = new Vektor(center, new Point(x,y)); // Kan benyttes hvis man ønsker en vektorrepræsentation
		return retninger;				
	}

	@Override
	public void run() {
		try {
			this.startOpgaveAlgoritme();
		} catch (InterruptedException e) {
			this.destroy();
			return;
		}		
	}
}
