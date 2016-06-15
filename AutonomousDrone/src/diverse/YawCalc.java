package diverse;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import billedanalyse.BilledManipulation;
import billedanalyse.Squares;
import billedanalyse.Squares.FARVE;

public class YawCalc{
	
	PunktNavigering pn;
	
	public YawCalc() {
		pn = new PunktNavigering();
	}
	
	public double getYaw(QrFirkant qr){		
		
//		Point p10 = new Point();
//		p10.x = 690;
//		p10.y = 235;
//		
//		Point p11 = new Point();
//		p11.x = 688;
//		p11.y = 408;
//		
//		Point p12 = new Point();
//		p12.x = 596;
//		p12.y = 405;
//		
//		Point p13 = new Point();
//		p13.x = 596;
//		p13.y = 244;
//		
//		Point p20 = new Point();
//		p20.x = 264;
//		p20.y = 271;
//		
//		Point p21 = new Point();
//		p21.x = 262;
//		p21.y = 389;
//		
//		Point p22 = new Point();
//		p22.x = 217;
//		p22.y = 387;
//		
//		Point p23 = new Point();
//		p23.x = 219;
//		p23.y = 276;
//		
//		Point p30 = new Point();
//		p30.x = 955;
//		p30.y = 80;
//		
//		Point p31 = new Point();
//		p31.x = 960;
//		p31.y = 236;
//		
//		Point p32 = new Point();
//		p32.x = 864;
//		p32.y = 251;
//		
//		Point p33 = new Point();
//		p33.x = 861;
//		p33.y = 106;
//		
//		Point p40 = new Point();
//		p40.x = 523;
//		p40.y = 192;
//		
//		Point p41 = new Point();
//		p41.x = 524;
//		p41.y = 295;
//		
//		Point p42 = new Point();
//		p42.x = 480;
//		p42.y = 302;
//		
//		Point p43 = new Point();
//		p43.x = 480;
//		p43.y = 203;
//		
//		QrFirkant sq1 = new QrFirkant(p10, p11, p12, p13);
//		QrFirkant sq2 = new QrFirkant(p20, p21, p22, p23);
//		QrFirkant sq3 = new QrFirkant(p30, p31, p32, p33);
//		QrFirkant sq4 = new QrFirkant(p40, p41, p42, p43);
		
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
		System.err.println("dist1: "+dist1);
		double dist2 = pn.calcDist(line2height, h);
		System.err.println("dist2: "+dist2);
		System.out.println();
		
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

			System.err.println("A: "+A);
			System.err.println("B: "+B);
			System.err.println("C: "+C);
			System.err.println();

			double nyYAW = 0;
			nyA = pn.getAngle(Math.abs(middle-linex));
//			nyA = pn.getAngle(middle,linex);
			
//			double be = (1280/2)/Math.tan(Math.toRadians(69)/2);
//			nyA = Math.toDegrees(Math.atan(Math.abs(middle-linex)/be));
			
			if(linex > middle && line2x > linex || linex < middle && line2x < linex){
				nyC = 180 - C;
				nyB = 180 - (nyA + nyC);
//				int b2 = (int) (180 - nyB);
				nyYAW = regulator*(180 - (90 + (180 - nyB)));
			} else {
				nyC = C;
				nyB = 180 - (nyA + nyC);
				nyYAW = regulator*(180 - (90 + nyB));
			}

//			System.err.println("nyA: "+nyA);
//			System.err.println("nyB: "+nyB);
//			System.err.println("nyC: "+nyC);
//			System.err.println();			
			
			int correction = 0;
			
			
//			System.err.println("nyYAW: "+nyYAW);
//			System.out.println();
			return nyYAW;
	}
}