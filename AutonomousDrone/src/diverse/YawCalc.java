package diverse;

import java.io.IOException;

import diverse.circleCalc.Vector2;
import diverse.koordinat.Koordinat;
import diverse.koordinat.LinearSolve;
import diverse.koordinat.M2;
import diverse.koordinat.OpgaveRum;

public class YawCalc{

	private PunktNavigering pn;
	private OpgaveRum opgRum;

	public YawCalc() {
		pn = new PunktNavigering();
	}

	public void setOpgaveRum(OpgaveRum opg){
		this.opgRum = opg;
	}
	
	public double getYaw(QrFirkant qr){		

		QrFirkant sq = qr;

		int line1height = (int) Math.abs(sq.getPoint0().y - sq.getPoint1().y);
		int line2height = (int) Math.abs(sq.getPoint2().y - sq.getPoint3().y);
		int line1x = (int) (sq.getPoint0().x + sq.getPoint1().x)/2;
		int line2x = (int) (sq.getPoint2().x + sq.getPoint3().x)/2;
		int linex = 0;

		int middle = 640;
		int regulator = 0;

		int h = 400;
		int w = 270;

		double A = 0;
		double B = 0;
		double C = 0;

		double a = 0;
		double b = 0;
		double c = 0;

		double nyA = 0;
		double nyB = 0;
		double nyC = 0;

		double dist1 = pn.calcDist(line1height, h);
		double dist2 = pn.calcDist(line2height, h);

		if(dist1 > dist2){
			linex = line2x;
			a = w;
			b = dist2;
			c = dist1;
			if(line1x > line2x){
				regulator = 1;				
			} else {
				regulator = -1;				
			}
		} else {
			linex = line1x;
			a = w;
			b = dist1;
			c = dist2;
			if(line1x > line2x){
				regulator = -1;				
			} else {
				regulator = 1;				
			}
		}		

//		A = Math.toDegrees(Math.acos(((b*b)+(c*c)-(a*a))/(2*b*c)));
//		B = Math.toDegrees(Math.acos(((a*a)+(c*c)-(b*b))/(2*a*c)));
		C = Math.toDegrees(Math.acos(((b*b)+(a*a)-(c*c))/(2*b*a)));
		
		int nyYaw = 0;
		
		if(!Double.isNaN(C)){
			
			double tempYaw = 0;
			nyA = pn.getAngle(middle,linex);
			
			//			double be = (1280/2)/Math.tan(Math.toRadians(69)/2);
			//			nyA = Math.toDegrees(Math.atan(Math.abs(middle-linex)/be));
			
			if(linex > middle && line2x > linex || linex < middle && line2x < linex){
				nyC = 180 - C;
				nyB = 180 - (nyA + nyC);
				tempYaw = regulator*(180 - (90 + (180 - nyB)));
			} else {
				nyC = C;
				nyB = 180 - (nyA + nyC);
				tempYaw = regulator*(180 - (90 + nyB));
			}
			
			nyYaw = (int) tempYaw;
			
			if(qr.getText()!=null || !qr.getText().isEmpty()){
				int wall = Integer.parseInt(""+qr.getText().charAt(2));			
				
				if(wall == 0){
					nyYaw = (int)(-90 + tempYaw);
				} else if (wall == 2){
					nyYaw = (int)(90 + tempYaw);
				} else if (wall == 3){
					if(tempYaw < 0){
						nyYaw = (int)(179 + tempYaw);
					} else {
						nyYaw = (int)(-179 + tempYaw);
					}
				}
			}
		} else {
			nyYaw = -1000;
		}

		System.err.println("nyYAW: "+nyYaw);
		//			System.out.println();
		return nyYaw;
	}
	
	
	public int findYaw(QrFirkant qrFirkant, Koordinat drone){
		Vector2 CameraViewPoint = null;
		PunktNavigering	punktNav = new PunktNavigering();
		
		double baseDegrees = 0;
		double baseDistance = 0;
		double distToCamCenter;
		Vector2 dronePosition = drone.getVector(); 
		boolean isX = false;
		boolean isLeft = false;

		distToCamCenter = Math.abs(640-qrFirkant.getCentrum().getX());

		if(qrFirkant.getCentrum().getX() < 640){
			isLeft = true;
		}else{
			isLeft = false;
		}
		String wall = qrFirkant.getText().substring(0,3);
		// Switch på hvilken væg der kigges på;
		int number = Integer.parseInt(qrFirkant.getText().substring(5,6));
//		System.out.println("Dronens Position er " + dronePosition);
//		System.out.println("Markeringen der kigges på er nummer " + number);
		if(number == 0|| number == 4){
			return -999999999;
		}
		switch (wall) {

		case "W00":
			baseDegrees = 90;
			isX = false;
			break;
		case "W01":
			baseDegrees = 0;
			isX = true;
			break;
		case "W02":
			baseDegrees = 270;
			isX = false;
			break;
		case "W03":
			baseDegrees = 180;
			isX = true;
			break;

		default:
			break;
		}

		// Find afstand fra qrKode til basisKoordinat
		Vector2 qrKoordinat = opgRum.getMultiMarkings(qrFirkant.getText())[1];
//		System.out.println("Det observeret vægmarkering har koordinatet" + qrKoordinat);


		if (isX) {
			baseDistance = Math.abs(qrKoordinat.x - dronePosition.x);

		}else if(!isX ){
			baseDistance = Math.abs(qrKoordinat.y - dronePosition.y);
		}

		

		double phi = punktNav.getAngle(640, qrFirkant.getCentrum().getX());
		if(!isLeft){
			phi = phi * -1;
		}
//		System.out.println("Phi er " + phi);

		M2 m = new M2(Math.cos(Math.toRadians(phi)), -Math.sin(Math.toRadians(phi)), Math.sin(Math.toRadians(phi)), Math.cos(Math.toRadians(phi)));
		Vector2 viewLine = m.mul(qrKoordinat.sub(dronePosition)).add(dronePosition);
//		System.out.println("Det nye roteret koordinat: " +viewLine);

		double a = LinearSolve.calcA(viewLine, dronePosition);
//		System.out.println("hældning af den roteret linje " + a);
		double b = LinearSolve.calcB(a, viewLine);
//		System.out.println("linjen skærer med y " + b);
		if(!isX){
		 CameraViewPoint = LinearSolve.calcCameraViewPointOnX(a, b, qrKoordinat.y);
//		 System.out.println( "Der læses på Y aksen");
		}else{
		 CameraViewPoint = LinearSolve.calcCameraViewPointOnY(a, b, qrKoordinat.x );
//		 System.out.println( "der læses på X aksen");
		}

		Log.writeYawLog("Væggen er: " + wall + "\n phi er: " + phi + " K");
		double angle = LinearSolve.getYawAngle(dronePosition, CameraViewPoint, baseDistance, baseDegrees);
//		System.out.println("************************ Vinklen er " + angle + " *****************************'");

		return (int) angle;
	}
}