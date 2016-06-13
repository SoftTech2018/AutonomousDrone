package billedanalyse;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import diverse.circleCalc.Vector2;

public class OpticalFlow {
	
	private BilledManipulation bm;
	private Mat first;
	private MatOfPoint fKey;
	private ArrayList<Vektor> vList;
	
	public OpticalFlow(BilledManipulation bm){
		this.bm = bm;
	}
	
	/**
	 * Hent de sidst fundne vektorer mellem to analyserede billeder. 
	 * Listen er renset for usandsynlige matches.
	 * @return ArrayList af vektorer
	 */
	public ArrayList<Vektor> getVektorArray(){	
		ArrayList<Vektor> v2 = new ArrayList();
		for(Vektor v: vList){
			v2.add(v);
		}
		vList.clear();
		return v2;
	}
	
	private Mat trackObject(Mat frame) {
		Mat klon = frame.clone();
		// TODO Auto-generated method stub
		return klon;
	}
	
	public Vektor getDroneMovement(){
		int xDir = 0;
		int yDir = 0;
		for(Vektor v : vList){
//			xDir += v.getX()
		}
		return null;
	}
	
	/**
	 * Udfører Optical Flow analyse mellem to frames og tegner resultatet på det returnede frame. 
	 * Optical Flow udføres først anden gang metoden kaldes.
	 * @param frame Frame der bliver påtegnet vektorer
	 * @param optFlow Hvorvidt der skal udføres Optical Flow
	 * @param objTrack Hvorvidt der skal trackes objekter
	 * @return Mat frame - påtegnet resultatet (vektorer)
	 */
	public Mat optFlow(Mat second, boolean optFlow){
		if(optFlow){
			// Første gang metoden kaldes gemmes billedet og der tegnes ingen vektorer.
			if(first==null){
				first = new Mat();
				second.copyTo(first);
				fKey = new MatOfPoint();

				//			first = this.gaus(first);
				first = bm.edde(first);
				first = bm.thresh(first);
				first = bm.toGray(first);
//				first = bm.canny(first);

				Imgproc.goodFeaturesToTrack(first, fKey, 400, 0.01, 10);

				return first;
			}
			Mat out = new Mat();
			second.copyTo(out);

			long startTime = System.nanoTime(); // DEBUG

			// Initier variable der gemmes data i
			MatOfPoint sKey = new MatOfPoint();

			// Behandling af billedet som fremhæver features
			//		second = this.gaus(second);
			out = bm.edde(out);
			out = bm.thresh(out);
			out = bm.toGray(out);
//			out = bm.canny(out);

			// Find punkter der er gode at tracke. Gemmes i fKey og sKey
			Imgproc.goodFeaturesToTrack(out, sKey, 400, 0.01, 10);

			// Hvis der ikke findes nogle features er der intet at tegne eller lave optical flow på
			if(sKey.empty()){
				System.err.println("******** NUL FEATURES FUNDET! ************** ");
				return out;
			}

			// Kør opticalFlowPyrLK
			MatOfPoint2f sKeyf = new MatOfPoint2f(sKey.toArray());
			MatOfPoint2f fKeyf = new MatOfPoint2f(fKey.toArray());
			MatOfByte status = new MatOfByte();
			MatOfFloat err = new MatOfFloat();
			Video.calcOpticalFlowPyrLK(first, out, fKeyf, sKeyf, status, err );
			
			// Gem det behandlede billede samt data så det kan benyttes næste gang metoden kaldes
			out.copyTo(first);
			fKey = sKey;

			// Find og Tegn vektorer på kopien af originale farvebillede 
			byte[] fundet = status.toArray();
			Point[] fArray = fKeyf.toArray();
			Point[] sArray = sKeyf.toArray();
			int thickness = 2;
			vList = new ArrayList<Vektor>();
			double vektorLength = 0;
			for(int i=0; i<fArray.length; i++){
				if(fundet[i] == 1){ // Tilføj kun vektorer hvor der er fundet matches
					Vektor v = new Vektor(fArray[i],sArray[i]);
					vList.add(v);
					vektorLength += v.getLength();
				}		
			}
			vektorLength = vektorLength/vList.size();
			// Fjerner støj - dvs. vektorer der er markant længere end gennemsnittet
			int p =0;
			do {
				if(vList.get(p).getLength() > vektorLength * 1.5){
					vList.remove(p);
					p--;
				}
				p++;
			} while(p < vList.size());
			// Tegn vektorerne på billedet
			for(int i=0; i<vList.size(); i++){
				Imgproc.line(out, vList.get(i).getX(), vList.get(i).getY(), new Scalar(255,0,0), thickness);
			}

			//		double avg = 0;
			//		for(int p=0; p<vList.size(); p++){
			//			avg = avg + vList.get(p).getLength();
			//		}
			//		avg = avg/vList.size();
			//		System.out.println("Længde: " + avg + ", Størrelse: " + vList.size());
			//		
			//		int x =0;
			//		while(x<vList.size()){
			//			if(vList.get(x).getLength() < avg){
			//				vList.remove(x);
			//				x--;
			//			}
			//			x++;
			//		}
			//		System.out.println("Størrelse2: " + vList.size());
			//		
			//		for(int i=0; i<vList.size(); i++){
			//			Imgproc.line(sOrg, vList.get(i).getX(), vList.get(i).getY(), new Scalar(255,0,0), thickness);
			//		}

			if(BilledAnalyse.BILLED_DEBUG){			
				long total = System.nanoTime() - startTime;
				long durationInMs = TimeUnit.MILLISECONDS.convert(total, TimeUnit.NANOSECONDS);
				String debug = "Vektorer fundet på: " + durationInMs + " milisekunder.";
				debug = debug + " Punkter fundet: " + vList.size() + ", ud af: " + sKey.size();
				System.out.println(debug);	
			}
			return out;
		} else {
			return second;
		}
	}

	/**
	 * Hent den sidst behandlede frame
	 * @return Mat frame
	 */
	public Mat getFrame() {
		return first;
	}
}
