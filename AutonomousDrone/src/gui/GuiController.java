package gui;

import java.awt.TextField;
import java.io.ByteArrayInputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import drone.DroneControl;
import drone.IDroneControl;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class GuiController {
	
	IDroneControl dc;
	
	public GuiController() {
		dc = new DroneControl();
	}


	// NUMPAD 7
	@FXML
	private Button strafeLeft_btn;

	// START CAMERA BUTTON
	@FXML
	private Button start_btn;

	// NUMPAD 1
	@FXML
	private Button up_btn;

	// NUMPAD 2
	@FXML
	private Button stop_btn;

	@FXML
	private CheckBox grey_checkBox;

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
	
	// ??
	@FXML
	private Button landdrone_btn;

	@FXML
	private ImageView currentFrame;

	@FXML
	private ChoiceBox<Integer> frames_choiceBox;

	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that realizes the video capture
//	private VideoCapture capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive = false;
	// a flag to enable/disable greyscale colors
	private boolean greyScale = false;
	// Antal ms mellem hver frame (33 ms = 30 fps)
	private int frameDt = 33;
	// Objekt der bruges til at opdatere billedet på GUI
	private Runnable frameGrabber;
	// Liste af valgmuligheder i GUI til frames per second
	private ObservableList<Integer> frameChoicesList = FXCollections.observableArrayList(15, 30, 60, 120);
	// Flyver dronen?
	private boolean flying = false;
	
	private TextField roll_txtfield;
	
	private TextField yaw_txtfield;
	
	private TextField pitch_txtfield;

	@FXML
	private void initialize(){
		frames_choiceBox.setValue(30);
		frames_choiceBox.setItems(frameChoicesList);
		frames_choiceBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>(){
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
				frameDt = 1000/frameChoicesList.get((Integer) arg2);

				if(GuiStarter.DEBUG){
					System.out.println("Debug: GuiController.frames_choiceBox changelistener kaldt! Frames er: " + frameDt);
				}
				if(GuiController.this.timer!=null){					
					GuiController.this.timer.shutdown();
					GuiController.this.timer = Executors.newSingleThreadScheduledExecutor();
					GuiController.this.timer.scheduleAtFixedRate(GuiController.this.frameGrabber, 0, frameDt, TimeUnit.MILLISECONDS);
				}
			}
		});
	}

	@FXML
	void startCamera(ActionEvent event) {
		if(GuiStarter.DEBUG){
			System.out.println("Debug: GuiController.startCamera() kaldt! " + event.getSource().toString());
		}

		if (!this.cameraActive)
		{
			// start the video capture
//			this.capture.open(0);

			// is the video stream available?
//			if (this.capture.isOpened())
			if(dc!=null)
			{
				this.cameraActive = true;

				// grab a frame every 33 ms (30 frames/sec)
				frameGrabber = new Runnable() {

					@Override
					public void run()
					{
//						Image imageToShow = GuiController.this.grabFrame();
						Image imageToShow = dc.getImage();
						currentFrame.setImage(imageToShow);
					}
				};

				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, frameDt, TimeUnit.MILLISECONDS);

				// update the button content
				this.start_btn.setText("Stop Camera");
			}
			else
			{
				// log the error
				System.err.println("Impossible to open the camera connection...");
			}
		}
		else
		{
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.start_btn.setText("Start Camera");

			// stop the timer
			try
			{
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				// log the exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}

			// release the camera
//			this.capture.release();
			// clean the frame
			this.currentFrame.setImage(null);
		}
	}

	/**
	 * Get a frame from the opened video stream (if any)
	 * 
	 * @return the {@link Image} to show
	 */
	private Image grabFrame()
	{
		// init everything
		Image imageToShow = null;
		Mat frame = new Mat();

		// check if the capture is open
//		if (this.capture.isOpened())
		if(dc!=null)
		{
			try
			{
				// read the current frame
//				this.capture.read(frame);

				// if the frame is not empty, process it
				if (!frame.empty())
				{
					if(greyScale){						
						// convert the image to gray scale
						Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
					}

					// convert the Mat object (OpenCV) to Image (JavaFX)
					imageToShow = mat2Image(frame);
				}

			}
			catch (Exception e)
			{
				// log the error
				System.err.println("Exception during the image elaboration: " + e);
			}
		}

		return imageToShow;
	}

	/**
	 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
	 * 
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @return the {@link Image} to show
	 */
	private Image mat2Image(Mat frame)
	{
		// create a temporary buffer
		MatOfByte buffer = new MatOfByte();
		// encode the frame in the buffer
		Imgcodecs.imencode(".png", frame, buffer);
		// build and return an Image created from the image encoded in the
		// buffer
		return new Image(new ByteArrayInputStream(buffer.toArray()));
	}


	@FXML
	void colorChange(ActionEvent event) {
		if(GuiStarter.DEBUG){
			System.out.println("Debug: GuiController.colorChange() kaldt! " + event.getSource().toString());
		}

		// Hvis der klikkes på greyScale_checkbox
		if(event.getSource().equals(grey_checkBox)){
			if(greyScale)
				greyScale = false;
			else
				greyScale = true;
		}
	}

	@FXML
	void goForward(ActionEvent event) {

	}

	@FXML
	void turnLeft(ActionEvent event) {

	}

	@FXML
	void goBack(ActionEvent event) {

	}

	@FXML
	void turnRight(ActionEvent event) {

	}

	@FXML
	void takeoff(ActionEvent event) {
		if(flying){
			if(GuiStarter.DEBUG){
				System.out.println("Dronen lander!");
			}

			// Land command
			flying = false;
			this.takeoff_btn.setText("Take Off");
			initButtons();
		} else {
			if(GuiStarter.DEBUG){
				System.out.println("Dronen starter!");
			}

			// take off command
			flying = true;
			this.takeoff_btn.setText("Land Drone");
			initButtons();
		}
	}
	
	@FXML
	void landdrone(ActionEvent event) {
		
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
		this.landdrone_btn.setDisable(!flying);
	}

	@FXML
	void goLeft(ActionEvent event) {

	}

	@FXML
	void goRight(ActionEvent event) {

	}

	@FXML
	void flyUp(ActionEvent event) {

	}

	@FXML
	void flyDown(ActionEvent event) {

	}

	@FXML
	void hoover(ActionEvent event) {

	}

	// Metoden tjekker om dronen pt flyver eller ej.
	private boolean isFlying(){
		return false;
	}

	public void landDrone() {
		if(flying){
			takeoff(null);
		}
	}

}