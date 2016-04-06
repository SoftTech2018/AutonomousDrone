package gui;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

import billedanalyse.BilledAnalyse;
import billedanalyse.IBilledAnalyse;
import billedanalyse.QRCodeScanner;
import diverse.TakePicture;
import drone.DroneControl;
import drone.IDroneControl;
import drone.OpgaveAlgoritme;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class GuiController {

	private final boolean RECORDVIDEO = false; // Sæt til true for at optage en videostream.
	
	private final boolean TAKEPICTURE = false; // Sæt til true for at tage et testbillede via toggle funktionen

	private IDroneControl dc = new DroneControl();
	private IBilledAnalyse ba = new BilledAnalyse(dc);
	private OpgaveAlgoritme opg = new OpgaveAlgoritme(dc, ba);
	private Thread opgThread, baThread;

	// NUMPAD 7
	@FXML
	private Button strafeLeft_btn;

	@FXML
	private CheckBox objTracking_checkBox;

	@FXML
	private CheckBox testVideo_checkBox;

	@FXML
	private ImageView objTrack_imageView;

	// START CAMERA BUTTON
	@FXML
	private Button start_btn;

	@FXML
	private Label qrt_label;

	@FXML
	private ImageView optFlow_imageView;

	// NUMPAD 1
	@FXML
	private Button up_btn;

	// NUMPAD 2
	@FXML
	private Button stop_btn;

	@FXML
	private CheckBox grey_checkBox;

	@FXML
	private CheckBox optFlow_checkBox;

	@FXML
	private CheckBox qr_checkBox;

	// NUMPAD 6
	@FXML
	private Button right_btn;

	// NUMPAD 5
	@FXML
	private Button back_btn;

	// NUMPAD 9
	@FXML
	private Button strafeRight_btn;

	// NUMPAD 3
	@FXML
	private Button down_btn;

	// NUMPAD 8
	@FXML
	private Button forward_btn;

	// NUMPAD 4
	@FXML
	private Button left_btn;

	// ENTER
	@FXML
	private Button takeoff_btn;

	@FXML
	private CheckBox cam_chk;

	// ??
	@FXML
	private Button changeCam_btn;

	@FXML
	private ImageView currentFrame;

	@FXML
	private ChoiceBox<Integer> frames_choiceBox;

	@FXML
	private Label roll_label;

	@FXML
	private Label yaw_label;

	@FXML
	private Label pitch_label;

	// a timer for acquiring the video stream
	private ScheduledExecutorService timer, droneTimer;
	// the OpenCV object der henter video fra Webcam
	private VideoCapture capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive = false;
	// a flag to enable/disable greyscale colors
	private boolean greyScale = false, optFlow = false, qr = false;
	// Antal ms mellem hver frame (33 ms = 30 fps)
	private int frameDt = 33;
	// Objekt der bruges til at opdatere billedet på GUI
	private Runnable frameGrabber;
	// Liste af valgmuligheder i GUI til frames per second
	private ObservableList<Integer> frameChoicesList = FXCollections.observableArrayList(15, 30);
	// Flyver dronen?
	private boolean flying = false;
	// Skal der hentes video fra webcam?
	private boolean webcamVideo = true;
	// trackes der objekter?
	private boolean objTrack = false;
	// Tæller op indtil der er forbindelse med dronen eller max er nået
	private int droneTime = 0, droneMaxTime = 20 ;
	// Bruges til at gemme en videosekvens
	private VideoWriter outVideo;
	// Bruges til at læse fra en videosekvens
	private VideoCapture testStream;
	// Analyserer vi test-video?
	private boolean useTestVideo = false; 

	@FXML
	private void initialize(){		
		frames_choiceBox.setValue(30);
		frames_choiceBox.setItems(frameChoicesList);

		// Håndterer når man skifter FPS - også selvom kameraet kører allerede
		frames_choiceBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>(){
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
				frameDt = 1000/frameChoicesList.get((Integer) arg2);
				dc.setFps((int) 1000/frameDt); // sæt dronens kamera til den matchende FPS (min 15, max 30).

				if(GuiStarter.GUI_DEBUG){
					System.out.println("Debug: GuiController.frames_choiceBox changelistener kaldt! Frames er: " + (int) 1000/frameDt);
				}
				if(GuiController.this.timer!=null){					
					GuiController.this.timer.shutdown();
					GuiController.this.timer = Executors.newSingleThreadScheduledExecutor();
					GuiController.this.timer.scheduleAtFixedRate(GuiController.this.frameGrabber, 0, frameDt, TimeUnit.MILLISECONDS);
				}
			}
		});

		// Databinding mekanisme til at opdatere GUI
		pitch_label.textProperty().bind(pitch);
		yaw_label.textProperty().bind(yaw);
		roll_label.textProperty().bind(roll);
		qrt_label.textProperty().bind(qrt);

		// Tjek om dronen er klar til takeoff
		this.takeoff_btn.setDisable(true);
		Runnable droneChecker = new Runnable() {
			@Override

			public void run(){
				droneTime++;
				if(dc.isReady()){
					takeoff_btn.setDisable(false);
					try{
						droneTimer.shutdown();
						droneTimer.awaitTermination(33, TimeUnit.MILLISECONDS);
					}catch (InterruptedException e){
						System.err.println("Exception in stopping gui.GuiController.droneTimer..." + e);
					}
				} 
				if(droneTime>=droneMaxTime){
					try{
						if(GuiStarter.GUI_DEBUG){
							System.out.println("Dronen kunne ikke kontaktes. Genstart program for at forsøge igen.");
						}
						droneTimer.shutdown();
						droneTimer.awaitTermination(33, TimeUnit.MILLISECONDS);
					}catch (InterruptedException e){
						System.err.println("Exception in stopping gui.GuiController.droneTimer..." + e);
					}
				}
			}
		};
		this.droneTimer = Executors.newSingleThreadScheduledExecutor();
		this.droneTimer.scheduleAtFixedRate(droneChecker, 0, 1000, TimeUnit.MILLISECONDS);
	}

	@FXML
	private Button startOpgAlgo;

	@FXML
	void startCamera(ActionEvent event) {
		if(GuiStarter.GUI_DEBUG){
			System.out.println("Debug: GuiController.startCamera() kaldt!");
		}

		optFlow_checkBox.setDisable(!cameraActive);
		objTracking_checkBox.setDisable(!cameraActive);
		testVideo_checkBox.setDisable(!cameraActive);
		
		// Hvis kameraet er inaktivt, skal det aktiveres. Ergo startes en tråd med billedanalyse
		if(!cameraActive){
			baThread = new Thread((BilledAnalyse) ba);
			baThread.start();
		} else { // Kameraet slukkes, billedanalyse stoppes.
			baThread.interrupt();
		}

		if(event == null || !event.getSource().equals(startOpgAlgo)){
			startOpgAlgo.setDisable(!cameraActive);
			if(useTestVideo){
				startTestVideoStream();
			} else{
				if(webcamVideo){
					// Video hentes fra webcam
					startWebcamStream();
				} else {
					// Video hentes fra dronen
					startDroneStream();
				}
			}
		} else if(event.getSource().equals(startOpgAlgo)){
			// Skynet starter
			startOpgaveAlgoritme();
		}
	}

	private void startTestVideoStream(){
		if (!this.cameraActive){
			testStream = new VideoCapture(".\\outVideo.avi");
			double fps = testStream.get(5);
			frameDt = (int) (1000/fps);
			this.cameraActive = true;

			// grab a frame every 33 ms (30 frames/sec)
			frameGrabber = new Runnable() {
				@Override
				public void run(){
					Mat frame = new Mat();
					testStream.read(frame);
					if(frame.empty()){
						timer.shutdown();
						System.err.println("Test video er slut.");
						Platform.runLater(new Runnable(){
							@Override
							public void run() {
								GuiController.this.startCamera(null);
							}	
						});
					} else {
						Image imageToShow[] = ba.getImages();
						currentFrame.setImage(imageToShow[0]); // Main billede
						optFlow_imageView.setImage(imageToShow[1]); // Optical Flow
						objTrack_imageView.setImage(imageToShow[2]); // Objeckt Tracking
					}
				}
			};
			this.timer = Executors.newSingleThreadScheduledExecutor();
			this.timer.scheduleAtFixedRate(frameGrabber, 0, frameDt, TimeUnit.MILLISECONDS);

			// update the button content
			this.start_btn.setText("Stop Camera");
		} else {
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.start_btn.setText("Start Camera");

			// stop the timer
			try{
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}catch (InterruptedException e)	{
				// log the exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
			// clean the frame
			this.currentFrame.setImage(null);
			this.optFlow_imageView.setImage(null);
			this.objTrack_imageView.setImage(null);
		}
	}

	private void startOpgaveAlgoritme(){
		grey_checkBox.setDisable(!cameraActive);
		optFlow_checkBox.setDisable(!cameraActive);
		qr_checkBox.setDisable(!cameraActive);
		start_btn.setDisable(!cameraActive);
		changeCam_btn.setDisable(!cameraActive);
		cam_chk.setDisable(!cameraActive);
		optFlow_checkBox.setDisable(!cameraActive);
		objTracking_checkBox.setDisable(!cameraActive);
		testVideo_checkBox.setDisable(!cameraActive);
		frames_choiceBox.setDisable(!cameraActive);

		if (!this.cameraActive)	{
			System.err.println("*** WARNING - SKYNET COMING ONLINE!");
			this.cameraActive = true;
			
//			// Indstil billedanalyse uanset brugerens valg
//			ba.setWebCam(false); // Billeder fra dronen bruges
//			ba.setOpticalFlow(true); // Der udføres optical flow
//			ba.setObjTrack(true); // Der udføres objekt tracking
//			ba.setGreyScale(false); // Der konverteres ikke til greyscale

			// Start opgaveAlgoritmen i en seperat tråd
			opgThread = new Thread(opg);
			opgThread.start();

			// Opdater GUI'en så det matcher med det antal FPS man har valgt
			frameGrabber = new Runnable() {
				@Override
				public void run(){
					//					opg.getPossibleManeuvers();
					Image imageToShow[] = ba.getImages();
					currentFrame.setImage(imageToShow[0]); // Main billede
					optFlow_imageView.setImage(imageToShow[1]);	// Optical Flow
					objTrack_imageView.setImage(imageToShow[2]); // Objeckt Tracking
				}
			};

			this.timer = Executors.newSingleThreadScheduledExecutor();
			this.timer.scheduleAtFixedRate(frameGrabber, 0, frameDt, TimeUnit.MILLISECONDS);

			if(RECORDVIDEO){	 // TESTKODE
				int fourcc = VideoWriter.fourcc('M', 'J', 'P', 'G');
				Size frameSize = new Size(640,480);
				outVideo = new VideoWriter(".\\outVideo.avi", fourcc, 15, frameSize, true);
			}

			// update the button content
			this.startOpgAlgo.setText("Stop Skynet");
		} else {
			// Forsøg at stoppe opgavealgoritmen ASAP
			System.err.println("*** TRYING TO DESTROY SKYNET");
			opgThread.interrupt();

			this.cameraActive = false;
			this.startOpgAlgo.setText("Start Skynet");
			
//			// Indstil billedanalyse tilbage til brugerens valg
//			ba.setWebCam(webcamVideo); // Billeder fra dronen bruges
//			ba.setOpticalFlow(optFlow); // Der udføres optical flow
//			ba.setObjTrack(objTrack); // Der udføres objekt tracking
//			ba.setGreyScale(greyScale); // Der konverteres ikke til greyscale

			// Hvis vi optager video stopper vi optageren.
			if(RECORDVIDEO){
				outVideo.release(); 			
			}
			// stop the timer
			try{
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}catch (InterruptedException e){
				// log the exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}

			// clean the frame
			this.currentFrame.setImage(null);
			this.optFlow_imageView.setImage(null);
			this.objTrack_imageView.setImage(null);
		}
	}

	private void startWebcamStream(){
		if (!this.cameraActive)	{
			// start the video capture
			this.capture.open(0);

			// is the video stream available?
			if (this.capture.isOpened()){
				this.cameraActive = true;

				// grab a frame every 33 ms (30 frames/sec)
				frameGrabber = new Runnable() {
					@Override
					public void run(){
						// Brug webkameraet
						Mat frame = new Mat();
						if (GuiController.this.capture.isOpened()){
							try	{
								// read the current frame
								GuiController.this.capture.read(frame);
								ba.setImg(frame);
								
								if(RECORDVIDEO){
									GuiController.this.recordVideo(frame); // TESTKODE
								}
							}catch (Exception e){
								// log the error
								System.err.println("Exception during the image elaboration: " + e);
								e.printStackTrace();
							}
						}
						Image imageToShow[] = ba.getImages();
						currentFrame.setImage(imageToShow[0]); // Main billede
						optFlow_imageView.setImage(imageToShow[1]);	// Optical Flow
						objTrack_imageView.setImage(imageToShow[2]); // Objeckt Tracking
//						System.out.println(ba.getQrText()+"testetsetse");
						String QrText = ba.getQrt();
						Platform.runLater(new Runnable(){
							@Override
							public void run() {
								// TODO Auto-generated method stub
								if(QrText!=null){
									System.out.println("Dette er test print "+QrText);
									qrt.set(QrText); // qr kode text
								}
							}
						});
					}
				};

				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, frameDt, TimeUnit.MILLISECONDS);

				if(RECORDVIDEO){	 // TESTKODE
					int fourcc = VideoWriter.fourcc('M', 'J', 'P', 'G');
					Size frameSize = new Size(640,480);
					outVideo = new VideoWriter(".\\outVideo.avi", fourcc, 15, frameSize, true);
				}

				// update the button content
				this.start_btn.setText("Stop Camera");
			}else{
				// log the error
				System.err.println("Impossible to open the camera connection...");
			}
		}else {
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.start_btn.setText("Start Camera");

			if(RECORDVIDEO){// TESTKODE	
				outVideo.release(); 			
			}
			// stop the timer
			try{
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}catch (InterruptedException e){
				// log the exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}

			// release the camera
			this.capture.release();
			// clean the frame
			this.currentFrame.setImage(null);
			this.optFlow_imageView.setImage(null);
			this.objTrack_imageView.setImage(null);
		}
	}

	private void startDroneStream(){
		if (!this.cameraActive){
			if(dc!=null){
				this.cameraActive = true;

				// grab a frame every 33 ms (30 frames/sec)
				frameGrabber = new Runnable() {
					@Override
					public void run(){
						Image imageToShow[] = ba.getImages();
						currentFrame.setImage(imageToShow[0]); // Main billede
						optFlow_imageView.setImage(imageToShow[1]); // Optical Flow
						objTrack_imageView.setImage(imageToShow[2]); // Objeckt Tracking
						int values[] = GuiController.this.dc.getFlightData();
						Platform.runLater(new Runnable(){
							@Override
							public void run() {
								// TODO Auto-generated method stub
								pitch.set(Float.toString(values[0]));
								roll.set(Float.toString(values[1]));
								yaw.set(Float.toString(values[2]));
							}
						});
					}
				};
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, frameDt, TimeUnit.MILLISECONDS);

				if(RECORDVIDEO){	 // TESTKODE
					int fourcc = VideoWriter.fourcc('M', 'J', 'P', 'G');
					Size frameSize = new Size(640,480);
					outVideo = new VideoWriter(".\\outVideo.avi", fourcc, 15, frameSize, true);
				}

				// update the button content
				this.start_btn.setText("Stop Camera");
			}
			else{
				// log the error
				System.err.println("Impossible to open the camera connection...");
			}
		} else {
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.start_btn.setText("Start Camera");

			// stop the timer
			try{
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}catch (InterruptedException e)	{
				// log the exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
			// clean the frame
			this.currentFrame.setImage(null);
			this.optFlow_imageView.setImage(null);
			this.objTrack_imageView.setImage(null);
		}
	}



//	private void findQR(Mat frame){
//		QRCodeScanner qrs = new QRCodeScanner();
//		qrs.imageUpdated(frame);
//		qr_label.setText("hej");
//	}

	@FXML
	void colorChange(ActionEvent event) {
		if(GuiStarter.GUI_DEBUG){
			System.out.println("Debug: GuiController.colorChange() kaldt! " + event.getSource().toString());
		}
		// Hvis der klikkes på greyScale_checkbox
		greyScale = !greyScale;
		ba.setGreyScale(greyScale);
	}

	@FXML
	void searchQR(ActionEvent event){
		if(GuiStarter.GUI_DEBUG){
			System.out.println("Debug: GuiController.searhQR() kaldt! " + event.getSource().toString());
		}
		// Hvis der klikkes på QR_checkbox
		qr = !qr;
		ba.setQR(qr);
		
//		if(event.getSource().equals(qr_checkBox)){
//			if(qr)
//				qr = false;
//			else
//				qr = true;
//		}
	}

	@FXML
	void goForward(ActionEvent event) {
		dc.forward();
	}

	@FXML
	void turnLeft(ActionEvent event) {
		dc.turnLeft();
	}

	@FXML
	void goBack(ActionEvent event) {
		dc.backward();
	}

	@FXML
	void turnRight(ActionEvent event) {
		dc.turnRight();
	}

	@FXML
	void takeoff(ActionEvent event) {
		if(flying){
			if(GuiStarter.GUI_DEBUG){
				System.out.println("Dronen lander!");
			}

			// Land command
			dc.land();
			flying = false;
			this.takeoff_btn.setText("Take Off");
			initButtons();
		} else {
			if(GuiStarter.GUI_DEBUG){
				System.out.println("Dronen starter!");
			}

			// take off command
			dc.takeoff();
			flying = true;
			this.takeoff_btn.setText("Land Drone");
			initButtons();
		}
	}

	@FXML
	void landdrone(ActionEvent event) {
		dc.land();
	}

	// Skifter knappers enabled tilstand afh�ngig af dronens tilstand
	private void initButtons(){
		this.stop_btn.setDisable(!flying);
		this.left_btn.setDisable(!flying);
		this.right_btn.setDisable(!flying);
		this.strafeLeft_btn.setDisable(!flying);
		this.strafeRight_btn.setDisable(!flying);
		this.forward_btn.setDisable(!flying);
		this.up_btn.setDisable(!flying);
		this.down_btn.setDisable(!flying);
		this.back_btn.setDisable(!flying);
		this.changeCam_btn.setDisable(!flying);
	}

	@FXML
	void goLeft(ActionEvent event) {
		dc.left();
	}

	@FXML
	void goRight(ActionEvent event) {
		dc.right();
	}

	@FXML
	void flyUp(ActionEvent event) {
		dc.up();
	}

	@FXML
	void flyDown(ActionEvent event) {
		dc.down();
	}

	@FXML
	void hover(ActionEvent event) {
		dc.hover();
	}

	// Skifter mellem dronens 2 kameraer
	@FXML
	void changeCam(ActionEvent event){
		if(!webcamVideo){
			
			if (TAKEPICTURE == true) {
			//Kode til at tage billede fra drone 
			TakePicture picture = new TakePicture(dc);
			picture.takePicture();
			}
			
			dc.toggleCamera();
		}
	}

	@FXML
	void setOptFlow(ActionEvent event){
		optFlow = !optFlow;
		if(GuiStarter.GUI_DEBUG){
			System.out.println("Optical Flow er sat til: " + optFlow);
		}
		ba.setOpticalFlow(optFlow);
	}

	@FXML
	void setObjectTracking(ActionEvent event){
		objTrack = !objTrack;
		if(GuiStarter.GUI_DEBUG){
			System.out.println("Object Tracking er sat til: " + objTrack);
		}
		ba.setObjTrack(objTrack);
	}

	@FXML
	void togglecam(ActionEvent event){
		if(cameraActive){
			// Kameraet kører. Derfor skal det genstartes med nyt input
			startCamera(null);
			webcamVideo = !webcamVideo;
			startCamera(null);
		} else {
			webcamVideo = !webcamVideo;
		}

		if(GuiStarter.GUI_DEBUG){
			if(webcamVideo)
				System.out.println("Kamera toggles til Webcam.");
			else
				System.out.println("Kamera toggles til Dronecam.");
		}
		ba.setWebCam(webcamVideo);
	}

	@FXML
	void setTestVideo(ActionEvent event){
		useTestVideo = !useTestVideo;
		startOpgAlgo.setDisable(useTestVideo);
		if(GuiStarter.GUI_DEBUG){
			System.err.println("Debug: Benytter testvideo: " + useTestVideo);
		}
	}


	private void recordVideo(Mat frame){
		if(frame.empty()){
			System.out.println("Tom frame :(");
		} else {
			System.out.println("Skriver til video-fil");
			outVideo.write(frame);				
		}
	}

	// Define a variable to store the property
	private StringProperty pitch = new SimpleStringProperty();
	private StringProperty yaw = new SimpleStringProperty();
	private StringProperty roll = new SimpleStringProperty();
	private StringProperty qrt = new SimpleStringProperty();

	// Define a getter for the property's value
	public final String getPitch(){return pitch.get();}
	public final String getYaw(){return yaw.get();}
	public final String getRoll(){return roll.get();}
	public final String getQrt(){return qrt.get();}

	// Define a setter for the property's value
	public final void setPitch(String value){pitch.set(value);}
	public final void setRoll(String value){roll.set(value);}
	public final void setYaw(String value){yaw.set(value);}
	public final void setQrt(String value){qrt.set(value);}

	// Define a getter for the property itself
	public StringProperty pitchProperty() {return pitch;}
	public StringProperty rollProperty() {return roll;}
	public StringProperty yawProperty() {return yaw;}
	public StringProperty qrtProperty(){return qrt;}
}