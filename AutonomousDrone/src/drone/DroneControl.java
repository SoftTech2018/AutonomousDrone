package drone;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.CommandManager;
import de.yadrone.base.command.LEDAnimation;
import de.yadrone.base.command.VideoChannel;
import de.yadrone.base.command.VideoCodec;
import de.yadrone.base.navdata.Altitude;
import de.yadrone.base.navdata.AltitudeListener;
import de.yadrone.base.navdata.AttitudeListener;
import de.yadrone.base.navdata.MagnetoData;
import de.yadrone.base.navdata.MagnetoListener;
import de.yadrone.base.navdata.NavDataManager;
import de.yadrone.base.video.ImageListener;
import diverse.Log;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

public class DroneControl implements IDroneControl {
	
	// Skal debug beskeder udskrives?
	protected static final boolean DRONE_DEBUG = false;

	//DEFINERER OM DRONEN BRUGER .doFor(time)
	private boolean timeMode = true;

	/*
	 *  DEFINERER TEST-MODE. 
	 *  SÆT TIL TRUE NÅR DER TESTES UDEN DRONE!
	 *  SÆT TIL FALSE NÅR DER TESTES MED DRONE!
	 */
	private final boolean TEST_MODE = false;

	private IARDrone drone;
	private CommandManager cmd;
	private NavDataManager ndm;
	
	//TurnLeft og TurnRight ganges med faktor 8-10, up og down ganges med faktor 5
	private final int SPEED = 20; /* % */ 
	private final int MINALT = 1000; /* mm */
	private final int MAXALT = 3000; /* mm */
	private final int DURATION = 5000; /* ms */
//	private WritableImage imageOutput;
	private BufferedImage bufImgOut;
	private int pitch, yaw, roll, altitude;

	public DroneControl() {
		pitch = 0;
		yaw = 0;
		roll = 0;
		altitude = 0;
		if(!TEST_MODE){			
			drone = new ARDrone();
			drone.start();
			cmd = drone.getCommandManager();
			ndm = drone.getNavDataManager();
			cmd.setMinAltitude(MINALT);
			cmd.setMaxAltitude(MAXALT);		
			imageCapture();	
			startNavListener();
			cmd.setVideoCodec(VideoCodec.H264_720P);
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
//		ndm.addMagnetoListener(new MagnetoListener(){
//
//			@Override
//			public void received(MagnetoData arg0) {
//				System.err.println("MagnetoData: " + arg0);
//			}
//		});
		
		// Nedenstående giver altid 0
//		ndm.addAltitudeListener(new AltitudeListener(){
//			@Override
//			public void receivedAltitude(int arg0) {
//				System.out.println("Højde er: " + arg0);
//				DroneControl.this.altitude = (int) arg0 / 1000;
//			}
//
//			@Override
//			public void receivedExtendedAltitude(Altitude arg0) {
//				DroneControl.this.altitude = (int) arg0.getRaw() / 1000;
//				System.out.println("Højde2 er: " + DroneControl.this.altitude);
//			}
//		});
	}

	private void imageCapture(){	
		drone.getVideoManager().addImageListener(new ImageListener() {			
			@Override
			public void imageUpdated(BufferedImage arg0) {		
				if(DRONE_DEBUG){
					System.out.println("Billede modtaget fra drone. Højde: " + arg0.getHeight() + ", Bredde: " + arg0.getWidth());
				}
				DroneControl.this.setImg(arg0);
//				System.out.println("Billede dimensioner: " + arg0.getHeight() + "," + arg0.getWidth());
//				bufImgOut = arg0;
//				imageOutput = javafx.embed.swing.SwingFXUtils.toFXImage(arg0, imageOutput);	
			}		
		});
	}

//	@Override
//	public Image getImage(){
//		return (Image) imageOutput;
//	}
	
	private synchronized void setImg(BufferedImage img){
//		System.err.println("Billede opdateret.");
		bufImgOut = img;
	}
	
	@Override
	public synchronized BufferedImage getbufImg(){	
		if(bufImgOut==null){
			return null;
		}
		ColorModel cm = bufImgOut.getColorModel();
	    boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
	    WritableRaster raster = bufImgOut.copyData(bufImgOut.getRaster().createCompatibleWritableRaster());
	    BufferedImage out = new BufferedImage(cm, raster, isAlphaPremultiplied, null);
		bufImgOut = null;
		return out;
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
		cmd.flatTrim();
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
	public void up() throws InterruptedException{
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Flyver Op");
		}
		if(!timeMode){
			cmd.up(SPEED*3);
		} else {
			cmd.up(SPEED*3).doFor(DURATION);
			Thread.sleep(DURATION);
		}

		//		cmd.hover();
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#down()
	 */
	@Override
	public void down() throws InterruptedException{
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Flyver Ned");
		}
		if(!timeMode){
			cmd.down(SPEED*3);
		} else {
			cmd.down(SPEED*3).doFor(DURATION);		
			Thread.sleep(DURATION);
		}
		//		cmd.hover();
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#forward()
	 */
	@Override
	public void forward() throws InterruptedException{
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Flyver Fremad");
		}
		if(!timeMode){
			cmd.forward(SPEED);
		} else {
			cmd.forward(SPEED).doFor(DURATION);		
			Thread.sleep(DURATION);
		}
		//		cmd.hover();
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#backward()
	 */
	@Override
	public void backward() throws InterruptedException{
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Flyver Baglæns");
		}
		if(!timeMode){
			cmd.backward(SPEED);
		} else {
			cmd.backward(SPEED).doFor(DURATION);	
			Thread.sleep(DURATION);
		}
		//		cmd.hover();
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#left()
	 */
	@Override
	public void left() throws InterruptedException{
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Flyver Venstre");
		}
		if(!timeMode){
			cmd.goLeft(SPEED);
		} else {
			cmd.goLeft(SPEED).doFor(DURATION);	
			Thread.sleep(DURATION);
		}
		//		cmd.hover();
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#right()
	 */
	@Override
	public void right() throws InterruptedException{
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Flyver Højre");
		}
		if(!timeMode){
			cmd.goRight(SPEED);
		} else {
			cmd.goRight(SPEED).doFor(DURATION);			
			Thread.sleep(DURATION);
		}
		//		cmd.hover();
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#turnLeft()
	 */
	@Override
	public void turnLeft() throws InterruptedException{
//		this.turnDrone(-90);
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Drejer Venstre");
		}
		if(!timeMode){
			cmd.spinLeft(SPEED*4);
		} else {
			cmd.spinLeft(SPEED*4); //.doFor(DURATION);	
			Thread.sleep(DURATION);
		}
//				cmd.hover();
	}

	/* (non-Javadoc)
	 * @see drone.IDroneControl#turnRight()
	 */
	@Override
	public void turnRight() throws InterruptedException{
		if(DRONE_DEBUG){
			System.out.println("DroneControl: Drejer Højre");
		}
		if(!timeMode){
			cmd.spinRight(SPEED*4);
		} else {
			cmd.spinRight(SPEED*4); //.doFor(DURATION);	
			Thread.sleep(DURATION);
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

	@Override
	public void turnDrone(double rotVinkel) {
		final int FACTOR = 13; //Tidsfaktor
		final int VINKELFEJL = 5; // Fejlmargin i sigte
		
		// Find YAW-værdien som dronen bør have når den peger på målet
		int targetYaw = (int) (this.yaw + rotVinkel);
		if(targetYaw>180){
			targetYaw = 360 - targetYaw;
		} else if(targetYaw<-180){
			targetYaw = 360 + targetYaw;
		}
		
		if(rotVinkel < 0){// Drej til venstre
			cmd.spinLeft(SPEED*4).doFor((long) Math.abs((FACTOR*rotVinkel)/4));
		} else {// Drej til højre
			cmd.spinRight(SPEED*4).doFor((long) Math.abs((FACTOR*rotVinkel)/4));
		}
		
		// Korrektion, finjustering af sigte
		int vinkel;
		while((vinkel = (targetYaw-this.yaw)) > VINKELFEJL || vinkel < -VINKELFEJL){
			if(vinkel<0){ // Vi skal dreje til venstre
				cmd.spinLeft(SPEED*4).doFor(FACTOR);
			} else if(vinkel>0){ // Vi skal dreje til højre
				cmd.spinRight(SPEED*4).doFor(FACTOR);
			}
			System.err.println("Justerer vinkel: " + vinkel);
		}
		Log.writeLog("Roteret dronen til YAW: " + targetYaw + " - Resultat: " + this.yaw);
		cmd.hover();
	}

	@Override
	public void flyDrone(double dist) {
		final double FACTOR = 10; //Tidsfaktor
		if(dist>0){
			cmd.forward(SPEED).doFor((long) (FACTOR*dist));				
		} else {
			cmd.backward(SPEED).doFor((long) (FACTOR*dist));
		}
	}

	@Override
	public void strafeRight(int dist) {
		final double FACTOR = 1.1; 
		cmd.goRight(SPEED).doFor((long) (FACTOR*dist));
	}

	@Override
	public void strafeLeft(int dist) {
		final double FACTOR = 1.1; 
		cmd.goLeft(SPEED).doFor((long) (FACTOR*dist));
	}

	@Override
	public void turnDroneTo(int targetYaw) {
		this.turnDrone(targetYaw-this.yaw);
	}
	

}
