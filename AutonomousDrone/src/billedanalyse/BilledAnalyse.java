package billedanalyse;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;

public class BilledAnalyse {

	private MatOfKeyPoint kp;
	private Mat first;
	private ArrayList<Vektor> vList;
	private MatOfPoint fKey;

	/*
	 * Definerer DEBUG-mode for billedmodulet (der udskrives til konsollen).
	 */
	protected static final boolean BILLED_DEBUG = false;

	public Mat KeyPointsImg(Mat frame){
		//		Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
		FeatureDetector detect = FeatureDetector.create(FeatureDetector.ORB);
		kp = new MatOfKeyPoint();
		detect.detect(frame, kp);
		Features2d.drawKeypoints(frame, kp, frame);
		return frame;
	}

	private Mat buffImgToMat(BufferedImage in){
		Mat out;
		byte[] data;
		int r, g, b;

		if(in.getType() == BufferedImage.TYPE_INT_RGB)
		{
			out = new Mat(240, 320, CvType.CV_8UC1);
			data = new byte[320 * 240 * (int)out.elemSize()];
			int[] dataBuff = in.getRGB(0, 0, 320, 240, null, 0, 320);
			for(int i = 0; i < dataBuff.length; i++)
			{
				data[i*3] = (byte) ((dataBuff[i] >> 16) & 0xFF);
				data[i*3 + 1] = (byte) ((dataBuff[i] >> 8) & 0xFF);
				data[i*3 + 2] = (byte) ((dataBuff[i] >> 0) & 0xFF);
			}
		}
		else
		{
			out = new Mat(240, 320, CvType.CV_8UC1);
			data = new byte[320 * 240 * (int)out.elemSize()];
			int[] dataBuff = in.getRGB(0, 0, 320, 240, null, 0, 320);
			for(int i = 0; i < dataBuff.length; i++)
			{
				r = (byte) ((dataBuff[i] >> 16) & 0xFF);
				g = (byte) ((dataBuff[i] >> 8) & 0xFF);
				b = (byte) ((dataBuff[i] >> 0) & 0xFF);
				data[i] = (byte)((0.21 * r) + (0.71 * g) + (0.07 * b)); //luminosity
			}
		}
		out.put(0, 0, data);
		return out;
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

		if(BILLED_DEBUG){
			System.out.println("Linjer fundet ved HoughLines: " + lines.rows());
		}

		int thickness = 3; // Tykkelse på de tegnede linjer
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

	/**
	 * Udfører Optical Flow analyse mellem to frames og tegner resultatet på det returnede frame
	 * @param first Første frame
	 * @param second Anden frame - denne frame returnes med resultatet
	 * @return Mat second - påtegnet resultatet
	 */
	public Mat[] optFlow(Mat second, boolean optFlow, boolean objTrack){
		Mat out[] = new Mat[3];

		if(objTrack){
			out[2] = this.trackObject(second);
		}

		if(optFlow){
			// Første gang metoden kaldes gemmes billedet og der tegnes ingen vektorer.
			if(first==null){
				first = second;
				fKey = new MatOfPoint();

				//			first = this.gaus(first);
				first = this.edde(first);
				first = this.thresh(first);
				//			first = this.canny(first);
				first = this.toGray(first);

				Imgproc.goodFeaturesToTrack(first, fKey, 400, 0.01, 10);

				out[0] = second;
				out[1] = first;
				return out;
			}

			long startTime = System.nanoTime(); // DEBUG

			// Gem en kopi af det orignale farvebillede der skal vises til sidst
			Mat sOrg = second.clone();

			// Initier variable der gemmes data i
			MatOfPoint sKey = new MatOfPoint();

			// Behandling af billedet som fremhæver features
			//		second = this.gaus(second);
			second = this.edde(second);
			second = this.thresh(second);
			//		second = this.canny(second);
			second = this.toGray(second);

			// Find punkter der er gode at tracke. Gemmes i fKey og sKey
			Imgproc.goodFeaturesToTrack(second, sKey, 400, 0.01, 10);

			// Hvis der ikke findes nogle features er der intet at tegne eller lave optical flow på
			if(sKey.empty()){
				System.err.println("******** NUL FEATURES FUNDET! ************** ");
				out[0] = second;
				return out;
			}

			// Kør opticalFlowPyrLK
			MatOfPoint2f sKeyf = new MatOfPoint2f(sKey.toArray());
			MatOfPoint2f fKeyf = new MatOfPoint2f(fKey.toArray());
			MatOfByte status = new MatOfByte();
			MatOfFloat err = new MatOfFloat();
			Video.calcOpticalFlowPyrLK(first, second, fKeyf, sKeyf, status, err );

			// Tegn vektorer på kopien af originale farvebillede 
			byte[] fundet = status.toArray();
			Point[] fArray = fKeyf.toArray();
			Point[] sArray = sKeyf.toArray();
			int thickness = 2;
			int antalFundet = 0;
			vList = new ArrayList<Vektor>();
			for(int i=0; i<fArray.length; i++){
				if(fundet[i] == 1){ // Tegn kun der hvor der er fundet matches
					Imgproc.line(sOrg, fArray[i], sArray[i], new Scalar(255,0,0), thickness);
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

			if(BILLED_DEBUG){			
				long total = System.nanoTime() - startTime;
				long durationInMs = TimeUnit.MILLISECONDS.convert(total, TimeUnit.NANOSECONDS);
				String debug = "Vektorer fundet på: " + durationInMs + " milisekunder.";
				debug = debug + " Punkter fundet: " + antalFundet + ", ud af: " + sKey.size();
				System.out.println(debug);	
			}

			out[0] = sOrg;
			out[1] = second;

			// Gem det behandlede billede samt data så det kan benyttes næste gang metoden kaldes
			first = second;
			fKey = sKey;

			//			this.calcOptMagnitude(vList, sOrg, 3); // TEST KODE
			//			out[0] = this.calcDistances(sOrg, vList, 17, 6); // TEST KODE

			return out;
		} else {
			out[0] = second;
			return out;
		}
	}

	/**
	 * Created by: Jon Tvermose Nielsen
	 * Finder den gennemsnitlige distance fra kamera til vektor i et 6x6 gitter
	 * @param frame Den frame der skal tegnes på (ændres ikke)
	 * @param vectors Vektorer der beregnes på
	 * @param degree Hvor mange grader har dronen bevæget sig
	 * @param Antal kollonner og rækker billedet deles i
	 * @return Kopi af frame med påtegnet gitter
	 */
	public Mat calcDistances(Mat frame, ArrayList<Vektor> vectors, double degree, int size){	
		Mat distFrame = frame.clone();
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

	/**
	 * Created by: Jon Tvermose Nielsen
	 * Finder den gennemsnitlige magnitude for vektorerne i hver firkant i et size * size størrelse billede
	 * @param vectors Vektoren der analyseres
	 * @param frame Billedet der hører til vektorerne
	 * @param size Antal rækker og kollonner billedet opsplittes i
	 * @return Array med magnitude værdier for vektorer i billedet
	 */
	public double[][] calcOptMagnitude(ArrayList<Vektor> vectors, Mat frame, int size){
		double out[][] = new double[size][size];

		if(vectors==null || frame==null){
			return out;
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
			out[x][y] += vectors.get(i).getLength();
			squaresCount[x][y]++;
		}

		// Beregn gennemsnitsmagnituden i hver firkant
		for(int i=0; i<size; i++){
			for(int o=0; o<size; o++){
				if(squaresCount[i][o] > 0){ // Beregn kun gennemsnitsdistance hvis der er noget at beregne på
					out[i][o] = out[i][o] / squaresCount[i][o];
//					if(out[i][o] > 20){						
//						Imgproc.rectangle(frame, new Point(sqWidth*i, sqHeight*o), new Point(sqWidth*(i+1), sqHeight*(o+1)), new Scalar(0,0,255), 4);
//					}
				}
			}
		}

		if(BILLED_DEBUG){
			System.out.println("***");
			System.out.println((int) out[0][0] + "\t" + (int) out[1][0] + "\t" + (int) out[2][0]);
			System.out.println((int) out[0][1] + "\t" + (int) out[1][1] + "\t" + (int) out[2][1]);
			System.out.println((int) out[0][2] + "\t" + (int) out[1][2] + "\t" + (int) out[2][2]);
		}
		return out;
	}

	/**
	 * Created by: Jon Tvermose Nielsen
	 * Finder matches mellem to billeder og forbinder dem med en streg
	 * @param first Første billede
	 * @param second Andet billede
	 * @return Kombineret billede med streger mellem matches
	 */
	public Mat drawMatches(Mat first, Mat second){
		// DEBUG
		long startTime = System.nanoTime();

		MatOfKeyPoint fKey = getKeyPoints(first);
		MatOfKeyPoint sKey = getKeyPoints(second);		
		Mat f = getDescriptors(first, fKey);
		Mat s = getDescriptors(second, sKey);

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

	// Identificerer keypoints i et billede
	private MatOfKeyPoint getKeyPoints(Mat mat){
		FeatureDetector detect = FeatureDetector.create(FeatureDetector.FAST); // Kan være .ORB .FAST eller .HARRIS
		MatOfKeyPoint kp = new MatOfKeyPoint();
		detect.detect(mat, kp);
		return kp;
	}

	// Identificer descriptors i et billede
	private Mat getDescriptors(Mat mat, MatOfKeyPoint kp){
		DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
		Mat descriptors = new Mat();
		extractor.compute(mat, kp, descriptors);
		return descriptors;
	}

	public MatOfKeyPoint getKP(){
		return kp;
	}

	public void qrread(Mat frame){		
		BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
				new BufferedImageLuminanceSource(mat2bufImg(frame))));
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

	public Mat canny(Mat frame){
		frame = toGray(frame);
		//		frame = resize(frame, 320, 240);
		Imgproc.Canny(frame, frame, 200.0, 200.0*2, 5, false );
		return frame;
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
		// create a temporary buffer
		MatOfByte buffer = new MatOfByte();
		// encode the frame in the buffer
		Imgcodecs.imencode(".bmp", frame, buffer);
		// build and return an Image created from the image encoded in the
		// buffer
		return new BufferedImage(frame.width(), frame.height(), java.awt.image.BufferedImage.TYPE_BYTE_INDEXED);

	}

	public ArrayList<Vektor> getVektorArray(){
		System.err.println("VEKTOR ARRAY HENTES. STØRRELSE ER: " + vList.size());
		return vList;
	}

	public Mat trackObject(Mat frame) {
		Mat klon = frame.clone();
		// TODO Auto-generated method stub
		return klon;
	}
	
	public Mat bufferedImageToMat(BufferedImage bi) {
		  Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
		  byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
		  mat.put(0, 0, data);
		  return mat;
		}
}
