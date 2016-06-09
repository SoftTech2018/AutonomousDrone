package billedanalyse;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

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
import org.opencv.imgproc.Moments;
import org.opencv.utils.Converters;

import diverse.QrFirkant;
import diverse.koordinat.Koordinat;
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
		Mat newPic = new Mat();
		mat.copyTo(newPic);
		Mat out = new Mat();
		mat.copyTo(out);
		Mat temp = new Mat();
		mat.copyTo(temp);
		temp = toGray(temp);
		Imgproc.GaussianBlur(temp, temp, new Size(5,5), -1);
		Imgproc.Canny(temp, temp, 50, 100);
		
		//QR TING:
		Mat qr = new Mat();
		qr = Mat.zeros(400, 400, CvType.CV_32S);
		Mat test3 = new Mat(400,400,temp.type());
		ArrayList<QrFirkant> firkant = new ArrayList<QrFirkant>();
		
		//Contours gemmes i array
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		//Finder Contours

		Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);

//		List<MatOfPoint> contoursFound = new ArrayList<MatOfPoint>();
//		System.out.println("Contour størrelse: "+contours.size());
		
		//Løber Contours igennem
		for(int i=0; i<contours.size(); i++){
			
			//Konverterer MatOfPoint til MatOfPoint2f, så ApproxPoly kan bruges
			MatOfPoint2f mop2 = new MatOfPoint2f();
			contours.get(i).convertTo(mop2, CvType.CV_32FC1); 
			double epsilon = 0.01*Imgproc.arcLength(mop2, true);
			Imgproc.approxPolyDP(mop2, mop2, epsilon, true);
			//Konverterer MatOfPoint2f til MatOfPoint
			mop2.convertTo(contours.get(i), CvType.CV_32S);
			
			if(contours.get(i).total()==4 && Imgproc.contourArea(contours.get(i))>3000){ //&& Imgproc.contourArea(contours.get(i))>150{
				List<Point> list = new ArrayList<Point>();
//				Konverterer contours om til en liste af punkter for at finde koordinaterne
				Converters.Mat_to_vector_Point(contours.get(i), list);
				
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
//				System.out.println("l1 afstand: "+ l1);
//				System.out.println("l2 afstand: "+ l2);
//				System.out.println("AREAL: "+l1*l2);
//				System.out.println("Checkfirkant "+checkFirkant(l1,l2));
				if(checkFirkant(l1,l2)){
//					firkant.add(new QrFirkant(new Point(x0,y0),new Point(x1,y1),new Point(x2,y2),new Point(x3,y3)));
//					System.out.println("Check true ");
//					Imgproc.putText(out, "0", new Point(list.get(0).x, list.get(0).y), 1, 5, new Scalar(255, 255, 255), 2);
//					Imgproc.putText(out, "1", new Point(list.get(1).x, list.get(1).y), 1, 5, new Scalar(255, 255, 255), 2);
//					Imgproc.putText(out, "2", new Point(list.get(2).x, list.get(2).y), 1, 5, new Scalar(255, 255, 255), 2);
//					Imgproc.putText(out, "3", new Point(list.get(3).x, list.get(3).y), 1, 5, new Scalar(255, 255, 255), 2);
//					Imgproc.putText(out, "list.get(1)", new Point(list.get(1).x, list.get(1).y), 1, 5, new Scalar(255, 255, 255), 2);
//					Imgproc.putText(out, Double.toString((int)l1*l2), new Point(list.get(0).x, list.get(0).y), 1, 5, new Scalar(255, 255, 255), 2);
//					Imgproc.drawContours(out, contours, i, new Scalar(0,0,255), 3);
					
					//QR kode punkter på originale billede
					Point p0 = new Point(list.get(0).x,list.get(0).y);
					Point p1 = new Point(list.get(1).x,list.get(1).y);
					Point p2 = new Point(list.get(2).x,list.get(2).y);
					Point p3 = new Point(list.get(3).x,list.get(3).y);
					
					List<Point> qrPunkter = new ArrayList<Point>();
					qrPunkter.add(p0);
					qrPunkter.add(p1);
					qrPunkter.add(p2);
					qrPunkter.add(p3);
//					System.out.println("X0: "+x0 + " og X3: "+ x3);
					List<Point> qrNyePunkter = new ArrayList<Point>();
					if(x0>x3){
//						System.out.println("X3 er større");
						qrNyePunkter.add(new Point(qr.cols(),0));
						qrNyePunkter.add(new Point(qr.cols(),qr.rows()));
						qrNyePunkter.add(new Point(0,qr.rows()));
						qrNyePunkter.add(new Point(0,0));						
					} else {
//						System.out.println("X3 er mindre");
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
//					System.out.println("HØJDE "+ test3.size().height + " og Bredde "+test3.size().width);
//					BufferedImage testimg = mat2bufImg(test3); 
//					File f = new File("/Users/JacobWorckJepsen/Desktop/MyFile.JPEG"); 
//					try { ImageIO.write(testimg, "JPEG", f); 
//					} catch (IOException e1) {
//					}
				}
				return test3;
			}
		}
		return out;
	}
	
	private double afstand(double x1, double x2, double y1, double y2){
		double result = Math.sqrt(Math.pow((x2-x1),2)+Math.pow((y2-y1),2));
		return result;
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

}
