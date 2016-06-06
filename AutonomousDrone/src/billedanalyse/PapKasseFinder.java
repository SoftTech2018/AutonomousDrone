package billedanalyse;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

public class PapKasseFinder {
	
	private static final double MAX_OBJECT_AREA = 5000;
	private static final double MIN_OBJECT_AREA = 500;

	/**
	 * Finder papkasser baseret på farve i et billede
	 * @param org Billedet der skal analyseres
	 * @return Koordinaterne for centrum af de identificerede papkasser. Returnerer null hvis intet blev fundet
	 */
	public Point findPapkasse(Mat org){
		double pX = 0, pY = 0;
		Mat out = new Mat();
		Scalar minBlue = new Scalar(75, 0, 0); // TODO
		Scalar maxBlue = new Scalar(130, 0, 0); // TODO
		
		Core.inRange(org, minBlue, maxBlue, out);	

		Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
		Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(8, 8));
		Imgproc.erode(out, out, erodeElement);
		Imgproc.erode(out, out, erodeElement);

		Imgproc.dilate(out, out, dilateElement);
		Imgproc.dilate(out, out, dilateElement);

		Mat findCont = new Mat();
		out.copyTo(findCont);

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(findCont, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

		int objectsFound = 0;
		if(contours.size() > 0){
			for (int i=0; i< contours.size(); i++){
				Moments moment = Imgproc.moments(contours.get(i));
				double area = moment.get_m00();

				// Forsøger at fjerne støj
				if (MAX_OBJECT_AREA > area && area > MIN_OBJECT_AREA) {
					objectsFound++;
					int x = (int)(moment.get_m10() / area);
					int y = (int)(moment.get_m01() / area);
					pX += x;
					pY += y;

					// Tegn omridset på det originale billede
					Imgproc.drawContours(org, contours, i, new Scalar(255,0,0), 3); 
					//Imgproc.putText(org, color.toString() + ": " + x + "," + y, new Point(x-(Math.sqrt(area)/2), y), 1, 2, new Scalar(255, 255, 255), 2);
					Imgproc.putText(org, Double.toString(area), new Point(x-(Math.sqrt(area)/2), y), 1, 2, new Scalar(255, 255, 255), 2);
					// Tegn firkanter og tekst på objekt tracking frame for hvert objekt der er fundet
//					Imgproc.rectangle(out, new Point(x-(Math.sqrt(area)/2), y-(Math.sqrt(area)/2)), new Point(x+(Math.sqrt(area)/2), y+(Math.sqrt(area)/2)), new Scalar(255,0,0), 3);
					// Imgproc.circle(out, new Point(x, y), (int) Math.sqrt(area/Math.PI), new Scalar(255,0,0), 3);
				}
			}
		}
		if(BilledAnalyse.BILLED_DEBUG){			
			System.out.println("Papkasser fundet: " + objectsFound);
		}
		if(objectsFound>0){
			return new Point(pX/objectsFound, pY/objectsFound);			
		}
		return null;
	}

}
