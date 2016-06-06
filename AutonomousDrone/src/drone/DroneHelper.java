package drone;

import org.opencv.core.Point;

import diverse.koordinat.Koordinat;
import diverse.koordinat.OpgaveRum;

public class DroneHelper {

	private IDroneControl dc;
	private OpgaveRum or;

	public DroneHelper(IDroneControl dc, OpgaveRum or){
		this.dc = dc;
		this.or = or;
	}

	public void flyTo(Koordinat start, Koordinat slut, int vinkel){
		// Beregn hvor meget dronen skal dreje for at "sigte" på slut punktet
		int a = slut.getX() - start.getX();
		int b = slut.getY() - start.getY();
		double dist = Math.sqrt(a*a+b*b); // Beregn distancen til punktet

		if(dist==0){
			return;
		}
		double rotVinkel = a/dist - vinkel;
		System.out.println("Original rotVinkel: " + rotVinkel); // DEBUG
		if(rotVinkel>180){
			rotVinkel = 360 - rotVinkel;
		} else if(rotVinkel<-180){
			rotVinkel = 360 + rotVinkel;
		}

		if(DroneControl.DRONE_DEBUG){
			System.out.println("Dronen skal dreje: " + rotVinkel + " grader.");
			System.out.println("Dronen skal flyve: " + dist + " fremad.");
		}

		if(this.papkasseTjek(start, slut, or.getPapkasse(), 200)){
			Koordinat temp = new Koordinat(500,450); // CENTRUM af rummet
			this.flyTo(start, temp, vinkel); // Flyv til centrum af rummet
			this.flyTo(temp, slut, dc.getFlightData()[2]); // Flyv fra centrum af rummet til landingsplads
		} else {
			// Drej dronen X grader
			dc.turnDrone(rotVinkel);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// Flyv dronen fremad X distance
			dc.flyDrone(dist);
		}
	}

	public void strafePunkt(Koordinat start, Koordinat slut){
		boolean goRight = false;
		if(start.getY() > slut.getY()){
			goRight = true;
		}
		int margin = 200; // Sikkerhedssafstand til papkassen
		Koordinat papKasse = or.getPapkasse();
		if(papkasseTjek(start, slut, papKasse, margin)){
			int yMargin;
			if(!goRight){
				yMargin = papKasse.getY() - margin;
			} else {
				yMargin = papKasse.getY() + margin;
			}
			Koordinat tempPunkt = new Koordinat(start.getX(), yMargin);

			// Flyv til temp punkt
			if(goRight){
				dc.strafeRight(start.getY() - tempPunkt.getY());
			} else {
				dc.strafeLeft(tempPunkt.getY() - start.getY());
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
				new Koordinat(start.getX(), tempPunkt.getY() + 2*margin);
				dc.strafeLeft(slut.getY() - p2.getY());
			}
		} else {
			if(goRight){
				dc.strafeRight(start.getY() - slut.getY());
			} else {
				dc.strafeLeft(slut.getY() - start.getY());
			}
		}
	}

	private boolean papkasseTjek(Koordinat pointA, Koordinat pointB, Koordinat center, double radius) {
		double baX = pointB.getX() - pointA.getX();
		double baY = pointB.getY() - pointA.getY();
		double caX = center.getX() - pointA.getX();
		double caY = center.getY() - pointA.getY();

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
}
