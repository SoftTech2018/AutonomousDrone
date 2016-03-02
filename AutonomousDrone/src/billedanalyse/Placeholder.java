package billedanalyse;

import java.awt.image.BufferedImage;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

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
	private Size resize = new Size(240, 180);
	Mat frame_out = new Mat();
	int iLowH = 160;
	int iHighH = 190;

	int iLowS = 50; 
	int iHighS = 255;

	int iLowV = 0;
	int iHighV = 255;

	public Mat AkazaKeyPoints(Mat frame){

		Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);

		FeatureDetector detect = FeatureDetector.create(FeatureDetector.ORB);

		kp = new MatOfKeyPoint();

		detect.detect(frame, kp);

		Features2d.drawKeypoints(frame, kp, frame);

		return frame;
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

	private Mat erode(Mat frame_in){

		Mat frame_out = new Mat();

		int erosion_size = 2;
		Point point = new Point( -1, -1 );
		Size size = new Size(erosion_size, erosion_size);

		Mat erodeelement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, size, point);

		Imgproc.erode(frame_in, frame_out, erodeelement);
		//		Imgproc.erode(frame_in, frame_out, new Mat());

		return frame_out;
	}

	private Mat dilate(Mat frame_in){

		Mat frame_out = new Mat();

		int dilation_size = 2;
		Point point = new Point( -1, -1 );
		Size size = new Size(dilation_size, dilation_size);

		Mat diluteelement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, size, point);

		Imgproc.dilate(frame_in, frame_out, diluteelement);
		//		Imgproc.dilate(frame_in, frame_out, new Mat());

		return frame_out;
	}


	//	Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2HSV);
	//	
	//	Core.inRange(frame, new Scalar(iLowH, iLowS, iLowV), new Scalar(iHighH, iHighS, iHighV), frame_out);
	//	
	//	frame = frame_out;
	//	
	//	Imgproc.erode(frame, frame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) );
	//	Imgproc.dilate( frame, frame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) );
	//	
	//	Imgproc.dilate( frame, frame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) );
	//	Imgproc.erode(frame, frame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) );

	//	
	//	Imgproc.equalizeHist(frame, frame);
	//	
	//	Imgproc.resize(frame, frame, resize);

	//	int erode_rep = 10;
	//	int dilate_rep = 5;
	//	
	//	for(int j = 0;j<dilate_rep;j++){
	//		frame = dilate(frame);
	//	}
	//	for(int i = 0;i<erode_rep;i++){
	//		frame = erode(frame);							
	//	}
	//	
	//	Imgproc.Canny( frame, frame, 100.0, 100.0*2, 3, false );
	//	Imgproc.bilateralFilter(frame, frame_out, 5, 80.0, 80.0);
	//	Imgproc.threshold(frame_out, frame, 30, 255, 0);
	//	frame = frame_out;

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
