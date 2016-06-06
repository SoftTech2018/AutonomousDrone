package billedanalyse;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
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

import billedanalyse.Squares.FARVE;
import diverse.Log;

public class ColorTracker {

	public enum MODE {webcam, droneFront, droneDown};

	private enum COLOR {green, red}

	private static final double MIN_OBJECT_AREA = 800; // Svarer ca. til flyvehøjde på 2,5-3 meter
	private static final double MAX_OBJECT_AREA = 6500; // Svarer ca. til flyvehøjde under 1 meter

	private ArrayList<Squares> squares;
	private Scalar minGreen;
	private Scalar maxGreen;
	private Scalar minRed;
	private Scalar maxRed;
	private Scalar minRed2;
	private Scalar maxRed2;
	private Mat org, out, tempGreen, temp;
	private ArrayList<ColorSetting> csList;

	public ColorTracker(){	
		tempGreen = new Mat();
		temp = new Mat();
		try {
			readFile();
		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void readFile() throws IOException, ClassNotFoundException{
		FileInputStream fin = new FileInputStream("..\\..\\ColorSettings.ser");
		ObjectInputStream ois = new ObjectInputStream(fin);
		csList = (ArrayList<ColorSetting>) ois.readObject();
		ois.close();
		
		ColorSetting csRed = csList.get(0);
		minRed = new Scalar(csRed.getHueMin(), csRed.getSatMin(), csRed.getValMin());
		maxRed = new Scalar(csRed.getHueMax(), csRed.getSatMax(), csRed.getValMax());
		ColorSetting csRed2 = csList.get(1);
		minRed2 = new Scalar(csRed2.getHueMin(), csRed2.getSatMin(), csRed2.getValMin());
		maxRed2 = new Scalar(csRed2.getHueMax(), csRed2.getSatMax(), csRed2.getValMax());
		ColorSetting csGreen = csList.get(2);
		minGreen = new Scalar(csGreen.getHueMin(), csGreen.getSatMin(), csGreen.getValMin());
		maxGreen = new Scalar(csGreen.getHueMax(), csGreen.getSatMax(), csGreen.getValMax());
		
		Log.writeLog("Fil indlæst med farvesettings:");
		for(ColorSetting cs : csList){
			Log.writeLog(cs.toString());
		}
	}

	public void setMode(MODE mode){
//		switch(mode){
//		case webcam:
//			minGreen = Colors.hsvMinGreenWebcam;
//			maxGreen = Colors.hsvMaxGreenWebcam;
//			minRed = Colors.hsvMinRedWebcam;
//			maxRed = Colors.hsvMaxRedWebcam;
//			minRed2 = minRed;
//			maxRed2 = maxRed;
//			break;
//		case droneFront:
//			minGreen = Colors.hsvMinGreenDrone;
//			maxGreen = Colors.hsvMaxGreenDrone;
//			minRed = Colors.hsvMinRedDrone;
//			maxRed = Colors.hsvMaxRedDrone;
//			minRed2 = Colors.hsvMinRedDrone2;
//			maxRed2 = Colors.hsvMaxRedDrone2;
//			break;
//		case droneDown:
//			minGreen = Colors.hsvMinGreenDroneDown;
//			maxGreen = Colors.hsvMaxGreenDroneDown;
//			minRed = Colors.hsvMinRedDroneDown;
//			maxRed = Colors.hsvMaxRedDroneDown;
//			minRed2 = Colors.hsvMinRedDroneDown2;
//			maxRed2 = Colors.hsvMaxRedDroneDown2;
//			break;
//		default:
//			minGreen = Colors.hsvMinGreenDroneDown;
//			maxGreen = Colors.hsvMaxGreenDroneDown;
//			minRed = Colors.hsvMinRedDroneDown;
//			maxRed = Colors.hsvMaxRedDroneDown;
//			minRed2 = Colors.hsvMinRedDroneDown2;
//			maxRed2 = Colors.hsvMaxRedDroneDown2;
//		}
	}

	/** Finder antallet og placering af objekter over en given størrelse, i en bestemt farve. 
	 * Tegner konturer i det tilsendte billede, og returnerer en kopi hvor der kun tegnes hvad kameraet ser.
	 * Udviklet på baggrund af reference:
	 * Reference: https://github.com/ahvsoares/openCVJavaMultipleObjectTracking/blob/master/src/main/java/br/cefetmg/lsi/opencv/multipleObjectTracking/processing/MultipleObjectTracking.java
	 * @param org
	 * @return
	 */
	public Mat findColorObjects(Mat org){
		squares = new ArrayList<>(); // Nulstil arraylisten hver gang denne metode kaldes
		this.org = org;
		org.copyTo(temp);

		Imgproc.cvtColor(org, temp, Imgproc.COLOR_BGR2HSV);

		findColorObjects(COLOR.green);
		findColorObjects(COLOR.red);

		if(BilledAnalyse.BILLED_DEBUG){
			for(Squares s : squares){
				System.out.println(s);
			}
		}
		return out;
	}

	private Mat findColorObjects(COLOR color){	
		out = new Mat();
		// Kombiner begge ranges af hues (170-> 180 og 0->10) for at finde alle røder farver
		if(color.equals(COLOR.red)){
			Mat tempRed = new Mat();
			Core.inRange(temp, minRed, maxRed, tempRed);
			Core.inRange(temp, minRed2, maxRed2, out);
			Core.addWeighted(tempRed, 1.0, out, 1.0, 0.0, out);
		} else {
			Core.inRange(temp, minGreen, maxGreen, out);	
		}
//		Scalar blackMin = new Scalar(0, 0, 0);
//		Scalar blackMax = new Scalar(180, 255, 50);
//		Core.inRange(temp, blackMin, blackMax, out);

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

					// Tegn omridset på det originale billede
					Imgproc.drawContours(org, contours, i, new Scalar(255,0,0), 3); 
					//Imgproc.putText(org, color.toString() + ": " + x + "," + y, new Point(x-(Math.sqrt(area)/2), y), 1, 2, new Scalar(255, 255, 255), 2);
					Imgproc.putText(org, Double.toString(area), new Point(x-(Math.sqrt(area)/2), y), 1, 2, new Scalar(255, 255, 255), 2);
					// Tegn firkanter og tekst på objekt tracking frame for hvert objekt der er fundet
					Imgproc.rectangle(out, new Point(x-(Math.sqrt(area)/2), y-(Math.sqrt(area)/2)), new Point(x+(Math.sqrt(area)/2), y+(Math.sqrt(area)/2)), new Scalar(255,0,0), 3);
					//					Imgproc.circle(out, new Point(x, y), (int) Math.sqrt(area/Math.PI), new Scalar(255,0,0), 3);
					FARVE farve;
					if(color.equals(COLOR.red)){
						farve = FARVE.RØD;
					} else {
						farve = FARVE.GRØN;
					}
					squares.add(new Squares(farve, x, y));
				}
			}
		}
		if(color.equals(COLOR.red)){
			Core.addWeighted(out, 1, tempGreen, 1, 0.0, out);			
		} else {
			out.copyTo(tempGreen);
		}
		if(BilledAnalyse.BILLED_DEBUG){			
			System.out.println(color.toString() + " objekter fundet: " + objectsFound);
		}
		return out;
	}

	public ArrayList<Squares> getSquares() {
		return squares;
	}
}
