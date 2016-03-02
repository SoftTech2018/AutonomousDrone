package drone;

import java.awt.image.BufferedImage;

import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.CommandManager;
import de.yadrone.base.navdata.AttitudeListener;
import de.yadrone.base.navdata.NavDataManager;
import de.yadrone.base.video.ImageListener;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

public class DroneControl implements IDroneControl {

	private IARDrone drone;
	private CommandManager cmd;
	private NavDataManager ndm;
	private final int SPEED = 10; /* % */
	private final int MINALT = 1000; /* mm */
	private final int MAXALT = 2500; /* mm */
	private final int DURATION = 10; /* ms */
	private WritableImage imageOutput;
	private BufferedImage bufImgOut;
	private float pitch, yaw, roll;

	protected static final boolean DRONE_DEBUG = false;

	/*
	 * DEFINERER OM DRONEN BRUGER .doFor(time)
	 */
	public static boolean TIMEMODE = false;


	/*
	 *  DEFINERER TEST-MODE. 
	 *  SÆT TIL TRUE NÅR DER TESTES UDEN DRONE!
	 *  SÆT TIL FALSE NÅR DER TESTES MED DRONE!
	 */
	private final boolean TEST_MODE = false;

	public DroneControl() {
		pitch = 0;
		yaw = 0;
		roll = 0;
		if(!TEST_MODE){			
			drone = new ARDrone();
			drone.start();
			cmd = drone.getCommandManager();
			ndm = drone.getNavDataManager();
			cmd.setMinAltitude(MINALT);
			cmd.setMaxAltitude(MAXALT);		
			imageCapture();	
			startNavListener();
		}
	}

	private void startNavListener(){
		ndm.addAttitudeListener(new AttitudeListener(){
			@Override
			public void attitudeUpdated(float pitch, float roll) {
				DroneControl.this.pitch = pitch;
				DroneControl.this.roll = roll;
			}
			@Override
			public void attitudeUpdated(float pitch, float roll, float yaw) {
				DroneControl.this.pitch = pitch;
				DroneControl.this.roll = roll;
				DroneControl.this.yaw = yaw;
			}
			@Override
			public void windCompensation(float arg0, float arg1) {	}
		});
	}

	private void imageCapture(){	
		drone.getVideoManager().addImageListener(new ImageListener() {			
			@Override
			public void imageUpdated(BufferedImage arg0) {		
				if(DRONE_DEBUG){
					System.out.println("Billede modtaget fra drone. Højde: " + arg0.getHeight() + ", Bredde: " + arg0.getWidth());
				}
				bufImgOut = arg0;
				imageOutput = javafx.embed.swing.SwingFXUtils.toFXImage(arg0, imageOutput);	
			}		
		});
	}

	@Override
	public Image getImage(){
		return (Image) imageOutput;
	}
	
	@Override
	public BufferedImage getbufImg(){
		return bufImgOut;
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#land()
	 */
	@Override
	public void land(){
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Lander");
		}
		cmd.landing();
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#takeoff()
	 */
	@Override
	public void takeoff(){
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Starter");
		}
		cmd.takeOff();
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#hover()
	 */
	@Override
	public void hover(){
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Hover");
		}
		cmd.hover();
	}	

	/* (non-Javadoc)
	 * @see drone.IDroneControl#up()
	 */
	@Override
	public void up(){
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Flyver Op");
		}
		if(TIMEMODE){
			cmd.up(SPEED);
		} else {
			cmd.up(SPEED).doFor(DURATION);			
		}

		//		cmd.hover();
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#down()
	 */
	@Override
	public void down(){
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Flyver Ned");
		}
		if(TIMEMODE){
			cmd.down(SPEED);
		} else {
			cmd.down(SPEED).doFor(DURATION);			
		}
		//		cmd.hover();
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#forward()
	 */
	@Override
	public void forward(){
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Flyver Fremad");
		}
		if(TIMEMODE){
			cmd.forward(SPEED);
		} else {
			cmd.forward(SPEED).doFor(DURATION);			
		}
		//		cmd.hover();
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#backward()
	 */
	@Override
	public void backward(){
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Flyver Baglæns");
		}
		if(TIMEMODE){
			cmd.backward(SPEED);
		} else {
			cmd.backward(SPEED).doFor(DURATION);			
		}
		//		cmd.hover();
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#left()
	 */
	@Override
	public void left(){
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Flyver Venstre");
		}
		if(TIMEMODE){
			cmd.goLeft(SPEED);
		} else {
			cmd.goLeft(SPEED).doFor(DURATION);			
		}
		//		cmd.hover();
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#right()
	 */
	@Override
	public void right(){
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Flyver Højre");
		}
		if(TIMEMODE){
			cmd.goRight(SPEED);
		} else {
			cmd.goRight(SPEED).doFor(DURATION);			
		}
		//		cmd.hover();
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#turnLeft()
	 */
	@Override
	public void turnLeft(){
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Drejer Venstre");
		}
		if(TIMEMODE){
			cmd.spinLeft(SPEED);
		} else {
			cmd.spinLeft(SPEED).doFor(DURATION);			
		}
		//		cmd.hover();
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#turnRight()
	 */
	@Override
	public void turnRight(){
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Drejer Højre");
		}
		if(TIMEMODE){
			cmd.spinRight(SPEED);
		} else {
			cmd.spinRight(SPEED).doFor(DURATION);			
		}
		//		cmd.hover();
	}

	@Override
	public void toggleCamera(){
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Skifter Kamera");
		}
		drone.toggleCamera();
	}

	/**
	 * Metoden tjekker om dronen er klar til take off
	 */
	@Override
	public boolean isReady() {
		if(cmd==null){
			if(DRONE_DEBUG){
				System.out.println("DroneControl: Dronen er IKKE klar.");
			}
			return false;
		}

		if(DRONE_DEBUG){
			System.out.println("DroneControl: Dronen er klar.");
		}
		return true;
	}

	@Override
	public float[] getFlightData() {
		if(DRONE_DEBUG){
			System.out.println("Pitch: " + pitch + ", Roll: " + roll + ", Yaw: " + yaw);
		}
		float out[] = {pitch, roll, yaw};
		return out;
	}
}
