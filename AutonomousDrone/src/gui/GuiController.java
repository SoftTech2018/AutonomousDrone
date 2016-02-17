package gui;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class GuiController {
	
	@FXML
	private Button start_btn;

    @FXML
    private ImageView currentFrame;
    
    @FXML
    private CheckBox grey_checkBox;
    
	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that realizes the video capture
	private VideoCapture capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive = false;
	// a flag to enable/disable greyscale colors
	private boolean greyScale = false;

	@FXML
	void startCamera(ActionEvent event) {
		if(GuiStarter.DEBUG){
			System.out.println("Debug: GuiController.startCamera() kaldt! " + event.getSource().toString());
		}
		
		if (!this.cameraActive)
		{
			// start the video capture
			this.capture.open(0);
			
			// is the video stream available?
			if (this.capture.isOpened())
			{
				this.cameraActive = true;
				
				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable() {
					
					@Override
					public void run()
					{
						Image imageToShow = grabFrame();
						currentFrame.setImage(imageToShow);
					}
				};

				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				
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
			this.capture.release();
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
		if (this.capture.isOpened())
		{
			try
			{
				// read the current frame
				this.capture.read(frame);
				
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

}