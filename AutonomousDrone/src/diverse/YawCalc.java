package diverse;

public class YawCalc{

	PunktNavigering pn;

	public YawCalc() {
		pn = new PunktNavigering();
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

		A = Math.toDegrees(Math.acos(((b*b)+(c*c)-(a*a))/(2*b*c)));
		B = Math.toDegrees(Math.acos(((a*a)+(c*c)-(b*b))/(2*a*c)));
		C = Math.toDegrees(Math.acos(((b*b)+(a*a)-(c*c))/(2*b*a)));

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

		int nyYaw = (int) tempYaw;

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

		System.err.println("nyYAW: "+nyYaw);
		//			System.out.println();
		return nyYaw;
	}
}