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

public class OpticalFlow {
	
	private BilledManipulation bm;
	private Mat first;
	private MatOfPoint fKey;
	private ArrayList<Vektor> vList;
	
	public OpticalFlow(BilledManipulation bm){
		this.bm = bm;
	}
	
	public ArrayList<Vektor> getVektorArray(){
		return vList;
	}
	
	private Mat trackObject(Mat frame) {
		Mat klon = frame.clone();
		// TODO Auto-generated method stub
		return klon;
	}
	
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

			// Tegn vektorer på kopien af originale farvebillede 
			byte[] fundet = status.toArray();
			Point[] fArray = fKeyf.toArray();
			Point[] sArray = sKeyf.toArray();
			int thickness = 2;
			int antalFundet = 0;
			vList = new ArrayList<Vektor>();
			for(int i=0; i<fArray.length; i++){
				if(fundet[i] == 1){ // Tegn kun der hvor der er fundet matches
					Imgproc.line(out, fArray[i], sArray[i], new Scalar(255,0,0), thickness);
					vList.add(new Vektor(fArray[i],sArray[i]));
					antalFundet++;
				}		
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
				debug = debug + " Punkter fundet: " + antalFundet + ", ud af: " + sKey.size();
				System.out.println(debug);	
			}

			

			//			this.calcOptMagnitude(vList, sOrg, 3); // TEST KODE
			//			out[0] = this.calcDistances(sOrg, vList, 17, 6); // TEST KODE

			return out;
		} else {
			return second;
		}
	}

	public Mat getFrame() {
		return first;
	}

}
