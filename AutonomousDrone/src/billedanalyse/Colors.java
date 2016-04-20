package billedanalyse;

import org.opencv.core.Scalar;

public class Colors {
	
	// Definerer den mindste størrelse (areal) et objekt skal have før det betragtes som baggrundsstøj (i pixels)
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
	
	// Farve til at identificere røde bolde med Jon's Webcam
	public static final Scalar hsvMinRedWebcam = new Scalar(0, 245, 70);
	public static final Scalar hsvMaxRedWebcam = new Scalar(180, 255, 160);

	// Farve til at identificere røde bolde med det fremadrettede DroneCam
	public static final Scalar hsvMinRedDrone = new Scalar(0, 130, 90);
	public static final Scalar hsvMaxRedDrone = new Scalar(20, 255, 255);
	
	// Farve til at identificere røde bolde med det nedadrettede DroneCam
	public static final Scalar hsvMinRedDroneDown = new Scalar(168, 165, 127);
	public static final Scalar hsvMaxRedDroneDown = new Scalar(180, 255, 255);

	public static Scalar hsvMaxRedDrone2 = hsvMaxRedDroneDown;
	public static Scalar hsvMinRedDrone2 = hsvMinRedDroneDown;

	public static Scalar hsvMinRedDroneDown2 = hsvMinRedDrone;
	public static Scalar hsvMaxRedDroneDown2 = hsvMaxRedDrone;


	
//	// Farve til at identificere grønne bolde med Jon's Webcam
//	public static final Scalar hsvMinGreenWebcam = new Scalar(34, 182, 0);
//	public static final Scalar hsvMaxGreenWebcam = new Scalar(50, 255, 75);
//	
//	// Farve til at identificere grønne bolde med det fremadrettede DroneCam
//	public static final Scalar hsvMinGreenDrone = new Scalar(54, 88, 48);
//	public static final Scalar hsvMaxGreenDrone = new Scalar(74, 145, 200);
//	
//	// Farve til at identificere grønne bolde med det nedadrettede DroneCam
//	public static final Scalar hsvMinGreenDroneDown = new Scalar(45, 94, 90);
//	public static final Scalar hsvMaxGreenDroneDown = new Scalar(65, 255, 214);
//	
//	// Farve til at identificere røde bolde med Jon's Webcam
//	public static final Scalar hsvMinRedWebcam = new Scalar(0, 245, 70);
//	public static final Scalar hsvMaxRedWebcam = new Scalar(180, 255, 160);
//
//	// Farve til at identificere røde bolde med det fremadrettede DroneCam
//	public static final Scalar hsvMinRedDrone = new Scalar(0, 157, 113);
//	public static final Scalar hsvMaxRedDrone = new Scalar(18, 255, 255);
//	
//	// Farve til at identificere røde bolde med det nedadrettede DroneCam
//	public static final Scalar hsvMinRedDroneDown = new Scalar(161, 177, 185);
//	public static final Scalar hsvMaxRedDroneDown = new Scalar(180, 255, 255);
//
//	public static Scalar hsvMinRed = hsvMinRedDroneDown;
//	public static Scalar hsvMaxRed = hsvMaxRedDroneDown;
}
