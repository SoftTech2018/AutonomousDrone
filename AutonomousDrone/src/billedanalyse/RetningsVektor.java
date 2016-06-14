package billedanalyse;

import java.util.ArrayList;
import billedanalyse.Vektor;
import static java.lang.Math.*;
import diverse.circleCalc.Vector2;
import diverse.koordinat.Koordinat;

public class RetningsVektor {
	
	private Vector2 aVector, angleV, yawVector, nyVector;
	Koordinat VfraStart, nytKoord;
	

	
	//Beregn ny vektor: 
	public Vector2 getVector(ArrayList<Vektor> optVectors, int yaw){
		
		ArrayList <Vector2> v2 = omregnTilVektor(optVectors);
		double x = 0;
		double y = 0; 
		//Beregner gennemsnitsvektor 
		aVector = averageVector(v2);
		System.out.println("GENNEMSNITSVEKTOR ER" + aVector.x);
		//Find totalvinkel mellem yaw og gns vektor
		yawVector = unitVector(yaw);
		System.out.println("ENHEDSVEKTOR MED YAW-VINKEL" + yawVector.x + " " + yawVector.y);
		
		double totalVinkel = totalAngle(aVector, yawVector, yaw);
		//Udregn ny enhedsvektor fra totalAngle
		angleV = unitVector((int) totalVinkel); 
		System.out.println("ENHEDSVEKTOR MED TOTALVINKEL" + angleV.x + " " + angleV.y);
		
		//Gang enhedsvektor med længden af gennemsnitsvektor 
		double optLength = vectorLength(aVector);
		x = angleV.x * optLength;
		y = angleV.y * optLength;
				
		nyVector = new Vector2(x, y);
		//Fuld længde af vektor:
		//nytKoord = vectorFromStart(start,nyVector);
		System.out.println("NY VECTOR ER " + nyVector.x + " " + nyVector.y);
		System.out.println(vectorLength(nyVector));
		return nyVector;
	}
	
	
	public Koordinat vectorFromStart(Koordinat start, Vector2 v){
		VfraStart = new Koordinat(((int)v.x) + start.getX(), (int) (v.y+start.getY()));
		return VfraStart;
	}
	
	private ArrayList<Vector2> omregnTilVektor(ArrayList<Vektor> optVectors){
		ArrayList <Vector2> nyeVektorer = new ArrayList();
		if(optVectors.size() > 0){
		for(Vektor v: optVectors){
			v.getX().x = v.getX().x/2.5;
			v.getX().y = v.getX().y/2.5;
			v.getY().x = v.getY().x/2.5;
			v.getY().y = v.getY().x/2.5;
			Vector2 nyVektor = new Vector2((v.getY().x-v.getX().x), v.getY().y-v.getX().y);
			nyeVektorer.add(nyVektor);
			}
		}
		return nyeVektorer;
	}
	

	//Beregn gennemsnitsvektor 
	private Vector2 averageVector(ArrayList<Vector2> allVectors){
		double xTemp = 0;
		double yTemp = 0;
		
		//Læg alle x,y sammen og divider med antal af vektorer
		for(Vector2 v: allVectors){
			xTemp += v.x;
			yTemp += v.y;
		}
		Double x = xTemp/allVectors.size();
		Double y = yTemp/allVectors.size();
		
		aVector = new Vector2(x, y);
		double aLength = vectorLength(aVector); 
		
		System.out.println("AVECTOR ER " + aLength + " " + System.currentTimeMillis());
		return aVector;
		
	}
	
	//Beregn længde på vektorer
	private double vectorLength(Vector2 v){
			double length = sqrt((v.x*v.x)+(v.y*v.y));
			return length;
		}
	
	private double getDegrees(double yaw){
		//Omregn til korrekte grader
		if(yaw < 0){
			yaw = Math.abs(yaw);
		} else {
			yaw = 360-yaw;
		}
		return yaw;	
	}
	
	//Beregn enhedsvektor 
	private Vector2 unitVector(int yaw){
		yaw = (int) getDegrees(yaw);
		
		//Omregn fra grader til radianer
		double radYaw = Math.toRadians(yaw);
		double x = Math.cos(radYaw);
		double y = Math.sin(radYaw);
		
//		x = getDegrees(x);
//		y = getDegrees(y);
		Vector2 unitV = new Vector2(x, y);
		return unitV;
	}
	
	//Beregn vinkel mellem opt vektor og yaw-vektor 
	private double getAngle(Vector2 optV, Vector2 yawV){
		//Beregn skalarprodukt -> a1*b1+a2*b2 
		double skalar = ((optV.x*yawV.x)+(optV.y*yawV.y));
		double optLength = vectorLength(optV);
		double yawLenght = vectorLength(yawV);
		//Beregn cos(v) = skalar/(længde af a)*(længde af b)
		double cos = skalar/(optLength*yawLenght);
		return Math.acos(cos);
	}
	
	private double totalAngle(Vector2 optV, Vector2 yawV, int yaw){
		yaw = (int) getDegrees(yaw);
		//Læg getAngle sammen med yaw
		double angle1 = getAngle(optV, yawV);
		double totAngle=angle1+yaw;
		System.out.println("!!TOTALANGLE ER!! " +totAngle);
		return totAngle;
	}	
}
