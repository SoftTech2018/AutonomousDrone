package diverse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import billedanalyse.BilledManipulation;
import diverse.circleCalc.Circle;
import diverse.circleCalc.CircleCircleIntersection;
import diverse.circleCalc.Vector2;
import diverse.koordinat.Koordinat;

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
	public Vector2 udregnDronePunkt(Vector2 p1, Vector2 p3, Vector2 p2, double alpha, double beta) {
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
		
	public double getAngle(double x1, double x2){
		double width = 1280;
		double angle = 69;		
		double [] xer = {x1,x2};
		for(int i = 0 ; i < xer.length ; i++){
			double x = xer[i];
			if(x>640){
				x=width-x;
			}
			xer[i] = 149.1470206-23.08239909*Math.log(x);
//			System.out.println(xer[i]);
		}
		return ((angle/width)*(Math.abs(xer[0]-xer[1])));
	}
	
	/**
	 * Beregner distancen til et givent objekt fra kameraet
	 * @param pixels Højden af objektet på billedet i pixels
	 * @param realHeight Objektets rigtige højde i mm
	 * @return Distancen til objektet
	 */
	public double calcDist(int pixels, int realHeight){
//		System.err.println("Pixels: " + pixels);
		return 1.34927 * (realHeight*720)/pixels;
	}
	
	public ArrayList<QrFirkant> findQR(Mat mat) {
		ArrayList<Koordinat> cKor = new ArrayList<Koordinat>();
		ArrayList<QrFirkant> qrFirkanter = new ArrayList<QrFirkant>();

		//Manipulerer billede til findContours
		Mat out = mat; // new Mat();
//		mat.copyTo(out);
		Mat temp = new Mat();
		mat.copyTo(temp);

//		Scalar blackMin = new Scalar(0, 0, 0);
//		Scalar blackMax = new Scalar(180, 255, 50);
//		Core.inRange(temp, blackMin, blackMax, temp);
//		temp = bm.toGray(temp);
		Imgproc.cvtColor(temp, temp, Imgproc.COLOR_BGR2GRAY);
		Imgproc.GaussianBlur(temp, temp, new Size(5,5), -1);
		Imgproc.Canny(temp, temp, 50, 100);

		//Contours gemmes i array
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		//Finder Contours
		Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);

		//Løber Contours igennem
		for(int i=0; i<contours.size(); i++){		
			//Konverterer MatOfPoint til MatOfPoint2f, så ApproxPoly kan bruges
			MatOfPoint2f mop2 = new MatOfPoint2f();
			contours.get(i).convertTo(mop2, CvType.CV_32FC1); 
			double epsilon = 0.01*Imgproc.arcLength(mop2, true);
			Imgproc.approxPolyDP(mop2, mop2, epsilon, true);
			//Konverterer MatOfPoint2f til MatOfPoint
			mop2.convertTo(contours.get(i), CvType.CV_32S);

			if(contours.get(i).total()==4 && Imgproc.contourArea(contours.get(i))>500){ //&& Imgproc.contourArea(contours.get(i))>150{
				List<Point> list = new ArrayList<Point>();
				//				Konverterer contours om til en liste af punkter for at finde koordinaterne
				Converters.Mat_to_vector_Point(contours.get(i), list);	
				double l1 = afstand(list.get(0).x,list.get(1).x,list.get(0).y,list.get(1).y);
				double l2 = afstand(list.get(1).x,list.get(2).x,list.get(1).y,list.get(2).y);
				if(checkFirkant(l1,l2)){
					//Finder den fundne firkants centrum
					double xKor = (list.get(0).x+list.get(1).x+list.get(2).x+list.get(3).x)/4;
					double yKor = (list.get(0).y+list.get(1).y+list.get(2).y+list.get(3).y)/4;
					Koordinat centrum = new Koordinat((int) xKor,(int) yKor);
					if(cKor.isEmpty()){
						cKor.add(centrum);
//						Imgproc.putText(out, Double.toString((int)l1/l2), new Point(list.get(1).x, list.get(1).y), 1, 5, new Scalar(255, 255, 255), 2);
//						Imgproc.putText(out, Double.toString((int)l1*l2), new Point(list.get(0).x, list.get(0).y), 1, 5, new Scalar(255, 255, 255), 2);
//						Imgproc.drawContours(out, contours, i, new Scalar(0,0,255), 3);
						
						QrFirkant qr = new QrFirkant(list.get(0), list.get(1), list.get(2), list.get(3));
						qrFirkanter.add(qr);
					}
					for(int p=0; p<cKor.size();p++){
						int dist = cKor.get(p).dist(centrum);
//						System.err.println(dist);
						if(dist > 20){
							cKor.add(centrum);	
//							Imgproc.putText(out, Double.toString((int)l1/l2), new Point(list.get(1).x, list.get(1).y), 1, 5, new Scalar(255, 255, 255), 2);
//							Imgproc.putText(out, Double.toString((int)l1*l2), new Point(list.get(0).x, list.get(0).y), 1, 5, new Scalar(255, 255, 255), 2);
//							Imgproc.drawContours(out, contours, i, new Scalar(0,0,255), 3);
							
							QrFirkant qr = new QrFirkant(list.get(0), list.get(1), list.get(2), list.get(3));
							qrFirkanter.add(qr);
						}
					}
				}
			}
		}
//		System.out.println("Der er fundet: " + qrFirkanter.size() + " qrFirkanter");
		return qrFirkanter;
	}

	private double afstand(double x1, double x2, double y1, double y2){
		double result = Math.sqrt(Math.pow((x2-x1),2)+Math.pow((y2-y1),2));
		return result;
	}

	private boolean checkFirkant(double l1, double l2){
		double ratio;
		if(l1>l2){
			ratio = l1/l2;
		} else {
			ratio = l2/l1;
		}

		if(ratio>1.3 && ratio<2.9 && l1*l2<80000){
			return true;
		}
		return false;
	}
	
}