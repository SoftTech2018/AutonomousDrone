package billedanalyse;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import javafx.scene.image.Image;

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
	
	/** Finder antallet og placering af objekter over en given størrelse, i en bestemt farve. 
	 * Tegner konturer i det tilsendte billede, og returnerer en kopi hvor der kun tegnes hvad kameraet ser.
	 * Udviklet på baggrund af reference:
	 * Reference: https://github.com/ahvsoares/openCVJavaMultipleObjectTracking/blob/master/src/main/java/br/cefetmg/lsi/opencv/multipleObjectTracking/processing/MultipleObjectTracking.java
	 * @param frame
	 * @param hsvMin
	 * @param hsvMax
	 * @return
	 */
	public Mat findColorObjects(Mat org, Mat frame, Mat second, Scalar hsvMin, Scalar hsvMax){
		String color;
		if(hsvMin.equals(Colors.hsvMinGreenDrone) || hsvMin.equals(Colors.hsvMinGreenDroneDown) || hsvMin.equals(Colors.hsvMinGreenWebcam)){
			color = "Green";
		} else {
			color = "Red";
		}
		
		Mat out = new Mat();	
		Imgproc.cvtColor(org, out, Imgproc.COLOR_BGR2HSV);
		
		// Kombiner begge ranges af hues (170-> 180 og 0->10) for at finde alle røder farver
		if(color.equals("red")){
			Mat temp = new Mat();
			Core.inRange(out, hsvMin, hsvMax, temp);
			Core.inRange(out, Colors.hsvMinRed, Colors.hsvMaxRed, out);
			Core.addWeighted(temp, 1.0, out, 1.0, 0.0, out);
		} else {
			Core.inRange(out, hsvMin, hsvMax, out);	
		}
			
		Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
		Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(8, 8));
		Imgproc.erode(out, out, erodeElement);
		Imgproc.erode(out, out, erodeElement);

		Imgproc.dilate(out, out, dilateElement);
		Imgproc.dilate(out, out, dilateElement);
		
		Mat temp = new Mat();
		out.copyTo(temp);
		
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(temp, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

		int objectsFound = 0;

		if(contours.size() > 0){
			for (int i=0; i< contours.size(); i++){
				Moments moment = Imgproc.moments(contours.get(i));
				double area = moment.get_m00();
		
				// Forsøger at fjerne støj
				if (area > Colors.MIN_OBJECT_AREA) {
					objectsFound++;
					int x = (int)(moment.get_m10() / area);
					int y = (int)(moment.get_m01() / area);
					
					// Tegn omridset på det originale billede
					Imgproc.drawContours(frame, contours, i, new Scalar(255,0,0), 3); 
					// Tegn firkanter og tekst på objekt tracking frame for hvert objekt der er fundet
					Imgproc.rectangle(out, new Point(x-(Math.sqrt(area)/2), y-(Math.sqrt(area)/2)), new Point(x+(Math.sqrt(area)/2), y+(Math.sqrt(area)/2)), new Scalar(255,0,0), 3);
//					Imgproc.circle(out, new Point(x, y), (int) Math.sqrt(area/Math.PI), new Scalar(255,0,0), 3);
					Imgproc.putText(frame, color + ": " + x + "," + y, new Point(x-(Math.sqrt(area)/2), y), 1, 2, new Scalar(255, 255, 255), 2);
				}
			}
		}
		if(second != null){
			Core.addWeighted(out, 1, second, 1, 0.0, out);			
		}
		if(BilledAnalyse.BILLED_DEBUG){			
			System.out.println("Fundet " + objectsFound + " objekter!");
		}
		return out;
	}
}
