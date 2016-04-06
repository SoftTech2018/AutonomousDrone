package billedanalyse;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;

import drone.DroneControl;
import drone.IDroneControl;
import javafx.scene.image.Image;

public class BilledAnalyse implements IBilledAnalyse, Runnable {

	/*
	 * Definerer DEBUG-mode for billedmodulet (der udskrives til konsollen).
	 */
	protected static final boolean BILLED_DEBUG = true;

	private BilledManipulation bm;
	private OpticalFlow opFlow;
	private DescriptorMatcher matcher;
	private IDroneControl dc;

	private Mat objectImage;
	private Mat objectDescriptors;
	private MatOfKeyPoint fKey;
	private Image[] imageToShow;
	private Mat[] frames;
	private boolean objTrack, greyScale, qr, webcam = true, opticalFlow;
	private Mat webcamFrame;
	private Mat matFrame;
	private QRCodeScanner qrs = new QRCodeScanner();

	public BilledAnalyse(IDroneControl dc){
		this.dc = dc;
		this.bm = new BilledManipulation();
		imageToShow = new Image[3];
		frames = new Mat[3];
		this.opFlow = new OpticalFlow(bm);
		this.matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
	}

	@Override
	public void setQR(boolean qr){
		this.qr = qr;
	}

	/* (non-Javadoc)
	 * @see billedanalyse.IBilledAnalyse#calcDistances(org.opencv.core.Mat, double, int)
	 */
	@Override
	public Mat calcDistances(Mat distFrame, double degree, int size){
		ArrayList<Vektor> vectors;
		if((vectors = opFlow.getVektorArray())==null){
			return distFrame;
		}

		double sqWidth = distFrame.size().width/size;
		double sqHeight = distFrame.size().height/size;

		double squares[][] = new double[size][size];
		int squaresCount[][] = new int[size][size];

		for(int i=0; i<vectors.size();i++){	
			//find hvilken firkant vektoren hører til
			double pointX = vectors.get(i).getY().x;
			double pointY = vectors.get(i).getY().y;
			// Hvis vi er ude over billedets grænser springes til næste punkt
			if(pointY > sqHeight*size || pointX > sqWidth*size){
				continue;
			}
			int x = (int) (pointX/sqWidth);
			int y = (int) (pointY/sqHeight);
			// Beregn distance og adder distancen i den tilsvarende firkant
			squares[x][y] += vectors.get(i).distance(degree);
			squaresCount[x][y]++;
		}

		double minDist = 30; // Den mindste tilladte distance til et givent objekt
		Scalar color = new Scalar(0,0,255); // Rød farve til stregen
		int thickness = 4; // Tykkelse på stregen

		// Beregn gennemsnitsdistancen i hver firkant
		for(int i=0; i<size; i++){
			for(int o=0; o<size; o++){
				if(squaresCount[i][o] > 0){ // Beregn kun gennemsnitsdistance hvis der er noget at beregne på
					squares[i][o] = squares[i][o] / squaresCount[i][o];		
					// Tegn firkant hvis objektet er for tæt på
					if(squares[i][o] > minDist){
						Imgproc.rectangle(distFrame, new Point(sqWidth*i, sqHeight*o), new Point(sqWidth*(i+1), sqHeight*(o+1)), color, thickness);
						//						System.err.println("Forhindring fundet i sektor " + i + ":" + o +", dist: " + squares[i][o]);
					}
				}
				//				if(BILLED_DEBUG){
				//					System.out.println("Distance for " + i + "," + o + " : " + squares[i][o] + ", antal: " + squaresCount[i][o]);					
				//				}
			}
		}
		return distFrame;
	}

	/* (non-Javadoc)
	 * @see billedanalyse.IBilledAnalyse#calcOptMagnitude(int)
	 */
	@Override
	public double[][] calcOptMagnitude(int size){
		double out[][] = new double[size][size];
		ArrayList<Vektor> vectors;
		Mat frame;

		// Hvis der ikke er kørt Optical Flow endnu returnerer vi høje tal der er større end threshold.
		if((vectors = opFlow.getVektorArray())==null || (frame = opFlow.getFrame())==null){
			for(int i=0; i<size; i++){
				for(int o=0; o<size; o++){
					out[i][o] = Integer.MAX_VALUE;
				}
			}
			return out;
		}

		if(this.webcam){
			frame=this.webcamFrame;				
		}

		double sqWidth = frame.size().width/size;
		double sqHeight = frame.size().height/size;

		int squaresCount[][] = new int[size][size];

		for(int i=0; i<vectors.size();i++){			
			//find hvilken firkant vektoren hører til
			double pointX = vectors.get(i).getY().x;
			double pointY = vectors.get(i).getY().y;
			// Hvis vi er ude over billedets grænser springes til næste punkt
			if(pointY > sqHeight*size || pointX > sqWidth*size){
				continue;
			} 
			int x = (int) (pointX/sqWidth);
			int y = (int) (pointY/sqHeight);

			// Adder længden i den tilsvarende firkant
			out[x][y] += ((double) vectors.get(i).getLength());
			squaresCount[x][y]++;
		}

		// Beregn gennemsnitsmagnituden i hver firkant
		for(int i=0; i<size; i++){
			for(int o=0; o<size; o++){
				if(squaresCount[i][o] > 0){ // Beregn kun gennemsnitsdistance hvis der er noget at beregne på
					out[i][o] = (double) out[i][o] / (double) squaresCount[i][o];
					//					if(out[i][o] > 20){						
					//						Imgproc.rectangle(frame, new Point(sqWidth*i, sqHeight*o), new Point(sqWidth*(i+1), sqHeight*(o+1)), new Scalar(0,0,255), 4);
					//					}
				} else { // Hvis der ikke spores noget i en kvadrant må vi gå ud fra vi ikke kan flyve den vej
					out[i][o] = Double.MAX_VALUE;
				}
			}
		}

		// Tegn røde og grønne firkanter der symboliserer mulige manøvre
		if(this.webcam){
			double hStep = 480/size;
			double vStep = 640/size;
			int threshold = 10; // Bestemmer hvor stor bevægelse der må være i et kvadrant før det markeres som rødt
			Scalar red = new Scalar(0,0,255); // Rød farve til stregen
			Scalar green = new Scalar(0,255,0); // Grøn farve 
			int thickness = 2; // Tykkelse på stregen
			for(int i=0; i<size; i++){
				for(int o=0; o<size; o++){
					// Tegn rød firkant hvis objektet er for tæt på, ellers tegn grøn firkant
					if(out[i][o] >= threshold){
						Imgproc.rectangle(frame, new Point(vStep*i, hStep*o), new Point(vStep*(i+1)-2, hStep*(o+1)-2), red, thickness);
					} else {
						Imgproc.rectangle(frame, new Point(vStep*i, hStep*o), new Point(vStep*(i+1)-2, hStep*(o+1)-2), green, thickness);
					}
				}
			}
		}

		if(BILLED_DEBUG){
			System.out.println("***");
			System.out.println(out[0][0] + "\t" + out[1][0] + "\t" + out[2][0]);
			System.out.println(out[0][1] + "\t" + out[1][1] + "\t" + out[2][1]);
			System.out.println(out[0][2] + "\t" + out[1][2] + "\t" + out[2][2]);
		}
		return out;
	}

	/* (non-Javadoc)
	 * @see billedanalyse.IBilledAnalyse#drawMatches(org.opencv.core.Mat, org.opencv.core.Mat)
	 */
	@Override
	public Mat drawMatches(Mat first, Mat second){
		// DEBUG
		long startTime = System.nanoTime();

		MatOfKeyPoint fKey = bm.getKeyPoints(first);
		MatOfKeyPoint sKey = bm.getKeyPoints(second);		
		Mat f = bm.getDescriptors(first, fKey);
		Mat s = bm.getDescriptors(second, sKey);

		DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
		MatOfDMatch dmatches = new MatOfDMatch();
		matcher.match(f, s, dmatches);
		Mat out = new Mat();
		Features2d.drawMatches(first, fKey, second, sKey, dmatches, out);
		//		Features2d.drawMatches(img1, keypoints1, img2, keypoints2, matches1to2, outImg, matchColor, singlePointColor, matchesMask, flags);

		if(BILLED_DEBUG){
			long total = System.nanoTime() - startTime;
			long durationInMs = TimeUnit.MILLISECONDS.convert(total, TimeUnit.NANOSECONDS);
			String debug = "Matches fundet på: " + durationInMs + " milisekunder";
			System.out.println(debug);	
		}

		return out;
	}

	/* (non-Javadoc)
	 * @see billedanalyse.IBilledAnalyse#getKP()
	 */
	@Override
	public MatOfKeyPoint getKP(){
		return bm.getKP();
	}

	/* (non-Javadoc)
	 * @see billedanalyse.IBilledAnalyse#qrread(org.opencv.core.Mat)
	 */
	@Override
	public void qrread(Mat frame){	
		BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
				new BufferedImageLuminanceSource(bm.mat2bufImg(frame))));
		Reader reader = new QRCodeMultiReader();

		try {							
			Result qrout = reader.decode(binaryBitmap);
			System.out.println(qrout.getText());
			System.out.println("HIT");
		} catch (NotFoundException e) {

		} catch (ChecksumException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see billedanalyse.IBilledAnalyse#bufferedImageToMat(java.awt.image.BufferedImage)
	 */
	@Override
	public Mat bufferedImageToMat(BufferedImage bi) {
		Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
		byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
		mat.put(0, 0, data);
		return mat;
	}

	@Override
	public Mat getMatFrame(){
		return matFrame;
	}

	/* (non-Javadoc)
	 * @see billedanalyse.IBilledAnalyse#resize(org.opencv.core.Mat, int, int)
	 */
	public Mat resize(Mat frame, int i, int j) {
		return bm.resize(frame, i, j);
	}

	/* (non-Javadoc)
	 * @see billedanalyse.IBilledAnalyse#optFlow(org.opencv.core.Mat, boolean, boolean)
	 */
	public Mat[] optFlow(Mat frame, boolean optFlow, boolean objTrack) {
		return null; //opFlow.optFlow(frame, optFlow, objTrack);
	}


	/* (non-Javadoc)
	 * @see billedanalyse.IBilledAnalyse#getVektorArray()
	 */
	@Override
	public ArrayList<Vektor> getVektorArray() {
		return opFlow.getVektorArray();
	}

	private Mat trackObject(){
		Mat out = new Mat();
		this.opFlow.getFrame().copyTo(out);;
		try {
			if(objectImage==null){
				try {
					BufferedImage img = ImageIO.read(new File(".\\test.png"));
					objectImage = bufferedImageToMat(img);
					//					first = bm.gaus(first);
					objectImage = bm.edde(objectImage);
					objectImage = bm.thresh(objectImage);
					objectImage = bm.toGray(objectImage);
					objectImage = bm.medianBlur(objectImage);
					objectImage = bm.canny(objectImage);
					//										objectImage = bm.eq(objectImage);
					objectImage = bm.houghLines(objectImage);
					//					first = bm.filterMat(first);
					//													first = bm.houghLines(first);
					//									first = bm.canny(first);

					fKey = bm.getKeyPoints(objectImage);
					objectDescriptors = bm.getDescriptors(objectImage, fKey);
				} catch (IOException e) {
					e.printStackTrace();
					return out;
				}
			} 
			//				first.copyTo(out); // Billede af object der trackes vises hvis der ikke er nok matches.
			//						for(int i =0; i < fKey.toList().size(); i++){	
			//							Point p = fKey.toList().get(i).pt;
			//							Imgproc.circle(out, p, 4, new Scalar(255,255,255));
			//						}
			//			System.err.println(fKey.toList().size());
			//
			//			if(true)
			//				return out;


			long startTime = System.nanoTime();
			//			Mat image32S = new Mat();
			//out.convertTo(image32S, CvType.CV_32SC1);
			//			List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
			//			Imgproc.findContours(image32S, contours, new Mat(), Imgproc.RETR_FLOODFILL, Imgproc.CHAIN_APPROX_SIMPLE);
			//			for (int i = 0; i < contours.size(); i++) {
			//			    Imgproc.drawContours(out, contours, 0, new Scalar(255, 255, 255), 10);
			//			}
			//			if(true)
			//				return out;
			//			out = bm.toGray(out);
			out = bm.medianBlur(out);
			//			out = bm.thresh(out);
			//			out = bm.gaus(out);
			//			out = bm.edde(out);
			out = bm.canny(out);
			//						out = bm.eq(out);
			out = bm.houghLines(out);

			MatOfKeyPoint sKey = bm.getKeyPoints(out);		
			Mat s = bm.getDescriptors(out, sKey);
			if(s.empty()){
				return out;
			}
			//			System.out.println(s.size().toString());

			//			MatOfDMatch dmatches = new MatOfDMatch();
			//			matcher.match(f, s, dmatches);
			List<MatOfDMatch> matchesList = new ArrayList<MatOfDMatch>();
			matcher.knnMatch(objectDescriptors, s, matchesList, 2);

			// ratio test
			LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
			for (Iterator<MatOfDMatch> iterator = matchesList.iterator(); iterator.hasNext();) {
				MatOfDMatch matOfDMatch = (MatOfDMatch) iterator.next();
				if (matOfDMatch.toArray()[0].distance / matOfDMatch.toArray()[1].distance < 0.9) {
					//				if (matOfDMatch.toArray()[0].distance < 0.7*matOfDMatch.toArray()[1].distance) {
					good_matches.add(matOfDMatch.toArray()[0]);	            	
				}
			}
			if(BilledAnalyse.BILLED_DEBUG){				
				System.err.println("Antal good_matches: " + good_matches.toArray().length);
			}
			if(good_matches.toArray().length < 50){
				return out;
			}

			// get keypoint coordinates of good matches to find homography and remove outliers using ransac
			List<Point> pts1 = new ArrayList<Point>();
			List<Point> pts2 = new ArrayList<Point>();
			for(int i = 0; i<good_matches.size(); i++){
				pts1.add(fKey.toList().get(good_matches.get(i).queryIdx).pt);
				pts2.add(sKey.toList().get(good_matches.get(i).trainIdx).pt);
			}

			// convertion of data types - there is maybe a more beautiful way
			Mat outputMask = new Mat();
			MatOfPoint2f pts1Mat = new MatOfPoint2f();
			pts1Mat.fromList(pts1);
			MatOfPoint2f pts2Mat = new MatOfPoint2f();
			pts2Mat.fromList(pts2);

			// Find homography - here just used to perform match filtering with RANSAC, but could be used to e.g. stitch images
			// the smaller the allowed reprojection error (here 15), the more matches are filtered 
			Mat Homog = Calib3d.findHomography(pts1Mat, pts2Mat, Calib3d.RANSAC, 100, outputMask, 2000, 0.995);

			// outputMask contains zeros and ones indicating which matches are filtered
			LinkedList<DMatch> better_matches = new LinkedList<DMatch>();
			for (int i = 0; i < good_matches.size(); i++) {
				if (outputMask.get(i, 0)[0] != 0.0) {
					better_matches.add(good_matches.get(i));
				}
			}

			MatOfDMatch better_matches_mat = new MatOfDMatch();
			better_matches_mat.fromList(better_matches);
			List<DMatch> best_matches = better_matches_mat.toList();

			if(BilledAnalyse.BILLED_DEBUG){
				System.err.println("Antal best_matches: " + best_matches.size());				
			}

			if(best_matches.isEmpty() || best_matches == null){
				return out;
			}

			//	    Features2d.drawMatches(first, fKey, second, sKey, better_matches_mat, out);

			// Beregn center af de bedste matches, og tegn en firkant rundt om objektet
			double centroidX = 0, centroidY = 0;
			// Tjek om der er fundet en passende mængde gode matches
			double score = (double) best_matches.size() / good_matches.toArray().length;

			if(BilledAnalyse.BILLED_DEBUG){
				System.out.printf("Matchet %.2f procent\n", score*100);
			}

			if(score > 0.3){
				for(int i =0; i < best_matches.size(); i++){			
					Point pt2 = sKey.toList().get(best_matches.get(i).trainIdx).pt;
					centroidX += pt2.x;
					centroidY += pt2.y;
					Imgproc.circle(out, pt2, 2, new Scalar(0,255,0)); // TODO erstat evt frame med out
				}
				Point p1 = new Point(centroidX/best_matches.size()-50, centroidY/best_matches.size()-50);
				Point p2 = new Point(centroidX/best_matches.size()+50, centroidY/best_matches.size()+50);
				Imgproc.rectangle(out, p1, p2, new Scalar(255,0,0), 5); // TODO erstat evt frame med out
			} 

			if(BilledAnalyse.BILLED_DEBUG){
				long total = System.nanoTime() - startTime;
				long durationInMs = TimeUnit.MILLISECONDS.convert(total, TimeUnit.NANOSECONDS);
				String debug = "Object tracket på: " + durationInMs + " milisekunder";
				System.out.println(debug);	
			}
		} catch (Exception e){
			return out;
		}

		return out;
	}

	@Override
	public void setObjTrack(boolean objTrack){
		this.objTrack = objTrack;
	}

	@Override
	public void setGreyScale(boolean greyScale){
		this.greyScale = greyScale;
	}

	@Override
	public void setWebCam(boolean webcam){
		this.webcam = webcam;
	}

	@Override
	public void setOpticalFlow(boolean opticalFlow){
		this.opticalFlow = opticalFlow;
	}

	@Override
	public Image[] getImages(){
		return this.imageToShow;
	}

	@Override
	public void run() {
		System.err.println("*** BilledAnalyse starter.");
		Mat img;
		boolean interrupted = false;
		while(!Thread.interrupted() || interrupted){
			Long startTime = System.currentTimeMillis();
			try {
				if(webcam){	
					if(webcamFrame==null){		
						continue;
					}
					img = this.webcamFrame;
				} else {
					img = this.bufferedImageToMat(dc.getbufImg());				
				}
				img = resize(img, 640, 480);
				matFrame = img;
				frames[0] = img;
				if(opticalFlow || objTrack){ // opticalFlow boolean
					frames[1] = this.opFlow.optFlow(img, true);
				}
				if(objTrack){
					frames[2] = this.trackObject();
				} 
				this.calcOptMagnitude(3);
				//			outFrame[0] = ba.trackObject(frame);
				//			} else {
				//				outFrame[0] = frame;						
				//			}

				//			if(objTrack){
				//				outFrame[2] = ph.trackObject(frame);
				//			} 

				// Enable image filter?
				if(greyScale){						
					frames[0] = bm.filterMat(frames[0]);
				}

				//Enable QR-checkBox?
				if(qr){
					findQR(matFrame);
				}

				// convert the Mat object (OpenCV) to Image (JavaFX)
				for(int i=0; i<frames.length;i++){
					if(frames[i] != null){
						imageToShow[i] = this.mat2Image(frames[i]);
					}
				}
			} catch (NullPointerException e){
				System.err.println("Intet billede modtaget til billedanalyse. Prøver igen om 50 ms.");
				try {
					// Intet billede modtaget. Vent 50 ms og tjek igen.
					Thread.sleep(50);
				} catch (InterruptedException e1) {
					interrupted = true;
				}
			}
			if(BilledAnalyse.BILLED_DEBUG){
				System.out.println("BilledAnalyse færdig på: " + (System.currentTimeMillis() - startTime) + " ms.");				
			}
		}
		System.err.println("*** BilledAnalyse stopper.");
	}

	public void findQR(Mat frame){
		qrs.imageUpdated(frame);
		//		qr_label.setText("hej");
	}

	/**
	 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
	 * 
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @return the {@link Image} to show
	 */
	public Image mat2Image(Mat frame){
		// create a temporary buffer
		MatOfByte buffer = new MatOfByte();
		// encode the frame in the buffer
		Imgcodecs.imencode(".png", frame, buffer);
		// build and return an Image created from the image encoded in the
		// buffer
		return new Image(new ByteArrayInputStream(buffer.toArray()));
	}

	@Override
	public void setImg(Mat frame) {
		this.webcamFrame = frame;
	}

	@Override 
	public void setImage(Mat frame){
		this.imageToShow[0] = this.mat2Image(frame);
	}
	
	public String getQrt(){
		 		return qrs.getQrt();
		 	}
}
