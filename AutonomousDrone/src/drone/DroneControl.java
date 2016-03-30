package drone;

import java.awt.image.BufferedImage;

import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.CommandManager;
import de.yadrone.base.command.LEDAnimation;
import de.yadrone.base.navdata.AttitudeListener;
import de.yadrone.base.navdata.NavDataManager;
import de.yadrone.base.video.ImageListener;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

public class DroneControl implements IDroneControl {
	
	// Skal debug beskeder udskrives?
	protected static final boolean DRONE_DEBUG = false;

	//DEFINERER OM DRONEN BRUGER .doFor(time)
	private boolean timeMode = false;

	/*
	 *  DEFINERER TEST-MODE. 
	 *  SÆT TIL TRUE NÅR DER TESTES UDEN DRONE!
	 *  SÆT TIL FALSE NÅR DER TESTES MED DRONE!
	 */
	private final boolean TEST_MODE = true;

	private IARDrone drone;
	private CommandManager cmd;
	private NavDataManager ndm;
	
	//TurnLeft og TurnRight ganges med faktor 8-10, up og down ganges med faktor 5
	private final int SPEED = 10; /* % */ 
	private final int MINALT = 1000; /* mm */
	private final int MAXALT = 2500; /* mm */
	private final int DURATION = 10; /* ms */
//	private WritableImage imageOutput;
	private BufferedImage bufImgOut;
	private int pitch, yaw, roll;

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
				DroneControl.this.pitch = (int) pitch/1000;
				DroneControl.this.roll = (int) roll/1000;
			}
			@Override
			public void attitudeUpdated(float pitch, float roll, float yaw) {
				DroneControl.this.pitch = (int) pitch/1000;
				DroneControl.this.roll = (int) roll/1000;
				DroneControl.this.yaw = (int) yaw/1000;
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
//				imageOutput = javafx.embed.swing.SwingFXUtils.toFXImage(arg0, imageOutput);	
			}		
		});
	}

//	@Override
//	public Image getImage(){
//		return (Image) imageOutput;
//	}
	
	@Override
	public BufferedImage getbufImg(){
		System.err.println("*** Billede hentet fra DroneControl.");
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
		drone.getCommandManager().setLedsAnimation(LEDAnimation.BLINK_ORANGE, 3, 10);
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
		drone.getCommandManager().setLedsAnimation(LEDAnimation.BLINK_ORANGE, 3, 10);
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
	
	@Override
	public void setTimeMode(boolean timeMode){
		this.timeMode = timeMode;
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#up()
	 */
	@Override
	public void up(){
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Flyver Op");
		}
		if(timeMode){
			cmd.up(SPEED*5);
		} else {
			cmd.up(SPEED*5).doFor(DURATION);			
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
		if(timeMode){
			cmd.down(SPEED*5);
		} else {
			cmd.down(SPEED*5).doFor(DURATION);			
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
		if(timeMode){
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
		if(timeMode){
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
		if(timeMode){
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
		if(timeMode){
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
		if(timeMode){
			cmd.spinLeft(SPEED*8);
		} else {
			cmd.spinLeft(SPEED*8).doFor(DURATION);			
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
		if(timeMode){
			cmd.spinRight(SPEED*10);
		} else {
			cmd.spinRight(SPEED*10).doFor(DURATION);			
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
	public int[] getFlightData() {
		if(DRONE_DEBUG){
			System.out.println("Pitch: " + pitch + ", Roll: " + roll + ", Yaw: " + yaw);
		}
		int out[] = {pitch, roll, yaw};
		return out;
	}
	
	@Override
	public void setFps(int fps){
		if(cmd!=null){			
		cmd.setVideoCodecFps(fps);
		}
	}
	
	@Override
	public void setLedAnim(LEDAnimation anim, int freq, int dur){
		drone.getCommandManager().setLedsAnimation(anim, freq, dur);
	}
}
