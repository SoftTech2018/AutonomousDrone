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

public class BilledAnalyse implements IBilledAnalyse {

	/*
	 * Definerer DEBUG-mode for billedmodulet (der udskrives til konsollen).
	 */
	protected static final boolean BILLED_DEBUG = false;

	private BilledManipulation bm;
	private OpticalFlow opFlow;

	public BilledAnalyse(){
		this.bm = new BilledManipulation();
		this.opFlow = new OpticalFlow(bm);
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

		// Hvis der ikke er kørt Optical Flow endnu returnerer vi kun 0'er.
		if((vectors = opFlow.getVektorArray())==null || (frame = opFlow.getFrame())==null){
			for(int i=0; i<size; i++){
				for(int o=0; o<size; o++){
					out[i][o] = 0;
				}
			}
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

	/* (non-Javadoc)
	 * @see billedanalyse.IBilledAnalyse#drawMatches(org.opencv.core.Mat, org.opencv.core.Mat)
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see billedanalyse.IBilledAnalyse#resize(org.opencv.core.Mat, int, int)
	 */
	@Override
	public Mat resize(Mat frame, int i, int j) {
		return bm.resize(frame, i, j);
	}

	/* (non-Javadoc)
	 * @see billedanalyse.IBilledAnalyse#optFlow(org.opencv.core.Mat, boolean, boolean)
	 */
	@Override
	public Mat[] optFlow(Mat frame, boolean optFlow, boolean objTrack) {
		return opFlow.optFlow(frame, optFlow, objTrack);
	}

	/* (non-Javadoc)
	 * @see billedanalyse.IBilledAnalyse#filterMat(org.opencv.core.Mat)
	 */
	@Override
	public Mat filterMat(Mat mat) {
		return bm.filterMat(mat);
	}

	/* (non-Javadoc)
	 * @see billedanalyse.IBilledAnalyse#getVektorArray()
	 */
	@Override
	public ArrayList<Vektor> getVektorArray() {
		return opFlow.getVektorArray();
	}
}
