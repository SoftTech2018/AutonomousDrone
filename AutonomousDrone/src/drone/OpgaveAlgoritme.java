package drone;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import com.google.zxing.Result;

import billedanalyse.BilledAnalyse;
import billedanalyse.Vektor;
import de.yadrone.base.command.LEDAnimation;

public class OpgaveAlgoritme {

	private Result object;
	private IDroneControl dc = new DroneControl();
	private BilledAnalyse ba = new BilledAnalyse();
	protected boolean doStop = false;
	private int searchTime = 60000; // Max søgetid i ms når der ikke kan findes et target. Eks: 60000 = 60 sek.
	
	/*
	 * Markør hvor der kan udskrives debug-beskeder i konsollen.
	 */
	protected final boolean OPGAVE_DEBUG = true;
	
	public OpgaveAlgoritme(){
		while (!doStop) {
			boolean img = false;
			// Hvis dronen ikke er klar og videostream ikke er tilgængeligt, venter vi 500 ms mere
			if(!dc.isReady() || (img = dc.getImage() == null)){
				if(dc.getImage()==null){
					img = false;						
				}
				if(OPGAVE_DEBUG){
					System.out.println("Drone klar: " + dc.isReady()+ ", Billeder modtages: " + img);					
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue; // start forfra i while-løkke
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
					dc.land();
				}
			}
			
			try {
				if (object == null) { //Indsæt objekt listener her
					
				}
			}
			catch(Exception exc)
			{
				exc.printStackTrace();
			}
		}
	

	}
	
	/**
	 * Afsøg rummet indtil der findes et target
	 */
	private boolean findTarget() {
		if(OPGAVE_DEBUG){
			System.out.println("Målsøgning startes.");
		}
		int yaw = dc.getFlightData()[2];
		int degrees = 15;
		int turns = 0;
		long targetStartTime = System.currentTimeMillis();
		while(!targetFound() || (System.currentTimeMillis() - targetStartTime) > searchTime){ // Der søges i max 30 sek
			dc.setLedAnim(LEDAnimation.BLINK_RED, 3, 10); // Blink dronens lys rødt mens der søges
			while(Math.abs(yaw - dc.getFlightData()[2]) < degrees){ // drej x grader, søg efter targets
				dc.turnLeft();				
			}
			turns++;
			if(turns > 250/degrees && (Math.abs(yaw - dc.getFlightData()[2]) < 30)){ // Hvis der er drejet tæt på en fuld omgang, så flyves til nyt sted og søges på ny
				if(OPGAVE_DEBUG){
					System.out.println("Intet mål fundet. Flytter dronen.");
				}
				boolean retninger[] = getPossibleManeuvers(); // down, up, goLeft, goRight, forward
				long startTime = System.currentTimeMillis();
				while(!targetFound() || (System.currentTimeMillis() - startTime) < 5000){ // Gør noget i 5000 ms eller indtil et mål findes
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
	
	private boolean targetFound(){
		boolean targets[][] = getTargets(); // 3x3 array
		// Hvor er mål objektet i dronens synsfelt
		for(int i=0; i<3; i++){
			for(int o=0; o<3; o++){
				if(targets[i][o]){
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Find det target hvor vi skal bevæge os til
	 * @return
	 */
	private boolean[][] getTargets() {
		boolean out[][] = new boolean[3][3];
		// TODO Auto-generated method stub
		return out;
	}

	/**
	 * Find de mulige manøvremuligheder for dronen. False betyder at vi ikke kan flyve i den retning
	 * @return Array med mulige retninger: down, up, goLeft, goRight, forward
	 */
	private boolean[] getPossibleManeuvers(){
		int size = 3;
		double threshold = 50;
		
		Mat frame = ba.bufferedImageToMat(dc.getbufImg());
		ba.optFlow(frame, true, false);
		double magnitudes[][] = ba.calcOptMagnitude(ba.getVektorArray(), frame, size);
		
		Point center = new Point(frame.size().width/2, frame.size().height/2);
		double x = center.x;
		double y = center.y;
		
		double hStep = frame.size().height/size;
		double vStep = frame.size().width/size;
		
		// Mulige manøvre
		boolean retninger[] = {true,true,true,true,true};// down, up, goLeft, goRight, forward
		
		if(magnitudes[0][0] > threshold){
			x = x - vStep;
			y = y - hStep;
		}
		if(magnitudes[0][1] > threshold){
			y = y - hStep;
			retninger[1] = false;
		}
		if(magnitudes[0][2] > threshold){
			x = x + vStep;
			y = y - hStep;
		}
		if(magnitudes[1][0] > threshold){
			x = x - vStep;
			retninger[2] = false;
		}
		if(magnitudes[1][1] > threshold){
			retninger[4] = false;
		}
		if(magnitudes[1][2] > threshold){
			x = x + vStep;
			retninger[3] = false;
		}
		if(magnitudes[2][0] > threshold){
			x = x - vStep;
			y = y + hStep;
		}
		if(magnitudes[2][1] > threshold){
			y = y + hStep;;
			retninger[0] = false;
		}
		if(magnitudes[2][2] > threshold){
			x = x + vStep;
			y = y + hStep;
		}		
		
//		Vektor dir = new Vektor(center, new Point(x,y)); // Kan benyttes hvis man ønsker en vektorrepræsentation
		return retninger;				
	}
}
