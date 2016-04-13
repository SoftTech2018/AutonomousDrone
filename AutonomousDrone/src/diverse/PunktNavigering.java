package diverse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import diverse.circleCalc.Circle;
import diverse.circleCalc.CircleCircleIntersection;
import diverse.circleCalc.Vector2;

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
	public ArrayList<Vector2> findSkæringspunkt(Vector2 p1, Vector2 p3, Vector2 p2, double alpha, double beta) {
		ArrayList<Vector2> out = new ArrayList<Vector2>();

		Vector2 c1 = udregnCentrum(p1, p2, alpha); // Centrum på den første cirkel
		Vector2 c2 = udregnCentrum(p3, p2, beta); // Centrum på den anden cirkel

		Circle circle1 = new Circle(c1, udregnRadius(c1, p2, alpha));
		Circle circle2 = new Circle(c2, udregnRadius(c2, p2, beta));
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

	private double udregnRadius(Vector2 p1, Vector2 p2, double alpha) {
		double radius = (1/2)* (afstandMellemPunkter(p1, p2)/Math.sin(alpha));
		return radius;
	}

	private double afstandMellemPunkter(Vector2 p1, Vector2 p2) {
		//		int[] ab = new int[2];
		//		ab[1] = pt[1] - py[1];
		//		ab[2] = pt[2] - py[2];
		double ab = Math.sqrt(Math.pow((p2.x-p1.x), 2) + Math.pow((p2.y-p1.y), 2));

		return ab;
	}
	
	private Vector2 udregnCentrum(Vector2 p1, Vector2 p2, double alpha) {
		double a = afstandMellemPunkter(p1, p2);
		double x = 	(1/2) * ( (p2.y-p1.y) / Math.sqrt(Math.pow((p1.y-p2.y), 2) + Math.pow((p1.x-p2.x), 2)))
				* Math.sqrt(  ( Math.pow(a,2) / Math.pow(Math.sin(alpha), 2) ) - Math.pow(a, 2) )
				+ (1/2) * p1.x + (1/2) * p2.x;
		double y = 	(1/2) * ( (p1.x-p2.x) / Math.sqrt(Math.pow((p1.y-p2.y), 2) + Math.pow((p1.x-p2.x), 2)))
				* Math.sqrt(  ( Math.pow(a,2) / Math.pow(Math.sin(alpha), 2) ) - Math.pow(a, 2) )
				+ (1/2) * p1.y + (1/2) * p2.y;
		return new Vector2(x,y);
	}
}