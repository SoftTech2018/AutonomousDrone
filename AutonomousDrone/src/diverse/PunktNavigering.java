package diverse;

public class PunktNavigering {

	 /*
	  * 3 punkter: 				punktEt 				= (x1, y1) 
	  * 						punktTo 				= (x2, y2) 
	  * 						punktTre				= (x3, y3)
	  * Dronepunkt: 			punktDrone 				= (xD, yD)
	  * 
	  * Afstand mellem punkter: punktEt <-> punktTo 	= a
	  * 						punktTo <-> punktTre 	= b
	  * 
	  * Vinkel mellem punkter: 	punktEt <-> punktTo 	= alpha
	  * 						punktTo <-> punktTre 	= beta
	  * 
	  * Cirklens ligning: 		(x-a)^2 + (y-b)^2 = r^2
	  */
	
	
	public void udregnDronePunkt(int[] punktEt, double[] punktTo, double[] punktTre, double[] punktDrone) {
		
		
		
	}
	
	public double[] udregnCentrum(double[] p1, double[] p2, double alpha) {
		double[] centrum = new double[2];
		double a = afstandMellemPunkter(p1, p2);
		centrum[1] = 	(1/2) * ( (p2[2]-p1[2]) / Math.sqrt(Math.pow((p1[2]-p2[2]), 2) + Math.pow((p1[1]-p2[1]), 2)))
						* Math.sqrt(  ( Math.pow(a,2) / Math.pow(Math.sin(alpha), 2) ) - Math.pow(a, 2) )
						+ (1/2) * p1[1] + (1/2) * p2[1];
		centrum[2] = 	(1/2) * ( (p1[1]-p2[1]) / Math.sqrt(Math.pow((p1[2]-p2[2]), 2) + Math.pow((p1[1]-p2[1]), 2)))
						* Math.sqrt(  ( Math.pow(a,2) / Math.pow(Math.sin(alpha), 2) ) - Math.pow(a, 2) )
						+ (1/2) * p1[2] + (1/2) * p2[2];
		return centrum;
	}
	public double[] findSkæringspunkt(double[] c1, double[] c2, double[] punktDrone) {
		
		//første cirkelligning:
		//(x-a)^2+(y-b)^2 = r^2
		double test = Math.pow((punktDrone[1]-c1[1]),2) + Math.pow((punktDrone[2]-c1[2]),2);
		
		//anden cirkelligning:
		//(x-a)^2+(y-b)^2 = r^2
		double test1 = Math.pow((punktDrone[1]-c2[1]),2) + Math.pow((punktDrone[2]-c2[2]),2);
		
		//De to cirkler sættes lig med hinanden:
		
		return punktDrone;
	}
	
	private double udregnRadius(double[] p1, double[] p2, double alpha) {
		double radius = (1/2)* (afstandMellemPunkter(p1, p2)/Math.sin(alpha));
		return radius;
	}
	

	
	private double afstandMellemPunkter(double[] p1, double[] p2) {
//		int[] ab = new int[2];
//		ab[1] = pt[1] - py[1];
//		ab[2] = pt[2] - py[2];
		double ab = Math.sqrt(Math.pow((p2[1]-p1[1]), 2) + Math.pow((p2[2]-p1[2]), 2));
		
		return ab;
	}
	
	
}