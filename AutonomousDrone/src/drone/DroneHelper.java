package drone;

/*
 * KLASSENS FORMÅL: Give hjælpemetoder til dronenavigation baseret på rummets koordinater
 * Eks: Start i koordinat X - flyv til koordinat Y
 */
import diverse.Log;
import diverse.koordinat.Koordinat;
import diverse.koordinat.OpgaveRum;

public class DroneHelper {

	private IDroneControl dc;
	private Koordinat papKasse;
	private int adjustment = 100, xMax, xMin, yMax, yMin, directionChange = 0, y, x;

	public enum DIRECTION { UP, DOWN, LEFT, RIGHT, STOPPED };

	/**
	 * 
	 * @param dc Implementation af IDroneControl
	 * @param x Bredden af det fysiske rum i cm
	 * @param y Længden/højden af det fysiske rum i cm
	 */
	public DroneHelper(IDroneControl dc, int x, int y){
		this.dc = dc;
		xMax = x - 300;
		xMin = 450;
		yMax = y - 300;
		yMin = 300;
		this.y = y;
		this.x = x;
	}

	/**
	 * Flyv dronen fra et koordinat til et andet. Dronen drejer sig mod koordinatet
	 * og flyver lige ud.
	 * @param start Start-koordinat
	 * @param slut Slut-koordinat
	 */
	public void flyTo(Koordinat start, Koordinat slut){
		double dist = start.dist(slut); // Beregn distancen til punktet
		if(dist<50){ // Mindre end 50 cm fra landingsplads
			Log.writeLog("Distance mellem koordinater er under 50 cm. Dronen bevæges ikke");
			return;
		}
		int vinkel = dc.getFlightData()[2];

		// Korrigerer for dronens "sjove" YAW værdier
		if(vinkel < 0){
			vinkel = Math.abs(vinkel);
		} else {
			vinkel = 360-vinkel;
		}

		// *** Beregn hvor meget dronen skal dreje for at "sigte" på slut punktet ***
		// Vektor fra nuværende punkt til slutpunkt
		int a = slut.getX() - start.getX();
		int b = slut.getY() - start.getY();
		double abLength = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));

		// Enhedsvektor i retning af YAW
		double yA = Math.cos(Math.toRadians(vinkel));
		double yB = Math.sin(Math.toRadians(vinkel));
		double yLength = Math.sqrt(Math.pow(yA, 2) + Math.pow(yB, 2));

		// Beregn vinkel mellem de to vektorer. Dette er vinklen der skal roteres
		double rotVinkel = Math.toDegrees(Math.acos(((a*yA)+(b*yB))/(abLength*yLength)));
		double retning = -a * yB + b * yA; // Negativ hvis dronen skal dreje til højre
		if(retning > 0){
			rotVinkel = rotVinkel * (-1); // Korrigeret vinkel
		}
		Log.writeLog("Vinkel der skal roteres: " + rotVinkel);
		Log.writeLog("Distance: " + dist);

		if(DroneControl.DRONE_DEBUG){
			System.out.println("Dronen skal dreje: " + rotVinkel + " grader.");
			System.out.println("Dronen skal flyve: " + dist + " fremad.");
		}
		Log.writeLog("Dronen skal dreje: " + rotVinkel + " grader.");
		Log.writeLog("Dronen skal flyve: " + dist + "cm fremad.");

		if(this.papkasseTjek(start, slut, 100)){
			Log.writeLog("Dronens rute går gennem papkassen. Forsøger alternativ rute.");
			Koordinat temp = new Koordinat(500,450); // CENTRUM af rummet
			this.flyTo(start, temp); // Flyv til centrum af rummet
			this.flyTo(temp, slut); // Flyv fra centrum af rummet til landingsplads
		} else {
			// Drej dronen X grader
			Log.writeLog("Dronen drejes: " + rotVinkel);
			dc.turnDrone(rotVinkel);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// Flyv dronen fremad X distance
			Log.writeLog("Dronen flyver fremad: " + dist);
			dc.flyDrone(dist);
		}
	}

	/**
	 * Strafe fra et start punkt til et slut punkt. YAW justeres IKKE.
	 * Der tages højde for om Papkassen vil rammes i oprindelig rute, og 
	 * ruten justeres derefter.
	 * @param start
	 * @param slut
	 */
	public void strafePunkt(Koordinat start, Koordinat slut){
		boolean goRight = false;
		if(start.getY() > slut.getY()){
			goRight = true;
		}
		int margin = 80; // Sikkerhedssafstand til papkassen

		if(papkasseTjek(start, slut, margin)){
			int yMargin;
			if(!goRight){
				yMargin = papKasse.getY() - margin;
			} else {
				yMargin = papKasse.getY() + margin;
			}
			Koordinat tempPunkt = new Koordinat(start.getX(), yMargin);

			// Flyv til temp punkt
			if(goRight){
				int dist = start.getY() - tempPunkt.getY();
				Log.writeLog("Strafer højre: \t" + dist);
				dc.strafeRight(dist);
			} else {
				int dist = tempPunkt.getY() - start.getY();
				Log.writeLog("Strafer venstre: \t" + dist);
				dc.strafeLeft(dist);
			}

			// Naviger udenom papkassen
			if(tempPunkt.getX() > papKasse.getX()){
				// Frem, Strafe, Tilbage
				int xDist = papKasse.getX() + margin;
				dc.flyDrone(xDist - tempPunkt.getX()); //fremad
				if(goRight){
					// Strafe højre
					dc.strafeRight(margin*2);
				} else {
					//strafe venstre
					dc.strafeLeft(margin*2);
				}
				dc.flyDrone(tempPunkt.getX() - xDist); // tilbage (minus distance)
			} else {
				// Tilbage, Strafe, Frem
				int xDist = papKasse.getX() + margin;
				dc.flyDrone(tempPunkt.getX() - xDist); // tilbage (minus distance)
				if(goRight){
					// Strafe højre
					dc.strafeRight(margin*2);
				} else {
					//strafe venstre
					dc.strafeLeft(margin*2);
				}
				dc.flyDrone(xDist - tempPunkt.getX()); //fremad
			}
			Koordinat p2;
			if(goRight){
				p2 = new Koordinat(start.getX(), tempPunkt.getY() - 2*margin);
				dc.strafeRight(p2.getY() - slut.getY());
			} else {
				p2 = new Koordinat(start.getX(), tempPunkt.getY() + 2*margin);
				dc.strafeLeft(slut.getY() - p2.getY());
			}
		} else {
			if(goRight){
				dc.strafeRight(start.getY() - slut.getY());
			} else {
				int dist = slut.getY() - start.getY();
				Log.writeLog("Strafer venstre: " + dist);
				dc.strafeLeft(dist);
			}
		}
	}

	/**
	 * Tjekker om linjen mellem to koordinater rammer papkassen
	 * @param pointA Startkoordinat
	 * @param pointB Slutkoordinat
	 * @param radius Sikkerhedsmargin til papkassen
	 * @return true hvis linjen rammer papkassen
	 */
	private boolean papkasseTjek(Koordinat pointA, Koordinat pointB, double radius) {
		if(papKasse==null){
			return false;
		}
		double baX = pointB.getX() - pointA.getX();
		double baY = pointB.getY() - pointA.getY();
		double caX = papKasse.getX() - pointA.getX();
		double caY = papKasse.getY() - pointA.getY();

		double a = baX * baX + baY * baY;
		double bBy2 = baX * caX + baY * caY;
		double c = caX * caX + caY * caY - radius * radius;

		double pBy2 = bBy2 / a;
		double q = c / a;

		double disc = pBy2 * pBy2 - q;
		if (disc < 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Juster dronens koordinater vha. frem/tilbage + strafe.
	 * Bør ikke benyttes til at flyve større distancer
	 * @param dronePos Start-koordinat
	 * @param koordinat Slut-koordinat
	 */
	public void adjust(Koordinat dronePos, Koordinat koordinat) {
		// TODO Auto-generated method stub

	}

	/**
	 * Sæt papkassens koordinat i rummet
	 * @param papkasse
	 */
	public void setPapKasse(Koordinat papkasse){
		this.papKasse = papkasse;
	}

	/**
	 * Bevæger dronen i et bestem mønster baseret på dets nuværende position
	 * @param drone
	 * @throws InterruptedException 
	 */
	public DIRECTION moveDrone(Koordinat drone, DIRECTION lastDir) throws InterruptedException {
		DIRECTION newDir = null;
		if(drone.getX() >= xMax) { // Tæt ved vinduet
			if(drone.getY() > yMin){ // "Øverste del af rummet"
				newDir = DIRECTION.DOWN;
			} else {
				newDir = DIRECTION.LEFT;
			}
		} else if (drone.getX() <= xMin && drone.getX() >= xMin - 200){ // Langt fra vinduet
			if(drone.getY() > yMax){ // "Øverste del af rummet"
				newDir = DIRECTION.RIGHT;
			} else {
				newDir = DIRECTION.UP;
			}
		} else if (drone.getX() > xMin && drone.getX() < xMax){ // Midten af rummet (x-retning)
			if(drone.getY() > yMax){ // "Øverste del af rummet"
				newDir = DIRECTION.RIGHT;
			} else if(drone.getY() < yMin){ // Nederste
				newDir = DIRECTION.LEFT;
			} else if(yMax > drone.getY() && yMin < drone.getY()){ // midten
				newDir = DIRECTION.RIGHT;
			}
		} else if(drone.getX() < xMin - 200) { // MEGET langt fra vinduet
			newDir = DIRECTION.RIGHT;
		}

		// Beregn den estimerede position når næste bevægelse er udført
		// Tjek om bevægelsen fra nuværende til estimeret position vil ramme papkassen
		int xKoor = drone.getX();
		int yKoor = drone.getY();
		switch(newDir){
		case UP:
			yKoor += 100;
			break;
		case DOWN:
			yKoor -= 100;
			break;
		case LEFT:
			xKoor -= 100;
			break;
		case RIGHT:
			xKoor += 100;
			break;
		case STOPPED:
			break;
		}
		Koordinat estimeretPosition = new Koordinat(xKoor, yKoor);
		if(this.papkasseTjek(drone, estimeretPosition, 100)){
			Log.writeLog("Dronens rute rammer papkassen! Forsøger alternativ rute.");
			switch(newDir){ // Flyv udenom papkassen
			case UP:
				move(DIRECTION.LEFT);
				break;
			case DOWN:
				move(DIRECTION.RIGHT);
				break;
			case LEFT:
				move(DIRECTION.DOWN);
				break;
			case RIGHT:
				move(DIRECTION.UP);
				break;
			case STOPPED:
				break;
			}
		} else { // Bevægelsen rammer ikke papkassen
			move(newDir);		
		}
		if(lastDir == null){ // Når vi starter er lastDir null
			lastDir = newDir;
		}
		// Når der er fløjet en runde (3 retningsskift), justeres dronens bane
		if(newDir != lastDir){
			directionChange++;
			yMax = yMax - adjustment *(int) (directionChange / 4);
			xMax = xMax - adjustment *(int) (directionChange / 4);
			yMin = yMin + adjustment *(int) (directionChange / 4);
			xMin = xMin + adjustment *(int) (directionChange / 4);
		}
		Log.writeLog("Direction: " + newDir);
		return newDir;
	}

	/**
	 * Justerer YAW-værdien baseret på ønsket retning, og bevæger dronen vha. strafe, frem eller tilbage
	 * @param dir Hvilken retning skal dronen bevæge sig i?
	 * @throws InterruptedException
	 */
	private void move(DIRECTION dir) throws InterruptedException{
		int yaw = dc.getFlightData()[2];
		final int orgYaw = yaw; // Benyttes kun til debug
		// Korrigerer for dronens "sjove" YAW værdier, så vi får værdier fra 0-360 grader
		if(yaw < 0){
			yaw = Math.abs(yaw);
		} else {
			yaw = 360-yaw;
		}

		switch(dir){
		case UP: // Basis
			if(yaw >= 0 && yaw < 45 || yaw >= 315 && yaw <=360){ // Kigger mod rudevæggen
				Log.writeLog("Bevæger dronen: \tSTRAFE VENSTRE\t YAW: " + orgYaw);
				dc.strafeLeft(300);
			} else if (yaw >= 225 && yaw < 315){ // Kigger nedaf y-aksen (minus retning)
				Log.writeLog("Bevæger dronen: \tBAGLÆNS\t YAW: " + orgYaw);
				dc.backward();
			} else if (yaw >= 45 && yaw < 135){ // Kigger op af y-aksen (plus retning)
				Log.writeLog("Bevæger dronen: \tFREMAD\t YAW: " + orgYaw);
				dc.forward();
			} else if (yaw >= 135 && yaw < 225){ // Kigger modsat af rudevæggen
				Log.writeLog("Bevæger dronen: \tSTRAFE HØJRE\t YAW: " + orgYaw);
				dc.strafeRight(300);
			}
			break;
		case DOWN:
			if(yaw >= 0 && yaw < 45 || yaw >= 315 && yaw <=360){ // Kigger mod rudevæggen
				Log.writeLog("Bevæger dronen: \tSTRAFE HØJRE\t YAW: " + orgYaw);
				dc.strafeRight(300);
			} else if (yaw >= 225 && yaw < 315){ // Kigger nedaf y-aksen (minus retning)
				Log.writeLog("Bevæger dronen: \tFREMAD\t YAW: " + orgYaw);
				dc.forward();
			} else if (yaw >= 45 && yaw < 135){ // Kigger op af y-aksen (plus retning)
				Log.writeLog("Bevæger dronen: \tBAGLÆNS\t YAW: " + orgYaw);
				dc.backward();
			} else if (yaw >= 135 && yaw < 225){ // Kigger modsat af rudevæggen
				Log.writeLog("Bevæger dronen: \tSTRAFE VENSTRE\t YAW: " + orgYaw);
				dc.strafeLeft(300);
			}
			break;
		case LEFT:
			if(yaw >= 0 && yaw < 45 || yaw >= 315 && yaw <=360){ // Kigger mod rudevæggen
				Log.writeLog("Bevæger dronen: \tBAGLÆNS\t YAW: " + orgYaw);
				dc.backward();
			} else if (yaw >= 225 && yaw < 315){ // Kigger nedaf y-aksen (minus retning)
				Log.writeLog("Bevæger dronen: \tSTRAFE HØJRE\t YAW: " + orgYaw);
				dc.strafeRight(300);
			} else if (yaw >= 45 && yaw < 135){ // Kigger op af y-aksen (plus retning)
				Log.writeLog("Bevæger dronen: \tSTRAFE VENSTRE\t YAW: " + orgYaw);
				dc.strafeLeft(300);
			} else if (yaw >= 135 && yaw < 225){ // Kigger modsat af rudevæggen
				Log.writeLog("Bevæger dronen: \tFREMAD\t YAW: " + orgYaw);
				dc.forward();
			}
			break;
		case RIGHT:
			if(yaw >= 0 && yaw < 45 || yaw >= 315 && yaw <=360){ // Kigger mod rudevæggen
				Log.writeLog("Bevæger dronen: \tFREMAD\t YAW: " + orgYaw);
				dc.forward();
			} else if (yaw >= 225 && yaw < 315){ // Kigger nedaf y-aksen (minus retning)
				Log.writeLog("Bevæger dronen: \tSTRAFE VENSTRE\t YAW: " + orgYaw);
				dc.strafeLeft(300);
			} else if (yaw >= 45 && yaw < 135){ // Kigger op af y-aksen (plus retning)
				Log.writeLog("Bevæger dronen: \tSTRAFE HØJRE\t YAW: " + orgYaw);
				dc.strafeRight(300);
			} else if (yaw >= 135 && yaw < 225){ // Kigger modsat af rudevæggen
				Log.writeLog("Bevæger dronen: \tBAGLÆNS\t YAW: " + orgYaw);
				dc.backward();
			}
			break;
		case STOPPED:
			Log.writeLog("Bevæger dronen: \tHOVER\t YAW: " + orgYaw);
			dc.hover();
		}
	}

	public int getCenterAreal(){
		int x = xMax - xMin;
		int y = yMax - yMin;
		if(this.papKasse == null) {
			return (int) ((x*y)/4); // Hvis papkassen ikke er fundet benyttes en større sikkerhedsmargin
		}
		return x*y;
	}

	public Koordinat getPapkasse() {
		return papKasse;
	}

	/**
	 * Flyver YOLO-style fra et start koordinat til et slut koordinat udelukkende vha. frem, tilbage, strafe
	 * Det antages at dronens YAW = 0.
	 * @param start
	 * @param slut
	 * @return True hvis bevægelsen blev udført. False hvis der ikke er en gyldig rute (udenom papkassen)
	 * @throws InterruptedException
	 */
	public boolean flyToKoordinat(Koordinat start, Koordinat slut) throws InterruptedException{
		int xDist = slut.getX()- start.getX();
		int yDist = slut.getY() - start.getY();
		Log.writeLog("Dronen skal flyttes: " + xDist + "cm af X-aksen.");
		Log.writeLog("Dronen skal flyttes: " + yDist + "cm af Y-aksen.");
		if(!this.papkasseTjek(start, new Koordinat(slut.getX(), start.getY()), 100)){
			if(!this.papkasseTjek(new Koordinat(slut.getX(), start.getY()), slut, 100)){
				// Bevæg frem/tilbage først
				while(xDist > 50){
					dc.forward();
					xDist = xDist - 75;
					Thread.sleep(3000);
				}
				while(xDist < -50){
					dc.backward();
					xDist = xDist + 75;
					Thread.sleep(3000);
				}	
				while(yDist > 50){
					dc.strafeLeft(0);
					yDist = yDist - 75;
					Thread.sleep(3000);
				}
				while(yDist < -50){
					dc.strafeRight(0);
					yDist = yDist + 75;
					Thread.sleep(3000);
				}
				return true;
			}
		} else if(!this.papkasseTjek(start, new Koordinat(start.getX(), slut.getY()), 100)){
			if(!this.papkasseTjek(new Koordinat(start.getX(), slut.getY()), slut, 100)){
				// Bevæg venstre/højre først
				while(yDist > 50){
					dc.strafeLeft(0);
					yDist = yDist - 75;
					Thread.sleep(3000);
				}
				while(yDist < -50){
					dc.strafeRight(0);
					yDist = yDist + 75;
					Thread.sleep(3000);
				}
				while(xDist > 50){
					dc.forward();
					xDist = xDist - 75;
					Thread.sleep(3000);
				}
				while(xDist < -50){
					dc.backward();
					xDist = xDist + 75;
					Thread.sleep(3000);
				}	
				return true;
			}
		} 
		return false;
	}

	/**
	 * Drejer dronen så den peger mod den korrekte væg i forhold til sin position i rummet
	 * @param drone Dronens position
	 * @return
	 * @throws InterruptedException
	 */
	public boolean turnDroneByPosition(Koordinat drone) throws InterruptedException {
		boolean adjusted = false;
		int yaw = dc.getFlightData()[2];
		if(drone.getX() > x - 300){ // Tæt på glasvæggen
			if(drone.getY() > 400){
				Log.writeLog("Peger drone mod W01. " + drone);
				dc.turnDroneTo(0); // Peg mod glasvæg
				adjusted = true;
			} else if (drone.getY() < 400){
				Log.writeLog("Peger drone mod W02. " + drone);
				dc.turnDroneTo(90);
				adjusted = true;
			} else {
				Log.writeLog("Peger drone mod W00. " + drone);
				dc.turnDroneTo(-90);
				adjusted = true;
			}
		} else if (drone.getY() > y-550){ // Langt fra glasvæg og i toppen af rummet
			Log.writeLog("Peger drone mod W00");
			dc.turnDroneTo(-90); // Peg mod væg0
			adjusted = true;
		} else if (drone.getX() < 300 && drone.getY() > 400 && drone.getY() < y - 500){
			// Langt fra glasvæg og midt i rummet
			Log.writeLog("Peger drone mod W03");	
			dc.turnDroneTo(-179);
			adjusted = true;
		} else if (drone.getY() < 400){
			Log.writeLog("Peger drone mod W02");
			dc.turnDroneTo(90);
			adjusted = true;
		}
		if(adjusted){			
			Log.writeLog("Droneretning er justeret");
			Thread.sleep(2000);
		} else {
			Log.writeLog("Droneretning er IKKE justeret");
		}
		return adjusted;
	}


}
