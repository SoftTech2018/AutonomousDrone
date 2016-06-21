package billedanalyse;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import diverse.Log;
import diverse.PunktNavigering;
import diverse.QrFirkant;
import diverse.YawCalc;
import diverse.circleCalc.Circle;
import diverse.circleCalc.CircleCircleIntersection;
import diverse.circleCalc.Vector2;
import diverse.koordinat.Koordinat;
import diverse.koordinat.OpgaveRum;
import drone.IDroneControl;
import gui.GuiStarter;
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
	private ColorTracker colTracker;
	private YawCalc yawCalc;
	private PapKasseFinder papkassefinder;

	private Image[] imageToShow;
	private Mat[] frames;
	private boolean objTrack, greyScale, qr, webcam = true, opticalFlow, droneLocator, papKasseLocator, posPrecision = true;
	private Mat webcamFrame;
	private Mat matFrame;
	private QRCodeScanner qrs;

	private PunktNavigering punktNav;
	private OpgaveRum opgrum;
	private Koordinat droneKoordinat;
	private long droneKoordinatUpdated = 0;

	public BilledAnalyse(IDroneControl dc){
		this.dc = dc;
		this.bm = new BilledManipulation(dc);
		this.yawCalc = new YawCalc();
		imageToShow = new Image[3];
		frames = new Mat[3];
		this.opFlow = new OpticalFlow(bm);
		objTracker = new ObjectTracking(opFlow, bm);
		colTracker = new ColorTracker(dc);
		papkassefinder = new PapKasseFinder();
		this.punktNav = new PunktNavigering();
		this.qrs = new QRCodeScanner();
	}

	public void setOpgaveRum(OpgaveRum opgRum){
		this.opgrum = opgRum;
		this.yawCalc.setOpgaveRum(opgRum);
	}

	private boolean findDronePos2(Mat frame){
		Circle circle1, circle2;
		Mat temp = new Mat();
		frame.copyTo(temp);
		//Returnerer liste med de to firkanter
		ArrayList<QrFirkant> list = bm.dronePos2(frame);
		if(list==null){
			//			System.out.println("TOM firkant liste");
			return false;
		}
		//		System.out.println("Liste størrelse "+list.size());
		//Returnerer biledet, osm QR læseren skal læse
		String qrText = bm.warpQrImage(list.get(0), qrs, temp);
////		String qrText = qrs.applyFilters(bm.readQrSkewed(temp));
		if(qrText.length() < 3){ // Der kan ikke læses nogen QR-kode
			//			System.out.println("QR kode < 3");
			return false;
		}
		String[] qrTextArray = qrText.split(","); // 0 = QR koden, 1 = x koordinat, 2 = y koordinat
		//		System.out.println("0"+qrTextArray[0]);
		//		System.out.println("1"+qrTextArray[1]);
		//		System.out.println("2"+qrTextArray[2]);
		QrFirkant readQr;
		QrFirkant readQr2;


		//		readQr = bm.getFirkanten();
		readQr = list.get(0);
		readQr.setText(qrTextArray[0]);

		//		readQr2 = bm.getFirkanten2();
		readQr2 = list.get(1);

		//Finder centrumkoordinat for firkanterne
		Koordinat qrKoord1 = readQr.getCentrum();
		Koordinat qrKoord2 = readQr2.getCentrum();

		//Finder distance og koordinater for QR-firkanten
		Vector2 v1 = this.opgrum.getMultiMarkings(readQr.getText())[1];
		Vector2 v2 ;
		// Beregn distancen til QR koden
		double dist1 = punktNav.calcDist(readQr.getHeight(), 420)/10;
		//		System.out.println("Dist1 "+dist1);
		//		System.out.println("Højde 1 "+readQr.getHeight());
		//Laver cirkel for læst qr kode
		circle1 = new Circle(v1, dist1);

		if(qrKoord1.getX() > qrKoord2.getX()){
			v2 = this.opgrum.getMultiMarkings(readQr.getText())[0];
		}else{
			v2 = this.opgrum.getMultiMarkings(readQr.getText())[2];
		}
		getFirkant().setText(readQr.getText());

		double dist2 = punktNav.calcDist(readQr2.getHeight(), 420)/10;
		//		System.out.println("Dist2 "+dist2);
		//		System.out.println("Højde 2 "+readQr2.getHeight());
		circle2 = new Circle(v2, dist2);

		CircleCircleIntersection cci = new CircleCircleIntersection(circle1, circle2);
		//		System.err.println(cci.getIntersectionPoints().length);
		//		for (int i = 0; i < cci.getIntersectionPoints().length; i++) {
		//			System.err.println(cci.getIntersectionPoints()[i]);
		//			//			
		//		}
		//		System.out.println("Afstand 1 = " + dist1 + "Afstand 2 = + " + dist2);

		//		System.out.println("f1 ck: "+readQr.getCentrum().getX() + " og f2 ck: "+readQr2.getCentrum().getX());

		// Find skæringspunktet der ligger inde i rummets koordinatsystem
		Vector2[] intersections = cci.getIntersectionPoints();
		for(int i=0; i<intersections.length; i++){
			// Tjek om punktet ligger inde i rummets koordinatsystem
			if(intersections[i].x > 0 && intersections[i].x < 963 && intersections[i].y > 0 && intersections[i].y < 1078){
				Koordinat drone = new Koordinat((int) intersections[i].x, (int)intersections[i].y);
				this.setDroneKoordinat(drone, true); // Opdater dronens position på GUI m.m.
				opgrum.setCircleInfo(v1, v2, dist1, dist2);
				return true;
			}
		}
		return false;
	}

	private void findDronePos(Mat frame){
//		ArrayList<QrFirkant> qrFirkanter = punktNav.findQR(frame);
//		if(qrFirkanter==null || qrFirkanter.isEmpty()){
//			return;
//		}
		Mat temp = new Mat();
		frame.copyTo(temp);
//		String qrText = qrs.applyFilters(bm.readQrSkewed(frame));
		//		System.err.println("QR text: " + qrText);
		
		// Tjek om den læste QR-kode matcher en fundet firkant i billedet
		QrFirkant readQr = bm.getFirkanten();
		if(readQr==null){
			return;
		}
		String qrText = bm.warpQrImage(readQr, qrs, temp);
		String[] qrTextArray = qrText.split(","); // 0 = QR koden, 1 = x koordinat, 2 = y koordinat
		if(qrText.length() < 3){ // Der kan ikke læses nogen QR-kode
			return;
		}
		readQr.setText(qrTextArray[0]);

		//		Koordinat qrCentrum = bm.getQrCenter();
		//		readQr = qrFirkanter.get(0);
		//		int minDist = readQr.getCentrum().dist(qrCentrum);
		//		for(QrFirkant qrF : qrFirkanter){
		//			int dist = qrF.getCentrum().dist(qrCentrum);
		//			if(dist < minDist){
		//				readQr = qrF;
		//			}
		//		}
		//			Imgproc.putText(frame, Integer.toString(minDist), new Point(readQr.getCentrum().getX(), readQr.getCentrum().getY()), 1, 2.0, new Scalar(255,0,0),3);
		//			Imgproc.circle(frame, new Point(readQr.getCentrum().getX(), readQr.getCentrum().getY()), 50, new Scalar(255,0,0), 5);
		//			Imgproc.circle(frame, new Point(qrCentrum.getX(), qrCentrum.getY()), 10, new Scalar(255,0,0), 5);
		//			Imgproc.line(frame, new Point(readQr.getCentrum().getX(), readQr.getCentrum().getY()), new Point(qrCentrum.getX(), qrCentrum.getY()), new Scalar(255,0,0), 3);
		//			System.err.println("DIST: " + minDist + " - (" + qrCentrum.getX() + "," + qrCentrum.getY() + ")");
		//			System.err.println("QR centrum: (" + qrCentrum.getX() + "," + qrCentrum.getY() + ")");
		//			System.err.println("QrFirkant centrum: (" + readQr.getCentrum().getX() + "," + readQr.getCentrum().getY() + ")");
		//		if(minDist > 25){
		//			System.err.println("Ingen matchende firkant fundet!");
		//			return;
		//		}

		// Find det rigtige koordinat på den aflæse vægmarkering
		Vector2 v = this.opgrum.getMultiMarkings(readQr.getText())[1];
		//		System.err.println("Vægmarkering koordinat: (" + v.x + "," + v.y + ")");
		readQr.setPlacering(new Koordinat((int) v.x, (int) v.y));
		getFirkant().setText(readQr.getText());

		// Beregn distancen til QR koden
		double dist = punktNav.calcDist(readQr.getHeight(), 420);
		//		System.err.println("Distance:" + dist * 0.1);
		//		Imgproc.putText(frame, Integer.toString((int) (dist*0.1)), new Point(readQr.getCentrum().getX(), readQr.getCentrum().getY()), 1, 4, new Scalar(255,0,0), 5);

		// Find vinklen til QR koden
		// Dronens YAW + vinklen i kameraet til QR-koden
		double yawCorrection = this.yawCalc.getYaw(this.getFirkant());
		if(yawCorrection > -180 && yawCorrection < 180){
			dc.setYawCorrection(yawCorrection);			
		}
		int yaw = -1*dc.getFlightData()[2]; // Spejlvend YAW så vinklen passer med radianer
		int imgAngle = (int) punktNav.getAngle(readQr.getCentrum().getX(), (int) (frame.size().width/2)); // DeltaX fra centrum af billedet til centrum af QR koden/firkanten
		int totalAngle = yaw - imgAngle;

		//		System.err.println("Total vinkel:" + totalAngle);

		Koordinat qrPlacering = readQr.getPlacering();
		// Beregn dronens koordinat
		Koordinat dp = new Koordinat((int) (dist*Math.cos(Math.toRadians(totalAngle))*0.1), 
				(int) (dist*Math.sin(Math.toRadians(totalAngle))*0.1));
		dp.setX(qrPlacering.getX() - dp.getX()); //Forskyder i forhold til QR-kodens rigtige markering
		dp.setY(qrPlacering.getY() - dp.getY());
		//		System.err.println("DroneKoordinat: (" + dp.getX() + "," + dp.getY() + ")");
		// Logisk tjek for om dronen befinder sig i rummet eller ej
		if(dp.getX()>0 && dp.getY() >0 && dp.getX() < 963 && dp.getY() < 1078){			
			setDroneKoordinat(dp, false);
		}
	}

	/**
	 * Set dronens koordinat i rummet
	 * @param drone Koordinatet
	 * @param toQrKoderMetode true hvis der er brugt to QR koder til at bestemme koordinatet
	 */
	private void setDroneKoordinat(Koordinat drone, boolean toQrKoderMetode){
		if(toQrKoderMetode){ // To QR-koder er brugt til positionsbestemmelse
			this.updateDroneKoordinat(drone);
//			int yawCorrection = this.yawCalc.findYaw(this.getFirkant(), drone);
			int yawCorrection = (int) this.yawCalc.getYaw(bm.getFirkanten());
//			if(yawCorrection > 179){
//				yawCorrection = 359 - yawCorrection;
//			} else {
//				yawCorrection = yawCorrection * -1;
//			}
//			Log.writeLog("DEBUG: yawCorrection er: " + yawCorrection);
			if(yawCorrection > -180 && yawCorrection < 180){
				dc.setYawCorrection(yawCorrection);			
			}
			this.posPrecision = true;
		} else if (posPrecision){ // En QR-kode er brugt til positionsbestemmelse
			if((System.currentTimeMillis() - droneKoordinatUpdated) > 3500){
				this.updateDroneKoordinat(drone);
				posPrecision = false;
			}
		} else {
			this.updateDroneKoordinat(drone);
			posPrecision = false;
		}
	}

	/**
	 * MÅ IKKE KALDES! Benyt i stedet setDroneKoordinat()
	 * @param drone
	 */
	private void updateDroneKoordinat(Koordinat drone){
		int yaw = dc.getFlightData()[2];
//		Log.writeLog("DroneKoordinat opdateret: " + drone.toString() + "\t YAW: " + yaw);
		this.droneKoordinat = drone;			
		this.droneKoordinatUpdated = System.currentTimeMillis();
		this.opgrum.setDronePosition(drone, Math.toRadians(-1*yaw));
		playSound();
	}

	@Override
	public Koordinat getDroneKoordinat(){
		// Hvis koordinatet er mere end X ms gammelt så dur det ikke...
		if(System.currentTimeMillis() - droneKoordinatUpdated > 2500){ 
			return null;
		} 
		return droneKoordinat;
	}

	private void playSound(){
		try{
			Clip clip = AudioSystem.getClip();
			AudioInputStream inputStream = AudioSystem.getAudioInputStream(
					GuiStarter.class.getResourceAsStream("beep.wav"));
			clip.open(inputStream);
			clip.start();
		} catch (Exception e){
			//			e.printStackTrace();
		}
	}

	@Override
	public void setQR(boolean qr){
		this.droneLocator = qr;
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

	//	/* (non-Javadoc)
	//	 * @see billedanalyse.IBilledAnalyse#qrread(org.opencv.core.Mat)
	//	 */
	//	@Override
	//	public void qrread(Mat frame){	
	//		BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
	//				new BufferedImageLuminanceSource(bm.mat2bufImg(frame))));
	//		Reader reader = new QRCodeMultiReader();
	//
	//		try {							
	//			Result qrout = reader.decode(binaryBitmap);
	//			System.out.println(qrout.getText());
	//			System.out.println("HIT");
	//		} catch (NotFoundException e) {
	//
	//		} catch (ChecksumException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		} catch (FormatException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//	}

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

	public QrFirkant getFirkant(){
		return bm.getFirkanten();
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
	public void setDroneLocator(boolean drone){
		this.droneLocator = drone;
	}

	@Override
	public Mat[] getImages(){
		return this.frames;
	}

	@Override
	public void run() {
		System.err.println("*** BilledAnalyse starter.");
		Mat img;
		BufferedImage bufimg = null;
		boolean interrupted = false;
		while(!Thread.interrupted() || interrupted){
			Long startTime = System.currentTimeMillis();
			BufferedImage temp = null;
			try {
				if(webcam){	
					if(webcamFrame==null){		
						continue;
					}
					img = this.webcamFrame;
					bufimg = bm.mat2bufImg(img);
				} else {
					temp = dc.getbufImg();
					try{
						// Er billedet forskelligt fra sidst behandlede billede?
						if(temp != null && !bufimg.equals(temp)){ 
							bufimg = temp;
						} else { // Vent 5 ms, og start while løkke forfra
							Thread.sleep(5);
							//							System.err.println("****** Venter 5 ms!");
							continue;
						}
					} catch (NullPointerException e){ // bufimg er null, men temp er ikke
						e.printStackTrace();
						bufimg = temp;
					}
					img = this.bufferedImageToMat(bufimg);				
				}
				//				System.err.println("Højde: " + img.size().height + ", Bredde: " + img.size().width);
				//				img = resize(img, 640, 480);

				//				BufferedImage bufResize = new BufferedImage(bufimg.getWidth()/2, bufimg.getHeight()/2,
				//				        BufferedImage.TYPE_BYTE_INDEXED);
				//
				//				    AffineTransform tx = new AffineTransform();
				//				    tx.scale(1, 2);
				//
				//				    AffineTransformOp op = new AffineTransformOp(tx,
				//				        AffineTransformOp.TYPE_BILINEAR);
				//				    bufimg = op.filter(bufimg, null);

				matFrame = img;

				if(opticalFlow){ // opticalFlow boolean
					frames[1] = this.opFlow.optFlow(img, true);

				}

				if(objTrack){
					//					frames[2] = objTracker.trackSurfObject(bufimg);
					//					frames[2] = this.papkassefinder.findPapkasse(img);
					frames[2] = this.colTracker.findColorObjects(img);
				} 

				//frames[0] = img;
				// Enable image filter?
				if(greyScale){						
					//					frames[0] = bm.filterMat(frames[0]);						
				}

				if(papKasseLocator){
					this.papkassefinder.findPapkasse(img);
				}

				if(droneLocator){
					if(!this.findDronePos2(img)){
						this.findDronePos(img);						
					} else {
						// Tegn den firkant der forsøges at blive læst
//						Imgproc.rectangle(img, bm.getFirkanten().getPoint0(), bm.getFirkanten().getPoint2(), new Scalar(255,0,0), 3);
					}
				}

				//Enable QR-checkBox?
				if(qr){
					//					Mat testimg = bm.readQrSkewed(img);
					//					findQR(testimg);
					//					frames[2]=testimg;
					//										frames[0] = bm.filterMat(img);
//										this.findDronePos2(img);
				} 
				frames[0]=img;

			} catch (NullPointerException e){
				System.err.println("Intet billede modtaget til billedanalyse. Prøver igen om 50 ms.");
				e.printStackTrace();

				// Intet billede modtaget. Vent 50 ms og tjek igen.
				try {
					Thread.sleep(50);
				} catch (InterruptedException e1) {
					interrupted = true;
					return;
				}

			} catch (InterruptedException e) {
				interrupted = true;
				return;
			}
			if(BilledAnalyse.BILLED_DEBUG){
				System.out.println("*** BilledAnalyse færdig på: " + (System.currentTimeMillis() - startTime) + " ms. ****");				
			}
		}
		System.err.println("*** BilledAnalyse stopper.");
	}

	public void findQR(Mat frame){
		Mat out = new Mat();
		frame.copyTo(out);
		//		bm.toGray(frame);
		qrs.applyFilters(frame);
		//		qrs.imageUpdated(frame);
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

	@Override
	public ArrayList<Squares> getColorSquares() {
		return colTracker.getSquares();
	}

	public void setMaxVal(int val){
		bm.setMax(val);
	}
	public void setMinVal(int val){
		bm.setMin(val);
	}

	@Override
	public boolean isPapKasseLocator() {
		return papKasseLocator;
	}

	@Override
	public void setPapKasseLocator(boolean papKasseLocator) {
		this.papKasseLocator = papKasseLocator;
	}

	@Override
	public int[] getPapKasse() {
		return this.papkassefinder.getDist();
	}


}
