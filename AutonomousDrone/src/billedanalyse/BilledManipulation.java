package billedanalyse;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import diverse.Log;
import diverse.QrFirkant;
import diverse.YawCalc;
import diverse.koordinat.Koordinat;
import drone.IDroneControl;
import javafx.scene.image.Image;

/**
 * Hjælpeklasse der indeholder en række metoder til at manipulere billeder
 *
 */
public class BilledManipulation {

	private IDroneControl dc;

	public int max=255,min=125;
	private MatOfKeyPoint kp;
	private FeatureDetector detect;
	private DescriptorExtractor extractor;
	private Koordinat qrCenter;
	private QrFirkant firkanten, firkanten2;
	private long firkantTime;

	public BilledManipulation(IDroneControl dc){
		this.dc = dc;
		detect = FeatureDetector.create(FeatureDetector.ORB); // Kan være .ORB .FAST eller .HARRIS
		extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
	}

	public MatOfKeyPoint getKP(){
		return kp;
	}

	private void setFirkanten(QrFirkant firkanten){
		firkantTime = System.currentTimeMillis();
		this.firkanten = firkanten;
	}

	public Mat edde(Mat frame){			
		Imgproc.erode(frame, frame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) );
		Imgproc.dilate( frame, frame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) );

		Imgproc.dilate( frame, frame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) );
		Imgproc.erode(frame, frame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) );
		//		int erode_rep = 10;
		//		int dilate_rep = 5;
		//
		//		for(int j = 0;j<dilate_rep;j++){
		//			frame = dilate(frame);
		//		}
		//		for(int i = 0;i<erode_rep;i++){
		//			frame = erode(frame);							
		//		}
		return frame;
	}

	public Mat thresh(Mat frame){
		Mat frame1 = new Mat();
		//		Imgproc.threshold(frame, frame1, 70, 255, Imgproc.THRESH_BINARY);
		Imgproc.threshold(frame, frame1, 20, 255, Imgproc.THRESH_TOZERO);
		return frame1;
	}

	public Mat toGray(Mat frame){
		// convert the image to gray scale
		Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
		return frame;
	}

	public Mat erode(Mat frame_in){
		Mat frame_out = new Mat();
		int erosion_size = 2;
		Point point = new Point( -1, -1 );
		Size size = new Size(erosion_size, erosion_size);
		Mat erodeelement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, size, point);
		Imgproc.erode(frame_in, frame_out, erodeelement);
		return frame_out;
	}

	public Mat dilate(Mat frame_in){
		Mat frame_out = new Mat();
		int dilation_size = 2;
		Point point = new Point( -1, -1 );
		Size size = new Size(dilation_size, dilation_size);
		Mat diluteelement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, size, point);
		Imgproc.dilate(frame_in, frame_out, diluteelement);
		return frame_out;
	}

	public Mat bilat(Mat frame){
		Mat frame1 = new Mat();
		Imgproc.bilateralFilter(frame, frame1, 50, 80.0, 80.0);
		return frame1;
	}

	public Mat gaus(Mat frame){
		Mat frame1 = new Mat();
		Imgproc.GaussianBlur(frame, frame1, new Size(33,33), 10.0);
		return frame1;
	}

	public Mat medianBlur(Mat frame){
		Mat out = new Mat();
		Imgproc.medianBlur(frame, out, 17);
		return out;
	}

	public Mat canny(Mat frame){
		//		
		//		frame = resize(frame, 320, 240);
		Imgproc.Canny(frame, frame, 200.0, 200.0*2, 5, false );
		return frame;
	}

	public Mat eq(Mat frame){
		Imgproc.equalizeHist(frame, frame);
		return frame;		
	}

	public Mat resize(Mat frame,double width, double height){
		Size size = new Size(width, height);
		Imgproc.resize(frame, frame, size);
		return frame;		
	}

	public BufferedImage mat2bufImg(Mat frame){
		//		// create a temporary buffer
		//		MatOfByte buffer = new MatOfByte();
		//		// encode the frame in the buffer
		//		Imgcodecs.imencode(".bmp", frame, buffer);
		//		// build and return an Image created from the image encoded in the
		//		// buffer
		//		return new BufferedImage(frame.width(), frame.height(), java.awt.image.BufferedImage.TYPE_BYTE_INDEXED);

		BufferedImage out;
		byte[] data = new byte[frame.width() * frame.height() * (int)frame.elemSize()];
		int type;
		frame.get(0, 0, data);

		if(frame.channels() == 1)
			type = BufferedImage.TYPE_BYTE_GRAY;
		else
			type = BufferedImage.TYPE_3BYTE_BGR;

		out = new BufferedImage(frame.width(), frame.height(), type);

		out.getRaster().setDataElements(0, 0, frame.width(), frame.height(), data);
		return out;

	}



	public Mat keyPointsImg(Mat frame){
		//		Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
		FeatureDetector detect = FeatureDetector.create(FeatureDetector.ORB);
		kp = new MatOfKeyPoint();
		detect.detect(frame, kp);
		Features2d.drawKeypoints(frame, kp, frame);
		return frame;
	}

	/**
	 * Identificer og tegner linjer i billedet vha. Hough Transform
	 * Se javadoc: http://docs.opencv.org/java/2.4.2/org/opencv/imgproc/Imgproc.html#HoughLines(org.opencv.core.Mat, org.opencv.core.Mat, double, double, int)
	 * @param mat Billedet der skal analyseres
	 * @return Det originale billede, med påtegnede linjer
	 */
	public Mat houghLines(Mat mat){
		// Find linjer i billedet
		double rho = 50; //  Distance resolution of the accumulator in pixels.
		double theta = Math.PI/180; // Angle resolution of the accumulator in radians.
		int threshold = 200; // Accumulator threshold parameter. Only those lines are returned that get enough votes (>threshold).
		Mat lines = new Mat();
		Imgproc.HoughLinesP(mat, lines, rho, theta, threshold);

		if(BilledAnalyse.BILLED_DEBUG){
			System.out.println("Linjer fundet ved HoughLines: " + lines.rows());
		}

		int thickness = 2; // Tykkelse på de tegnede linjer
		Scalar color = new Scalar(255,255,255); // Farven på de tegnede linjer 

		// Tegn alle linjer
		for(int l = 0; l < lines.rows(); l++){
			double[] vec = lines.get(l, 0);
			double x1 = vec[0], 
					y1 = vec[1],
					x2 = vec[2],
					y2 = vec[3];
			Point start = new Point(x1, y1);
			Point end = new Point(x2, y2);
			Imgproc.line(mat, start, end, color, thickness);
		}
		return mat;
	}

	public Mat showColor(Mat frame){
		Mat frame_out = new Mat();
		int iLowH = 160;
		int iHighH = 190;

		int iLowS = 50; 
		int iHighS = 255;

		int iLowV = 0;
		int iHighV = 255;

		Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2HSV);
		Core.inRange(frame, new Scalar(iLowH, iLowS, iLowV), new Scalar(iHighH, iHighS, iHighV), frame_out);
		return frame_out;
	}

	// Identificerer keypoints i et billede
	protected MatOfKeyPoint getKeyPoints(Mat mat){
		MatOfKeyPoint kp = new MatOfKeyPoint();
		detect.detect(mat, kp);
		return kp;
	}

	// Identificer descriptors i et billede
	protected Mat getDescriptors(Mat mat, MatOfKeyPoint kp){
		Mat descriptors = new Mat();
		extractor.compute(mat, kp, descriptors);
		return descriptors;
	}

	/**
	 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
	 * 
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @return the {@link Image} to show
	 */
	protected Image mat2Image(Mat frame){
		// create a temporary buffer
		MatOfByte buffer = new MatOfByte();
		// encode the frame in the buffer
		Imgcodecs.imencode(".bmp", frame, buffer);
		// build and return an Image created from the image encoded in the
		// buffer
		return new Image(new ByteArrayInputStream(buffer.toArray()));
	}

	public Mat bufferedImageToMat(BufferedImage bi) {
		Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
		byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
		mat.put(0, 0, data);
		return mat;
	}

	public Mat readQrSkewed(Mat mat){
		//		this.setFirkanten(null);

		Mat out = new Mat();
		mat.copyTo(out);
		Mat temp = new Mat();
		mat.copyTo(temp);
		temp = toGray(temp);
		Imgproc.GaussianBlur(temp, temp, new Size(5,5), -1);
		Imgproc.Canny(temp, temp, 52, 106);

		//QR TING:
		Mat qr = new Mat();
		qr = Mat.zeros(560, 400, CvType.CV_32S);
		Mat test3 = new Mat(560,400,temp.type());
		ArrayList<QrFirkant> firkant = new ArrayList<QrFirkant>();


		//Contours gemmes i array
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		//Finder Contours

		Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);

		//Løber Contours igennem
		for(int i=0; i<contours.size(); i++){

			//Konverterer MatOfPoint til MatOfPoint2f, så ApproxPoly kan bruges
			MatOfPoint2f mop2 = new MatOfPoint2f();
			contours.get(i).convertTo(mop2, CvType.CV_32FC1); 
			double epsilon = 0.025*Imgproc.arcLength(mop2, true);
			Imgproc.approxPolyDP(mop2, mop2, epsilon, true);
			//Konverterer MatOfPoint2f til MatOfPoint
			mop2.convertTo(contours.get(i), CvType.CV_32S);

			if(contours.get(i).total()==4 && Imgproc.contourArea(contours.get(i))>2000){ //&& Imgproc.contourArea(contours.get(i))>150{
				List<Point> list = new ArrayList<Point>();
				//				Konverterer contours om til en liste af punkter for at finde koordinaterne
				Converters.Mat_to_vector_Point(contours.get(i), list);

				//Contour koordinaterne der danner en firkant
				double x0 = list.get(0).x;
				double x1 = list.get(1).x;
				double x2 = list.get(2).x;
				double x3 = list.get(3).x;
				double y0 = list.get(0).y;
				double y1 = list.get(1).y;
				double y2 = list.get(2).y;
				double y3 = list.get(3).y;

				double l1 = afstand(list.get(0).x,list.get(1).x,list.get(0).y,list.get(1).y);
				double l2 = afstand(list.get(1).x,list.get(2).x,list.get(1).y,list.get(2).y);
				if(checkFirkant(l1,l2)){
					firkant.add(new QrFirkant(new Point(x0,y0),new Point(x1,y1),new Point(x2,y2),new Point(x3,y3)));
					//					System.out.println("Check true ");
					//					Imgproc.putText(out, "0", new Point(list.get(0).x, list.get(0).y), 1, 5, new Scalar(255, 255, 255), 2);
					//					Imgproc.putText(out, "1", new Point(list.get(1).x, list.get(1).y), 1, 5, new Scalar(255, 255, 255), 2);
					//					Imgproc.putText(out, "2", new Point(list.get(2).x, list.get(2).y), 1, 5, new Scalar(255, 255, 255), 2);
					//					Imgproc.putText(out, "3", new Point(list.get(3).x, list.get(3).y), 1, 5, new Scalar(255, 255, 255), 2);
					//					Imgproc.putText(out, "list.get(1)", new Point(list.get(1).x, list.get(1).y), 1, 5, new Scalar(255, 255, 255), 2);
					//					Imgproc.putText(out, Double.toString((int)l1*l2), new Point(list.get(0).x, list.get(0).y), 1, 5, new Scalar(255, 255, 255), 2);
					//										Imgproc.drawContours(out, contours, i, new Scalar(0,0,255), 3);
				}
				//				return test3;
			}
		} // her slutter første for-løkke
		if(firkant.size()!=0){
			int maxA = firkant.get(0).getAreal();
			int id=0;
			for (int i = 1; i < firkant.size(); i++) {
				//bruger qr med største areal
				if(firkant.get(i).getAreal()>maxA){
					maxA = firkant.get(i).getAreal();
					id = i;
				}
			}			
			this.setFirkanten(firkant.get(id));
			qrCenter = firkanten.getCentrum();
			//QR-kode på originalt billede
			Point p0 = firkanten.getPoint0();
			Point p1 = firkanten.getPoint1();
			Point p2 = firkanten.getPoint2();
			Point p3 = firkanten.getPoint3();

			List<Point> qrPunkter = new ArrayList<Point>();
			qrPunkter.add(p0);
			qrPunkter.add(p1);
			qrPunkter.add(p2);
			qrPunkter.add(p3);

			List<Point> qrNyePunkter = new ArrayList<Point>();
			if(firkanten.getPoint0().x>firkanten.getPoint3().x){
				//				System.out.println("X3 er større");
				qrNyePunkter.add(new Point(qr.cols(),0));
				qrNyePunkter.add(new Point(qr.cols(),qr.rows()));
				qrNyePunkter.add(new Point(0,qr.rows()));
				qrNyePunkter.add(new Point(0,0));						
			} else {
				//				System.out.println("X3 er mindre");
				qrNyePunkter.add(new Point(0,0));
				qrNyePunkter.add(new Point(0,qr.rows()));
				qrNyePunkter.add(new Point(qr.cols(),qr.rows()));
				qrNyePunkter.add(new Point(qr.cols(),0));
			}
			//			
			MatOfPoint2f mp = new MatOfPoint2f();
			MatOfPoint2f mp2 = new MatOfPoint2f();
			mp.fromList(qrPunkter);
			mp2.fromList(qrNyePunkter);

			Mat warp = Imgproc.getPerspectiveTransform(mp, mp2);

			Imgproc.warpPerspective(out, test3, warp, new Size(qr.cols(),qr.rows()));
			//Dette må ikke slettes, skal bruges til at justere threshold værdier
			//						Imgproc.cvtColor(test3, test3, Imgproc.COLOR_RGB2GRAY);
			//DETTE SKAL ÆNDRES I FORHOLD TIL LYS-STYRKEN I LOKALET
			//						Imgproc.threshold(test3, test3, min, max, Imgproc.THRESH_BINARY);
			//			System.err.println("Min er "+min + " og max er "+max);
			return test3;
			//			return out;
		} 

		return mat;
	}

	public void readFilterQr(Mat mat){
		//		this.setFirkanten(null);

		Mat out = new Mat();
		mat.copyTo(out);
		Mat temp = new Mat();
		mat.copyTo(temp);
		temp = toGray(temp);
		Imgproc.GaussianBlur(temp, temp, new Size(5,5), -1);
		Imgproc.Canny(temp, temp, 50, 100);

		//QR TING:
		Mat qr = new Mat();
		qr = Mat.zeros(560, 400, CvType.CV_32S);
		Mat test3 = new Mat(560,400,temp.type());
		ArrayList<QrFirkant> firkant = new ArrayList<QrFirkant>();


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

			if(contours.get(i).total()==4 && Imgproc.contourArea(contours.get(i))>2000){ //&& Imgproc.contourArea(contours.get(i))>150{
				List<Point> list = new ArrayList<Point>();
				//				Konverterer contours om til en liste af punkter for at finde koordinaterne
				Converters.Mat_to_vector_Point(contours.get(i), list);

				//Contour koordinaterne der danner en firkant
				double x0 = list.get(0).x;
				double x1 = list.get(1).x;
				double x2 = list.get(2).x;
				double x3 = list.get(3).x;
				double y0 = list.get(0).y;
				double y1 = list.get(1).y;
				double y2 = list.get(2).y;
				double y3 = list.get(3).y;

				double l1 = afstand(list.get(0).x,list.get(1).x,list.get(0).y,list.get(1).y);
				double l2 = afstand(list.get(1).x,list.get(2).x,list.get(1).y,list.get(2).y);
				if(checkFirkant(l1,l2)){
					firkant.add(new QrFirkant(new Point(x0,y0),new Point(x1,y1),new Point(x2,y2),new Point(x3,y3)));
				}
			}
		} // her slutter første for-løkke
		if(firkant.size()!=0){
			int maxA = firkant.get(0).getAreal();
			int id=0;
			for (int i = 1; i < firkant.size(); i++) {
				//bruger qr med største areal
				if(firkant.get(i).getAreal()>maxA){
					maxA = firkant.get(i).getAreal();
					id = i;
				}
			}			
			this.setFirkanten(firkant.get(id));
			qrCenter = firkanten.getCentrum();
		} 
	}

	//metode der finder firkanter og QR kode i forbindelse med at finde dronens position i forhold til 2 qr koder
	public ArrayList<QrFirkant> dronePos2(Mat mat){
		//		this.setFirkanten(null);
		firkanten2 = null;
		Mat out = new Mat();
		mat.copyTo(out);
		Mat out2 = new Mat();
		mat.copyTo(out2);
		Mat temp = new Mat();
		mat.copyTo(temp);

		//Qr ting
		//			Mat qr = new Mat();
		//			qr = Mat.zeros(560, 400, CvType.CV_32S);
		//			Mat test3 = new Mat(560,400,temp.type());

		temp = toGray(temp);
		Imgproc.GaussianBlur(temp, temp, new Size(5,5), -1);
		Imgproc.Canny(temp, temp, 52, 106);

		ArrayList<QrFirkant> qrFirkantAll = new ArrayList<QrFirkant>();
		QrFirkant firkant1, firkant2;

		//Contours gemmes i array
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		//Finder Contours
		Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);

		for(int i=0; i<contours.size(); i++){
			//Konverterer MatOfPoint til MatOfPoint2f, så ApproxPoly kan bruges
			MatOfPoint2f mop2 = new MatOfPoint2f();
			contours.get(i).convertTo(mop2, CvType.CV_32FC1); 
			double epsilon = 0.025*Imgproc.arcLength(mop2, true);
			Imgproc.approxPolyDP(mop2, mop2, epsilon, true);
			//Konverterer MatOfPoint2f til MatOfPoint
			mop2.convertTo(contours.get(i), CvType.CV_32S);

			if(contours.get(i).total()==4 && Imgproc.contourArea(contours.get(i))>2000){
				List<Point> list = new ArrayList<Point>();
				//				Konverterer contours om til en liste af punkter for at finde koordinaterne
				Converters.Mat_to_vector_Point(contours.get(i), list);
				double l1 = afstand(list.get(0).x,list.get(1).x,list.get(0).y,list.get(1).y);
				double l2 = afstand(list.get(1).x,list.get(2).x,list.get(1).y,list.get(2).y);

				//Contour koordinaterne der danner en firkant
				double x0 = list.get(0).x;
				double x1 = list.get(1).x;
				double x2 = list.get(2).x;
				double x3 = list.get(3).x;
				double y0 = list.get(0).y;
				double y1 = list.get(1).y;
				double y2 = list.get(2).y;
				double y3 = list.get(3).y;

				if(checkFirkant(l1,l2)){
					//					Imgproc.drawContours(out, contours, i, new Scalar(0,0,255), 3);
					//					QrFirkant qr = new QrFirkant(new Point(x0,y0),new Point(x1,y1),new Point(x2,y2),new Point(x3,y3));
					qrFirkantAll.add(new QrFirkant(new Point(x0,y0),new Point(x1,y1),new Point(x2,y2),new Point(x3,y3)));

				}
			}
		}

		ArrayList<QrFirkant> qrFirkant2 = new ArrayList<QrFirkant>();
		ArrayList<QrFirkant> qrFirkant = new ArrayList<QrFirkant>();
		if(qrFirkantAll.size()>=2){

			Map<Koordinat, ArrayList<QrFirkant>> gruppering = new HashMap<Koordinat,ArrayList<QrFirkant>>();
			ArrayList<Koordinat> kontrol = new ArrayList<Koordinat>();
			for (int i = 0; i < qrFirkantAll.size(); i++) {
				Koordinat key = qrFirkantAll.get(i).getCentrum();
				if(gruppering.isEmpty()){
					gruppering.put(key, new ArrayList<QrFirkant>());
					gruppering.get(key).add(qrFirkantAll.get(i));
					kontrol.add(key);
				} else {

					//					for (Koordinat key2 : gruppering.keySet()) {
					for (int j = 0; j < gruppering.size(); j++) {
						Koordinat key2 = kontrol.get(j);
						if(!checkCentrum2(key2,key)){
							gruppering.get(key2).add(qrFirkantAll.get(i));
							//							Log.writeLog("Firkant i samme gruppering");
						} else {
							gruppering.put(key, new ArrayList<QrFirkant>());
							gruppering.get(key).add(qrFirkantAll.get(i));
							//							Log.writeLog("Firkant ikke i samme gruppering");
							kontrol.add(key);
						}						
					}
					//					}
				}
			}

			for (Koordinat key : gruppering.keySet()) {
				//				Log.writeLog("Key: "+key);
				ArrayList<QrFirkant> test = gruppering.get(key);
				int minA = test.get(0).getAreal();
				int id=0;
				for(int i=0; i<test.size();i++){
					if(test.get(i).getAreal()<=minA){
						minA = test.get(i).getAreal();
						id = i;
					}
				}
				qrFirkant.add(test.get(id));
			}

			//			Log.writeLog("MAPPET STØRRELSE "+gruppering.size());

		} else if (!qrFirkantAll.isEmpty()){
			setFirkanten(qrFirkantAll.get(0));
		}

		//			Log.writeLog("over 2 grupperinger");
		int maxA = 0;
		int nextMaxA = 0;
		int id=0;
		int id2=0;
		//Finder største areal
		for (int i = 0; i < qrFirkant.size(); i++) {
			//				System.err.println("Areal størrelser : "+ qrFirkant.get(i).getAreal());
			//bruger qr med største areal
			//				if(qrFirkant.get(i).getCentrum().getX()>320 && qrFirkant.get(i).getCentrum().getX()<960){ // denne if-else skal måske fjernes
			if(qrFirkant.get(i).getAreal()>=maxA){
				//						System.err.println("maxA");
				maxA = qrFirkant.get(i).getAreal();
				id = i;
			}									
			//				}
		}
		if(qrFirkant.isEmpty()){
			return null;
		}
		firkant1 = qrFirkant.get(id);
		setFirkanten(firkant1);
		if(qrFirkant.size()>=2){
			//Finder næst største areal
			for (int i = 0; i < qrFirkant.size(); i++) {
				if(qrFirkant.get(i).getAreal()<=maxA && qrFirkant.get(i).getAreal()>=nextMaxA){
					if(checkCentrum(qrFirkant.get(id), qrFirkant.get(i))){
						//						System.err.println("nextMaxA");
						nextMaxA = qrFirkant.get(i).getAreal();
						id2 = i;						
					}
				}
			}

			firkant2 = qrFirkant.get(id2);

			qrFirkant2.add(firkant1);
			qrFirkant2.add(firkant2);

			if(qrFirkant2.size()!=2){// || !checkAreal(firkant1,firkant2) || !checkCentrum(firkant1,firkant2)
				//				System.err.println("fejl 1");
				Log.writeLog("FEJL 1");
				return null;
			}
			//			if(!checkAreal(firkant1,firkant2)){
			////				System.err.println("fejl 2");
			//				Log.writeLog("FEJL 2");
			//				return null;
			//			}
			if(!checkCentrum(firkant1,firkant2)){
				Log.writeLog("FEJL 3");
				//				System.err.println("fejl 3");
				//				System.err.println("Firkant 1 = " + firkant1.getCentrum().getX());
				//				System.err.println("Firkant 2 = " + firkant2.getCentrum().getX());
				return null;
			}
			return qrFirkant2;
		}
		return null;
	}
	//Denne metode er måske ligegyldig
	public String warpQrImage(QrFirkant firkant1, QRCodeScanner qrs, Mat mat){
		Mat qr = new Mat();
		qr = Mat.zeros(560, 400, CvType.CV_32S);
		Mat test3 = new Mat(560,400,mat.type());
		Point p0 = firkant1.getPoint0();
		Point p1 = firkant1.getPoint1();
		Point p2 = firkant1.getPoint2();
		Point p3 = firkant1.getPoint3();

		List<Point> qrPunkter = new ArrayList<Point>();
		qrPunkter.add(p0);
		qrPunkter.add(p1);
		qrPunkter.add(p2);
		qrPunkter.add(p3);

		List<Point> qrNyePunkter = new ArrayList<Point>();
		if(firkant1.getPoint0().x>firkant1.getPoint3().x){
			qrNyePunkter.add(new Point(qr.cols(),0));
			qrNyePunkter.add(new Point(qr.cols(),qr.rows()));
			qrNyePunkter.add(new Point(0,qr.rows()));
			qrNyePunkter.add(new Point(0,0));						
		} else {
			qrNyePunkter.add(new Point(0,0));
			qrNyePunkter.add(new Point(0,qr.rows()));
			qrNyePunkter.add(new Point(qr.cols(),qr.rows()));
			qrNyePunkter.add(new Point(qr.cols(),0));
		}
		MatOfPoint2f mp = new MatOfPoint2f();
		MatOfPoint2f mp2 = new MatOfPoint2f();
		mp.fromList(qrPunkter);
		mp2.fromList(qrNyePunkter);

		Mat warp = Imgproc.getPerspectiveTransform(mp, mp2);
		Imgproc.warpPerspective(mat, test3, warp, new Size(qr.cols(),qr.rows()));

		return qrs.applyFilters(test3);
	}

	private double afstand(double x1, double x2, double y1, double y2){
		double result = Math.sqrt(Math.pow((x2-x1),2)+Math.pow((y2-y1),2));
		return result;
	}

	private boolean checkAreal(QrFirkant f1, QrFirkant f2){
		int f1a = f1.getAreal();
		int f2a = f2.getAreal();
		int ratio = (int) (f1a*0.75);
		if(f2a > ratio){
			return true;
		} else {
			return false;
		}
	}

	private boolean checkCentrum(QrFirkant f1, QrFirkant f2){
		Koordinat f1k = f1.getCentrum();
		Koordinat f2k = f2.getCentrum();
		int dist = f1k.dist(f2k);
		if(dist>100){
			return true;
		} else {
			return false;
		}
	}

	private boolean checkCentrum2(Koordinat f1, Koordinat f2){
		//		Koordinat f1k = f1.getCentrum();
		//		Koordinat f2k = f2.getCentrum();
		int dist = f1.dist(f2);
		if(dist>100){
			return true;
		} else {
			return false;
		}
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

	public Koordinat getQrCenter(){
		return qrCenter;
	}

	public QrFirkant getFirkanten() {
		if((System.currentTimeMillis() - firkantTime) > 1000){
			return null;
		}
		return firkanten;
	}

	public QrFirkant getFirkanten2() {
		return firkanten2;
	}

	//	public int getMax(){
	//		return max;
	//	}
	public void setMax(int val){

		max = val;
	}

	//	public int getMin(){
	//		return min;
	//	}
	public void setMin(int val){
		min = val;
	}

}
