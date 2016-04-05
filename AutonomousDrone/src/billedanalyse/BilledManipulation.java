package billedanalyse;

import java.awt.image.BufferedImage;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * Hjælpeklasse der indeholder en række metoder til at manipulere billeder
 *
 */
public class BilledManipulation {
	
	private MatOfKeyPoint kp;
	private FeatureDetector detect;
	private DescriptorExtractor extractor;
	
	public BilledManipulation(){
		detect = FeatureDetector.create(FeatureDetector.ORB); // Kan være .ORB .FAST eller .HARRIS
		extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
	}
	
	public MatOfKeyPoint getKP(){
		return kp;
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
		Imgproc.medianBlur(frame, out, 9);
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
		// create a temporary buffer
		MatOfByte buffer = new MatOfByte();
		// encode the frame in the buffer
		Imgcodecs.imencode(".bmp", frame, buffer);
		// build and return an Image created from the image encoded in the
		// buffer
		return new BufferedImage(frame.width(), frame.height(), java.awt.image.BufferedImage.TYPE_BYTE_INDEXED);

	}
	
	public Mat filterMat(Mat mat) {
		// convert the image to gray scale
		//		Imgproc.cvtColor(outFrame[0], outFrame[0], Imgproc.COLOR_BGR2GRAY);
//		mat = resize(mat, 640, 480);
		mat = toGray(mat);
		mat = edde(mat);
		//		outFrame[0] = ph.bilat(outFrame[0]);
		mat = thresh(mat);
		mat = canny(mat);
		mat = keyPointsImg(mat);
		return mat;
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

}
