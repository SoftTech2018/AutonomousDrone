package billedanalyse;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import diverse.PunktNavigering;

public class PapKasseFinder {

	private static final int REAL_HEIGHT = 390;
	private int dist = -1;

	/**
	 * Finder papkasser baseret på farve i et billede
	 * @param org Billedet der skal analyseres
	 * @return Koordinaterne for centrum af de identificerede papkasser. Returnerer null hvis intet blev fundet
	 */
	public int findPapkasse(Mat org){

		PunktNavigering pn = new PunktNavigering();

//		org = bufferedImageToMat(UtilImageIO.loadImage(UtilIO.pathExample("C:/Users/ministeren/git/AutonomousDrone/AutonomousDrone/4.jpg")));

		Mat out = new Mat();
		org.copyTo(out);
		//		Mat temp = new Mat();
		//		org.copyTo(temp);
		Mat blurredImage = new Mat();
		Mat hsvImage = new Mat();


		Imgproc.blur(org, blurredImage, new Size(7, 7));

		Imgproc.cvtColor(blurredImage, hsvImage, Imgproc.COLOR_BGR2HSV);

		double huemin = 70;
		double huemax = 180;
		double satmin = 5;
		double satmax = 150;
		double valmin = 35;
		double valmax = 140;

		//		double huemin = 95;
		//		double huemax = 124;
		//		double satmin = 1;
		//		double satmax = 178;
		//		double valmin = 58;
		//		double valmax = 143;

		//		double huemin = 97;
		//		double huemax = 135;
		//		double satmin = 53;
		//		double satmax = 220;
		//		double valmin = 58;
		//		double valmax = 136;

		//		double huemin = 102;
		//		double huemax = 144;
		//		double satmin = 90;
		//		double satmax = 255;
		//		double valmin = 90;
		//		double valmax = 190;

		//		double huemin = 115;
		//		double huemax = 145;
		//		double satmin = 75;
		//		double satmax = 120;
		//		double valmin = 35;
		//		double valmax = 110;

		//				double huemin = 95;
		//				double huemax = 165;
		//				double satmin = 100;
		//				double satmax = 200;
		//				double valmin = 40;
		//				double valmax = 190;

		//		double huemin = 90;
		//		double huemax = 140;
		//		double satmin = 120;
		//		double satmax = 240;
		//		double valmin = 90;
		//		double valmax = 190;

		Scalar minblue = new Scalar(huemin, satmin, valmin);
		Scalar maxblue = new Scalar(huemax, satmax, valmax);

		Core.inRange(hsvImage, minblue, maxblue, out);		

		Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(6, 6));
		Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(16, 16));

		Imgproc.erode(out, out, erodeElement);
		Imgproc.dilate(out, out, dilateElement);
		Imgproc.dilate(out, out, dilateElement);
		Imgproc.erode(out, out, erodeElement);

		Mat findCont = new Mat();
		out.copyTo(findCont);

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(findCont, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

		List<Point> centers = new ArrayList<Point>();
		List<Rect> rects = new ArrayList<Rect>();
		if(contours.size() > 0){
			System.out.println("cont size: " + contours.size());
			//			for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0])
			//			{
			//				Moments moment = Imgproc.moments(contours.get(idx));
			//				double area = moment.get_m00();
			//				int x = (int)(moment.get_m10() / area);
			//				int y = (int)(moment.get_m01() / area);
			//				centers.add(new Point(x,y));
			//				Imgproc.drawContours(org, contours, idx, new Scalar(255,0,0), 3);
			//				Imgproc.rectangle(out, new Point(x-(Math.sqrt(area)/2), y-(Math.sqrt(area)/2)), new Point(x+(Math.sqrt(area)/2), y+(Math.sqrt(area)/2)), new Scalar(255,0,0), 3);
			//			}
			for (int i=0; i< contours.size(); i++){
				MatOfPoint2f approxCurve = new MatOfPoint2f();
				//Convert contours(i) from MatOfPoint to MatOfPoint2f
				MatOfPoint2f contour2f = new MatOfPoint2f( contours.get(i).toArray() );
				//Processing on mMOP2f1 which is in type MatOfPoint2f
				double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
				Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

				//Convert back to MatOfPoint
				MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

				// Get bounding rect of contour
				Rect rect = Imgproc.boundingRect(points);

				Moments moment = Imgproc.moments(contours.get(i));

				double area = moment.get_m00();
				int x = (int)(moment.get_m10() / area);
				int y = (int)(moment.get_m01() / area);
				double mudiff = Math.abs(moment.get_mu20()-moment.get_mu02());
				if(area>3000 && area < 6000){
					System.out.println("rect height: "+rect.height);
					System.out.println("rect width: "+rect.width);
//					Imgproc.rectangle(org, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height), new Scalar(255, 0, 0, 255), 3); 
//					Imgproc.drawContours(org, contours, i, new Scalar(255,0,0), 3);
//					Imgproc.putText(org, ""+i, new Point(x,y), 1, 5, new Scalar(0, 0, 0), 4);
					System.out.println("contour: "+i);
					System.out.println("arclength: "+Imgproc.arcLength(new MatOfPoint2f(contours.get(i).toArray()), true));
					//					System.out.println("rows: "+contours.get(i).rows());
					//					System.out.println("col 0: "+contours.get(i).col(0).rows());
					System.out.println("area: "+area);
					//					for(int m = 0; m < contours.get(i).col(0).rows() ; m++ ){
					//						System.out.println("cont width: "+contours.get(i).col(0).row(m));
					//					}
					centers.add(new Point(x,y));
					rects.add(rect);
					if (moment.get_mu11()>-150000 && moment.get_mu11()<150000){
						//						System.out.println("m00: "+moment.get_m00());
						//						System.out.println("m01: "+moment.get_m01());
						//						System.out.println("m02: "+moment.get_m02());
						//						System.out.println("m03: "+moment.get_m03());
						//						System.out.println("m10: "+moment.get_m10());
						//						System.out.println("m11: "+moment.get_m11());
						//						System.out.println("m12: "+moment.get_m12());
						//						System.out.println("m20: "+moment.get_m20());
						//						System.out.println("m21: "+moment.get_m21());
						//						System.out.println("m30: "+moment.get_m30());
						//						System.out.println("mu02: "+moment.get_mu02());
						//						System.out.println("mu03: "+moment.get_mu03());
						//						System.out.println("mu11: "+moment.get_mu11());
						//						System.out.println("mu12: "+moment.get_mu12());
						//						System.out.println("mu20: "+moment.get_mu20());
						//						System.out.println("mu21: "+moment.get_mu21());
						//						System.out.println("mu30: "+moment.get_mu30());
						//						System.out.println("nu02: "+moment.get_nu02());
						//						System.out.println("nu03: "+moment.get_nu03());
						//						System.out.println("nu11: "+moment.get_nu11());
						//						System.out.println("nu12: "+moment.get_nu12());
						//						System.out.println("nu20: "+moment.get_nu20());
						//						System.out.println("nu21: "+moment.get_nu21());
						//						System.out.println("nu30: "+moment.get_nu30());
						//						System.out.println("mudiff: "+mudiff);
						Imgproc.rectangle(out, new Point(x-(Math.sqrt(area)/2), y-(Math.sqrt(area)/2)), new Point(x+(Math.sqrt(area)/2), y+(Math.sqrt(area)/2)), new Scalar(255,0,0), 3);						
					}					
				}
			}
			System.out.println("centers size: " + centers.size());
		}

		int sumx = 0;
		int sumy = 0;
		int validcenter = 0;
		int distsum = 0;
		double centerx = 0;
		double centery = 0;
		int calcdist = 0;
		dist = -1;

		Point kassecenter = new Point();
		Point kasseh = new Point();
		Point kassel = new Point();
		if (!centers.isEmpty()&& centers.size()>=2){
			//			for(Point p : centers){
			//				sumx+=p.x;
			//				sumy+=p.y;
			//			}
			//			centerx = sumx / centers.size();
			//			centery = sumy / centers.size();
			for(int i = 0; i<centers.size(); i++){
				for(int j = i+1; j<centers.size(); j++){
					if(j<centers.size()){
						double centerix = centers.get(i).x;
						double centerjx = centers.get(j).x;
						if(centerix>centerjx*0.9 && centerix<centerjx*1.1){
							int ydiff = (int) Math.abs(centers.get(i).y-centers.get(j).y);
							System.out.println("ydiff: "+ydiff);
							System.out.println("heigth i: "+rects.get(i).height);
							System.out.println("heigth j: "+rects.get(j).height);
							int heightij = (int)((rects.get(i).height+rects.get(j).height)*1.2);
							System.out.println("heigth i+j: "+heightij);
							if(ydiff<heightij){
								
								validcenter++;

								Imgproc.rectangle(org, new Point(rects.get(i).x,rects.get(i).y), new Point(rects.get(i).x+rects.get(i).width,rects.get(i).y+rects.get(i).height), new Scalar(255, 0, 0, 255), 3); 
								Imgproc.rectangle(org, new Point(rects.get(j).x,rects.get(j).y), new Point(rects.get(j).x+rects.get(j).width,rects.get(j).y+rects.get(j).height), new Scalar(255, 0, 0, 255), 3); 


								int ydiffhalv = ydiff/2;
								double nyy = 0;
								if(centers.get(i).y<centers.get(j).y){
									nyy = centers.get(i).y+ydiffhalv;
								} else {
									nyy = centers.get(i).y-ydiffhalv;
								}
								kassecenter.x = centerix;
								kassecenter.y = nyy;
								calcdist = (int) pn.calcDist(ydiff, REAL_HEIGHT);
								double calcdistmin = calcdist*0.75; 
								double calcdistmax = calcdist*1.25;
								if (dist==-1){
									dist = calcdist;
									//								centerx = kassecenter.x;
									//								centery = kassecenter.y;								
								} else if (calcdistmin<dist && dist>calcdistmax){
									System.out.println("calcdist: "+calcdist);
									dist = (dist+calcdist)/2;
									//								centerx = (centerx+kassecenter.x)/2;
									//								centery = (centery+kassecenter.y)/2;
								}
								distsum+=dist;
								sumx+=kassecenter.x;
								sumy+=kassecenter.y;
//								Imgproc.circle(org, new Point(kassecenter.x,kassecenter.y), 2, new Scalar(255, 255, 255),2);			
//								Imgproc.putText(org, " "+dist+" mm", new Point(kassecenter.x,kassecenter.y), 1, 3, new Scalar(255, 255, 255), 3);							
							}
						}
					}
				}
			}
			if(dist>0){
				sumx=sumx/validcenter;
				sumy=sumy/validcenter;
				distsum=distsum/validcenter;
				Imgproc.circle(org, new Point(sumx,sumy), 2, new Scalar(255, 255, 255),2);			
				Imgproc.putText(org, " "+distsum+" mm", new Point(sumx,sumy), 1, 3, new Scalar(255, 255, 255), 3);		
				
			}
		}


		//		UtilImageIO.saveImage(toBufferedImage(org), UtilIO.pathExample("C:/Users/ministeren/git/AutonomousDrone/AutonomousDrone/kasse.png"));
//		ShowImages.showWindow(toBufferedImage(org),"Title",true);
		return distsum;
	}

	public BufferedImage toBufferedImage(Mat m){
		int type = BufferedImage.TYPE_BYTE_GRAY;
		if ( m.channels() > 1 ) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		int bufferSize = m.channels()*m.cols()*m.rows();
		byte [] b = new byte[bufferSize];
		m.get(0,0,b); // get all the pixels
		BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(b, 0, targetPixels, 0, b.length);  
		return image;
	}

	public Mat bufferedImageToMat(BufferedImage bi) {
		Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
		byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
		mat.put(0, 0, data);
		return mat;
	}

	public int getDist() {
		return dist;
	}

}

