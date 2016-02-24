package drone;

import java.awt.image.BufferedImage;

import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.CommandManager;
import de.yadrone.base.navdata.NavDataManager;
import de.yadrone.base.video.ImageListener;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

public class DroneControl implements IDroneControl {

	private IARDrone drone;
	private CommandManager cmd;
	private NavDataManager ndm;
	private final int SPEED = 30; /* % */
	private final int MINALT = 1000; /* mm */
	private final int MAXALT = 2500; /* mm */
	private final int DURATION = 10; /* ms */
	private WritableImage imageOutput;
	
	protected static final boolean DRONE_DEBUG = true;
	
	/*
	 *  DEFINERER TEST-MODE. 
	 *  SÆT TIL TRUE NÅR DER TESTES UDEN DRONE!
	 *  SÆT TIL FALSE NÅR DER TESTES MED DRONE!
	 */
	private final boolean TEST_MODE = true;
	
	public DroneControl() {
		if(!TEST_MODE){			
		drone = new ARDrone();
		drone.start();
		cmd = drone.getCommandManager();
		ndm = drone.getNavDataManager();
		cmd.setMinAltitude(MINALT);
		cmd.setMaxAltitude(MAXALT);		
		imageCapture();		
		}
	}
	
	private void imageCapture(){	
		drone.getVideoManager().addImageListener(new ImageListener() {			
			@Override
			public void imageUpdated(BufferedImage arg0) {		
				if(DRONE_DEBUG){
					System.out.println("Billede modtaget fra drone. Højde: " + arg0.getHeight() + ", Bredde: " + arg0.getWidth());
				}
				imageOutput = javafx.embed.swing.SwingFXUtils.toFXImage(arg0, imageOutput);	
			}		
		});
	}
	
	@Override
	public Image getImage(){
		return (Image) imageOutput;
	}
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#land()
	 */
	@Override
	public void land(){
		cmd.landing();
	}
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#takeoff()
	 */
	@Override
	public void takeoff(){
		cmd.takeOff();
	}
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#hover()
	 */
	@Override
	public void hover(){
		cmd.hover();
	}	
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#up()
	 */
	@Override
	public void up(){
		cmd.up(SPEED).doFor(DURATION);
//		cmd.hover();
	}
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#down()
	 */
	@Override
	public void down(){
		cmd.down(SPEED).doFor(DURATION);
//		cmd.hover();
	}
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#forward()
	 */
	@Override
	public void forward(){
		cmd.forward(SPEED).doFor(DURATION);
//		cmd.hover();
	}
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#backward()
	 */
	@Override
	public void backward(){
		cmd.backward(SPEED).doFor(DURATION);
//		cmd.hover();
	}
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#left()
	 */
	@Override
	public void left(){
		cmd.goLeft(SPEED).doFor(DURATION);
//		cmd.hover();
	}
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#right()
	 */
	@Override
	public void right(){
		cmd.goRight(SPEED).doFor(DURATION);
//		cmd.hover();
	}
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#turnLeft()
	 */
	@Override
	public void turnLeft(){
		cmd.spinLeft(SPEED).doFor(DURATION);
//		cmd.hover();
	}
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#turnRight()
	 */
	@Override
	public void turnRight(){
		cmd.spinRight(SPEED).doFor(DURATION);
//		cmd.hover();
	}

	@Override
	public void toggleCamera(){
		drone.toggleCamera();
	}

}
