package billedanalyse;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
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

import drone.IDroneControl;
import javafx.scene.image.Image;

public class BilledAnalyse implements IBilledAnalyse, Runnable {

	/*
	 * Definerer DEBUG-mode for billedmodulet (der udskrives til konsollen).
	 */
	protected static final boolean BILLED_DEBUG = true;

	private BilledManipulation bm;
	private OpticalFlow opFlow;
	private IDroneControl dc;
	private ObjectTracking objTracker;

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
		objTracker = new ObjectTracking(opFlow, bm);
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
		return bm.bufferedImageToMat(bi);
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
	public Mat[] getImages(){
		return this.frames;
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
				
				if(opticalFlow || objTrack){ // opticalFlow boolean
					frames[1] = this.opFlow.optFlow(img, true);
				}
				if(objTrack){
					frames[2] = objTracker.trackObject();
				} 
//				this.calcOptMagnitude(3);
				frames[0] = img;
				// Enable image filter?
				if(greyScale){						
					frames[0] = bm.filterMat(frames[0]);
				}

				//Enable QR-checkBox?
				if(qr){
					findQR(matFrame);
				}

			} catch (NullPointerException e){
				System.err.println("Intet billede modtaget til billedanalyse. Prøver igen om 50 ms.");
				try {
					// Intet billede modtaget. Vent 50 ms og tjek igen.
					Thread.sleep(50);
				} catch (InterruptedException e1) {
					interrupted = true;
					return;
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
	}

	@Override
	public void setImg(Mat frame) {
		this.webcamFrame = frame;
	}

	@Override 
	public void setImage(Mat frame){
		this.imageToShow[0] = bm.mat2Image(frame);
	}

	@Override
	public String getQrt(){
		return qrs.getQrt();
	}

	@Override
	public Point getObjectCenter() {
		return this.objTracker.getObjectCenter();
	}

	@Override
	public Image mat2Image(Mat mat) {
		return bm.mat2Image(mat);
	}
}
