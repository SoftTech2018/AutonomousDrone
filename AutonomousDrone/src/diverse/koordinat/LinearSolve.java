package diverse.koordinat;

import java.io.*;

import diverse.Log;
import diverse.circleCalc.Vector2;


public class LinearSolve  {

	public static double calcA(Vector2 dp, Vector2 newPosition){
		double a = ( newPosition.y- dp.y )/( newPosition.x - dp.x);
		return a;
	}

	public static double calcB(double a, Vector2 newPosition){
		double b;
		b = newPosition.y - a*newPosition.x;
		return b;
	}
	public static Vector2 calcCameraViewPointOnX(double a, double b, double yValue){
//		System.out.println("Hej fra LinearSolver. yValue er : " + yValue);
		double	x = (yValue - b)/a; 
		return new  Vector2(x, yValue);
	}

	public static Vector2 calcCameraViewPointOnY(double a, double b, double xValue){
		double y = a*xValue+b;

//		System.out.println("Hej fra LinearSolver. xValue er : " + xValue);
		return new  Vector2(xValue, y);
	}



	public static double calcY(double a, double b, double xValue){
		double y = a*xValue + b;
		return y;
	}

	public static void main(String[] arg){

		Vector2 dp =  new Vector2(100,800);
		Vector2 newPosition = new Vector2(600,400);
		double a = calcA(dp, newPosition);
		double b = calcB(a, newPosition);
//		System.out.println(a);
//		System.out.println(b);
		//		System.out.println(calcX(a,b,1060));
		//		System.out.println(getYawAngle(dp, new Vector2(calcX(a,b,1060), 1060),300));
	}

	public static double getYawAngle(Vector2 dp, Vector2 cameraPoint, double baseDistance, double baseDegrees){
		double cameraDistance = Math.sqrt(Math.pow(dp.x-cameraPoint.x, 2)+ Math.pow(dp.y-cameraPoint.y,2));
//		System.out.println("Distance fra cam girl til punkt " + cameraDistance);
//		System.out.println("BaseDistance = " +baseDistance);
		
		double degree = Math.toDegrees(Math.acos((baseDistance/cameraDistance)));
		if(baseDegrees == 0){
			if (dp.y > cameraPoint.y){
				degree =  baseDegrees - degree + 360;
			}else{
				degree =  baseDegrees + degree;
			}
		}else if(baseDegrees == 90){
			if (dp.x > cameraPoint.x){
				degree =  baseDegrees + degree;
			}else {
				degree =  baseDegrees - degree;
			}
		}else if(baseDegrees == 180){
			if (dp.y > cameraPoint.y){
				degree =  baseDegrees + degree;
			}else {
				degree =  baseDegrees - degree;
			}
		}else if(baseDegrees == 270){
			if (dp.x > cameraPoint.x){
				degree =  baseDegrees - degree;
				
			}else {
				degree =  baseDegrees + degree;
			}
		}
		Log.writeYawLog("Dronens Position: "+ dp +"\n" + "Punktet Kameraet peger på: " +cameraPoint+"\nDistance fra drone til punket er" + cameraDistance +"\nberegnet vinkel" + degree);



		return degree;
	}



} 