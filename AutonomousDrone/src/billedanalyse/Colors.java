package billedanalyse;

import org.opencv.core.Scalar;

public class Colors {
	
	public static final int MIN_OBJECT_AREA = 20 * 20;
	
	// Farve til at identificere grønne bolde med Jon's Webcam
	public static final Scalar hsvMinGreenWebcam = new Scalar(34, 182, 0);
	public static final Scalar hsvMaxGreenWebcam = new Scalar(50, 255, 75);
	
	// Farve til at identificere grønne bolde med det fremadrettede DroneCam
	public static final Scalar hsvMinGreenDrone = new Scalar(54, 88, 48);
	public static final Scalar hsvMaxGreenDrone = new Scalar(74, 145, 200);
	
	// Farve til at identificere grønne bolde med det nedadrettede DroneCam
	public static final Scalar hsvMinGreenDroneDown = new Scalar(45, 94, 90);
	public static final Scalar hsvMaxGreenDroneDown = new Scalar(65, 255, 214);

}
