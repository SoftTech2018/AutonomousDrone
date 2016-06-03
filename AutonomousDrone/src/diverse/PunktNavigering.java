package diverse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import diverse.circleCalc.Circle;
import diverse.circleCalc.CircleCircleIntersection;
import diverse.circleCalc.Vector2;

public class PunktNavigering {
	
	// Skal debug beskeder udskrives?
		protected static final boolean PNAV_DEBUG = false;
		
	/**
	 * Kigger på ArrayListen og tjekker hvor mange skæringspunkter der findes. 
	 * Hvis der findes mere end to skæringspunkter, tjekkes der op imod det midterste punkt, 
	 * som altid vil være det ene skæringspunkt.
	 * @param p1 Punkt 1 (x,y)
	 * @param p3 Punkt 3 (x,y)
	 * @param p2 Punkt 2 (x,y) - det midterste punkt
	 * @param alpha Vinklen fra dronen mellem p1 og p2
	 * @param beta Vinklen fra dronen mellem p3 og p2
	 * @return Skæringspunktet som ikke er p2.
	 */
	public Vector2 udregnDronePunkt(Vector2 p1, Vector2 p2, Vector2 p3, double alpha, double beta) {
		ArrayList<Vector2> out = findSkæringspunkt(p1, p2, p3, alpha, beta);
		
		if (out.size() == 2) { //Tjekker for om cirklerne ligger tangent på hinanden
			if(out.get(0) == p2) {
				return out.get(1); 
			}
			else if(out.get(1) == p2) {
				return out.get(0);
			}
		} 
		return null; //Dronen har samme koordinat som vægmarkeringen eller også er skæringspunkterne er forkerte
	}

	/**
	 * Beregner skæringspunkter mellem to circler (hvis det findes)
	 * Se tegning af punkter m.m. på side 11: 
	 * https://www.campusnet.dtu.dk/cnnet/filesharing/SADownload.aspx?FileId=4170281&FolderId=971310&ElementId=508171
	 * @param p1 Punkt 1 (x,y)
	 * @param p3 Punkt 3 (x,y)
	 * @param p2 Punkt 2 (x,y) - det midterste punkt
	 * @param alpha Vinklen fra dronen mellem p1 og p2
	 * @param beta Vinklen fra dronen mellem p3 og p2
	 * @return Liste af skæringspunkter. Er tom hvis der ingen skæringspunkter er.
	 */
	private ArrayList<Vector2> findSkæringspunkt(Vector2 p1, Vector2 p3, Vector2 p2, double alpha, double beta) {
		ArrayList<Vector2> out = new ArrayList<Vector2>();

		Vector2 c1 = udregnCentrum(p1, p2, alpha); 		// Centrum på den første cirkel
		Vector2 c2 = udregnCentrum(p3, p2, beta); 		// Centrum på den anden cirkel
		
		Circle circle1 = new Circle(c1, udregnRadius(c1, p2, alpha)); 		//Cirkel baseret på c1
		Circle circle2 = new Circle(c2, udregnRadius(c2, p2, beta));  		//Cirkel baseret på c2
		CircleCircleIntersection ccIntersect = new CircleCircleIntersection(circle1, circle2);

		switch(ccIntersect.type){
		case OVERLAPPING: // To skæringspunkter
			out.add(ccIntersect.intersectionPoint1);
			out.add(ccIntersect.intersectionPoint2);
			break;
		case EXTERNALLY_TANGENT: // Et skæringspunkt
			out.add(ccIntersect.intersectionPoint);
			break;
		default: // Ugyldige eller ingen skæringspunkter
		}
		return out;
	}
	
				//Udregner radius for to punkter i 2d
	private double udregnRadius(Vector2 p1, Vector2 p2, double alpha) {
		double radius = (1/2)* (afstandMellemPunkter(p1, p2)/Math.sin(alpha));
		
		if (PNAV_DEBUG) {
			System.out.println("Radius er: " + radius);
		}
		
		return radius;
	}
				//Udregner afstanden mellem to vektorer i 2d
	private double afstandMellemPunkter(Vector2 p1, Vector2 p2) {
		double ab = Math.sqrt(Math.pow((p2.x-p1.x), 2) + Math.pow((p2.y-p1.y), 2));
		
		if (PNAV_DEBUG) {
			System.out.println("Afstand mellem punkter: " + ab);
		}
		
		return ab;
	}
				//Udregner centrum for 2 punkter
	private Vector2 udregnCentrum(Vector2 p1, Vector2 p2, double alpha) {
		double a = afstandMellemPunkter(p1, p2);
		double x = 	(1/2) * ( (p2.y-p1.y) / Math.sqrt(Math.pow((p1.y-p2.y), 2) + Math.pow((p1.x-p2.x), 2)))
				* Math.sqrt(  ( Math.pow(a,2) / Math.pow(Math.sin(alpha), 2) ) - Math.pow(a, 2) )
				+ (1/2) * p1.x + (1/2) * p2.x;
		double y = 	(1/2) * ( (p1.x-p2.x) / Math.sqrt(Math.pow((p1.y-p2.y), 2) + Math.pow((p1.x-p2.x), 2)))
				* Math.sqrt(  ( Math.pow(a,2) / Math.pow(Math.sin(alpha), 2) ) - Math.pow(a, 2) )
				+ (1/2) * p1.y + (1/2) * p2.y;
		
		if (PNAV_DEBUG) {
			System.out.println("Centrum er: " + "(" + x + ", " + y + ")");
		}
		
		return new Vector2(x,y);
	}
	
	public double droneVendingsGrad(Vector2 dronePunkt, Vector2 vægMark, Vector2 nytPunkt) {
		//Vi rykker vægMark og nytPunkt ned til origo
		vægMark.sub(dronePunkt);
		nytPunkt.sub(dronePunkt);
		
		//Udregning af grad
		double øvreBrøk = vægMark.x*nytPunkt.x + vægMark.y*nytPunkt.y;
		double nedreBrøk = Math.sqrt(Math.pow(vægMark.x,2) + Math.pow(vægMark.y,2) * 
									 Math.pow(nytPunkt.x,2) + Math.pow(nytPunkt.y,2));
		Double resultat = øvreBrøk/nedreBrøk;
		return Math.cos(resultat);
	}
	
}