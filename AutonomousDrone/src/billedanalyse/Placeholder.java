package billedanalyse;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
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
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.google.zxing.qrcode.QRCodeReader;

public class Placeholder {

	private MatOfKeyPoint kp;

	/*
	 * Definerer DEBUG-mode for billedmodulet (der udskrives til konsollen).
	 */
	protected static final boolean BILLED_DEBUG = true;

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
	 * Udfører Optical Flow analyse mellem to frames og tegner resultatet på det returnede frame
	 * @param first Første frame
	 * @param second Anden frame - denne frame returnes med resultatet
	 * @return Mat second - påtegnet resultatet
	 */
	public Mat[] optFlow(Mat first, Mat second){
		long startTime = System.nanoTime(); // DEBUG

		// Gem en kopi af det orignale farvebillede der skal vises til sidst
		Mat sOrg = second.clone();

		// Initier variable der gemmes data i
		MatOfPoint fKey = new MatOfPoint();
		MatOfPoint sKey = new MatOfPoint();


		
		first = this.edde(first);
		first = this.thresh(first);
		first = this.canny(first);
		
		second = this.edde(second);
		second = this.thresh(second);
		second = this.canny(second);
		
//		// Konverter til gråt billede
//		Imgproc.cvtColor(first, first, Imgproc.COLOR_BGR2GRAY);
//		Imgproc.cvtColor(second, second, Imgproc.COLOR_BGR2GRAY);

		// Find punkter der er gode at tracke. Gemmes i fKey og sKey
		Imgproc.goodFeaturesToTrack(first, fKey, 400, 0.01, 30);
		Imgproc.goodFeaturesToTrack(second, sKey, 400, 0.01, 30);

		// Kør opticalFlowPyrLK
		MatOfPoint2f fKeyf = new MatOfPoint2f(fKey.toArray());
		MatOfPoint2f sKeyf = new MatOfPoint2f(sKey.toArray());
		MatOfByte status = new MatOfByte();
		MatOfFloat err = new MatOfFloat();
		Video.calcOpticalFlowPyrLK(first, second, fKeyf, sKeyf, status, err );

		// Tegn vektorer på kopien af originale farvebillede 
		byte[] fundet = status.toArray();
		Point[] fArray = fKeyf.toArray();
		Point[] sArray = sKeyf.toArray();
		int thickness = 2;
		for(int i=0; i<fArray.length; i++){
			if(fundet[i] == 1){ // Tegn kun der hvor der er fundet matches
				Imgproc.line(sOrg, fArray[i], sArray[i], new Scalar(255,0,0), thickness);						
			}		
		}

		if(BILLED_DEBUG){			
			long total = System.nanoTime() - startTime;
			long durationInMs = TimeUnit.MILLISECONDS.convert(total, TimeUnit.NANOSECONDS);
			String debug = "Vektorer fundet på: " + durationInMs + " milisekunder.";
			debug = debug + " Punkter fundet: " + sKey.size() + " : " + fKey.size();
			System.out.println(debug);	
		}

		Mat out[] = new Mat[2];
		out[0] = sOrg;
		out[1] = second;
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
		Imgproc.threshold(frame, frame1, 170, 255, 0);
		return frame1;
	}

	public Mat bilat(Mat frame){
		Mat frame1 = new Mat();
		Imgproc.bilateralFilter(frame, frame1, 50, 80.0, 80.0);
		return frame;
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

	private BufferedImage mat2bufImg(Mat frame){
		// create a temporary buffer
		MatOfByte buffer = new MatOfByte();
		// encode the frame in the buffer
		Imgcodecs.imencode(".bmp", frame, buffer);
		// build and return an Image created from the image encoded in the
		// buffer
		return new BufferedImage(frame.width(), frame.height(), java.awt.image.BufferedImage.TYPE_BYTE_INDEXED);

	}
}
