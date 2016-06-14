package drone;

import diverse.Log;
import diverse.koordinat.Koordinat;

public class DroneHelper {

	private IDroneControl dc;
	private Koordinat papKasse;

	public DroneHelper(IDroneControl dc, Koordinat papKasse){
		this.dc = dc;
		this.papKasse = papKasse;
	}

	/**
	 * Flyv dronen fra et koordinat til et andet. Dronen drejer sig mod koordinatet
	 * og flyver lige ud.
	 * @param start Start-koordinat
	 * @param slut Slut-koordinat
	 */
	public void flyTo(Koordinat start, Koordinat slut){
		double dist = start.dist(slut); // Beregn distancen til punktet
		if(dist==0){
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

		if(this.papkasseTjek(start, slut, 200)){
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

	public void adjust(Koordinat dronePos, Koordinat koordinat) {
		// TODO Auto-generated method stub

	}
	
	public void setPapKasse(Koordinat papkasse){
		this.papKasse = papkasse;
	}
}
