package drone;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import billedanalyse.IBilledAnalyse;
import de.yadrone.base.command.LEDAnimation;

public class OpgaveAlgoritme implements Runnable {

	/*
	 * Markør hvor der kan udskrives debug-beskeder i konsollen.
	 */
	protected final boolean OPGAVE_DEBUG = true;
	private int searchTime = 30000; // Max søgetid i ms når der ikke kan findes et target. Eks: 60000 = 60 sek.

	private IDroneControl dc;
	private IBilledAnalyse ba;
	protected boolean doStop = false;
	private boolean flying = false;
	private Mat[] frames = new Mat[3];

	public OpgaveAlgoritme(IDroneControl dc, IBilledAnalyse ba){
		this.dc = dc;
		this.ba = ba;
	}

	public Mat[] getFrames(){
		return frames;
	}

	/**
	 * WARNING - USE AT OWN RISK
	 * This method will attempt to start SKYNET
	 * WARNING - USE AT OWN RISK
	 */
	public void startOpgaveAlgoritme(){
		dc.setTimeMode(true);
		while (!doStop) {
			if(Thread.interrupted()){
				destroy();
				break;
			}
			boolean img = false;
			while(!flying){
				if(Thread.interrupted()){
					destroy();
					break;
				}
				// Hvis dronen ikke er klar og videostream ikke er tilgængeligt, venter vi 500 ms mere
				if(!dc.isReady() || (img = dc.getbufImg() == null)){
					if(dc.getbufImg()==null){
						img = false;						
					}
					if(OPGAVE_DEBUG){
						System.out.println("Drone klar: " + dc.isReady()+ ", Billeder modtages: " + img);					
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						destroy();
						break;
					}
					continue; // start forfra i while-løkke
				} else {// Dronen er klar til at letter
					System.err.println("*** WARNING - SKYNET IS ONLINE");
					flying = true;
					dc.takeoff();
					if(OPGAVE_DEBUG){
						System.err.println("*** Dronen letter og starter opgaveløsning ***");
					}
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						destroy();
						break;
					}
				}
			}
			// Find de mulige manøvre vi kan foretage os
			boolean retninger[] = getPossibleManeuvers(); // 0=down, 1=up, 2=goLeft, 3=goRight, 4=forward
			if(!retninger[4]){
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
					}
				}
			}
			if(x != -1 && y != -1){ // Der findes et mål, så det finder vi da..
				if(x==0){ // målet ligger til venstre for os
					dc.turnLeft();
				} else if (x==1){ // målet er foran os
					switch(y){
					case 0: 
						if(retninger[1]){
							dc.up();							
						}
						break;
					case 1:
						if(retninger[4]){
							dc.forward();
						}
						break;
					case 2: 
						if(retninger[0]){
							dc.down();
						}
						break;
					}
				} else if(x==2){ // målet er til højre for os
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
	}

	/**
	 * Afsøg rummet indtil der findes et target
	 */
	private boolean findTarget() {
		if(OPGAVE_DEBUG){
			System.err.println("Målsøgning startes.");
		}
		int yaw = 0;
		int degrees = 15;
		int turns = 0;
		long targetStartTime = System.currentTimeMillis();
		while(!targetFound() || (System.currentTimeMillis() - targetStartTime) > searchTime){ // Der søges i max 30 sek
			if(Thread.interrupted()){
				destroy();
				break;
			}
			dc.setLedAnim(LEDAnimation.BLINK_ORANGE, 3, 5); // Blink dronens lys orange mens der søges
			yaw = dc.getFlightData()[2];
			while(Math.abs(yaw - dc.getFlightData()[2]) < degrees){ // drej x grader, søg efter targets
				if(Thread.interrupted()){
					destroy();
					break;
				}
				if(OPGAVE_DEBUG){
					System.err.println("Intet mål fundet. Drejer dronen.");
				}
				getPossibleManeuvers();
				dc.turnLeft();				
			}
			turns++;
			if(turns > 250/degrees && (Math.abs(yaw - dc.getFlightData()[2]) < 30)){ // Hvis der er drejet tæt på en fuld omgang, så flyves til nyt sted og søges på ny
				if(OPGAVE_DEBUG){
					System.err.println("Intet mål fundet. Flytter dronen.");
				}
				boolean retninger[] = getPossibleManeuvers(); // down, up, goLeft, goRight, forward
				long startTime = System.currentTimeMillis();
				while(!targetFound() || (System.currentTimeMillis() - startTime) > 5000){ // Gør noget i 5000 ms eller indtil et mål findes
					if(Thread.interrupted()){
						destroy();
						break;
					}
					if(retninger[4]){
						dc.forward();
					} else if(retninger[2]){
						dc.turnLeft();
					} else if(retninger[3]){
						dc.turnRight();
					} else if(retninger[1]){
						dc.up();
					} else if(retninger[0]){
						dc.down();
					}
				}
				turns = 0;
			}
		}
		if(targetFound()){
			return true;
		}
		if(OPGAVE_DEBUG){
			System.err.println("Intet mål fundet indenfor tidsinterval. Dronen lander.");			
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
		double threshold = 15; // TODO Bestemmer hvor stor magnituden i en firkant må være

		Mat frame = ba.bufferedImageToMat(dc.getbufImg()); // Hent billede fra drone og konverter til Mat
		frames = ba.optFlow(frame, true, false); // Kør Optical Flow
		double magnitudes[][] = ba.calcOptMagnitude(size); // Beregn Magnituden (baseret på Optical Flow vektorer)

		Point center = new Point(frame.size().width/2, frame.size().height/2);
		double x = center.x;
		double y = center.y;
		double hStep = frame.size().height/size;
		double vStep = frame.size().width/size;

		// Mulige manøvre
		boolean retninger[] = {false,false,false,false,false};// down, up, goLeft, goRight, forward

		// Tegn røde og grønne firkanter der symboliserer mulige manøvre
		Scalar red = new Scalar(0,0,255); // Rød farve til stregen
		Scalar green = new Scalar(0,255,0); // Grøn farve 
		int thickness = 2; // Tykkelse på stregen
		for(int i=0; i<size; i++){
			for(int o=0; o<size; o++){
				// Tegn rød firkant hvis objektet er for tæt på, ellers tegn grøn firkant
				if(magnitudes[i][o] >= threshold){
					Imgproc.rectangle(frames[0], new Point(vStep*i, hStep*o), new Point(vStep*(i+1)-2, hStep*(o+1)-2), red, thickness);
				} else {
					Imgproc.rectangle(frames[0], new Point(vStep*i, hStep*o), new Point(vStep*(i+1)-2, hStep*(o+1)-2), green, thickness);
				}
			}
		}

		if(magnitudes[0][0] < threshold){
			x = x - vStep;
			y = y - hStep;
		}
		if(magnitudes[0][1] < threshold){
			y = y - hStep;
			retninger[1] = true;
		}
		if(magnitudes[0][2] < threshold){
			x = x + vStep;
			y = y - hStep;
		}
		if(magnitudes[1][0] < threshold){
			x = x - vStep;
			retninger[2] = true;
		}
		if(magnitudes[1][1] < threshold){
			retninger[4] = true;
		}
		if(magnitudes[1][2] < threshold){
			x = x + vStep;
			retninger[3] = true;
		}
		if(magnitudes[2][0] < threshold){
			x = x - vStep;
			y = y + hStep;
		}
		if(magnitudes[2][1] < threshold){
			y = y + hStep;;
			retninger[0] = true;
		}
		if(magnitudes[2][2] < threshold){
			x = x + vStep;
			y = y + hStep;
		}		

		//		Vektor dir = new Vektor(center, new Point(x,y)); // Kan benyttes hvis man ønsker en vektorrepræsentation
		return retninger;				
	}

	@Override
	public void run() {
		this.startOpgaveAlgoritme();		
	}
}
