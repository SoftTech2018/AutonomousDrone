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

	IARDrone drone;
	CommandManager cmd;
	NavDataManager ndm;
	final int speed = 30;
	final int minAlt = 1000;
	final int maxAlt = 3000;
	final int duration = 100;
	private WritableImage imageOutput;
	
	public DroneControl() {
		
		drone = new ARDrone();
		drone.start();
		cmd = drone.getCommandManager();
		ndm = drone.getNavDataManager();
		cmd.setMinAltitude(minAlt);
		cmd.setMaxAltitude(maxAlt);
		
		startImageCapture();		
	}
	
	private void startImageCapture(){
		
//		drone.toggleCamera();
		
		drone.getVideoManager().addImageListener(new ImageListener() {			
			@Override
			public void imageUpdated(BufferedImage arg0) {
				javafx.embed.swing.SwingFXUtils.toFXImage(arg0, imageOutput);				
			}		
		});
	}
	
	@Override
	public Image getImage(){
		startImageCapture();
		Image output = (Image) imageOutput;
		return output;
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
		cmd.up(speed).doFor(duration);
		cmd.hover();
	}
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#down()
	 */
	@Override
	public void down(){
		cmd.down(speed).doFor(duration);
		cmd.hover();
	}
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#forward()
	 */
	@Override
	public void forward(){
		cmd.forward(speed).doFor(duration);
		cmd.hover();
	}
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#backward()
	 */
	@Override
	public void backward(){
		cmd.backward(speed).doFor(duration);
		cmd.hover();
	}
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#left()
	 */
	@Override
	public void left(){
		cmd.goLeft(speed).doFor(duration);
		cmd.hover();
	}
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#right()
	 */
	@Override
	public void right(){
		cmd.goRight(speed).doFor(duration);
		cmd.hover();
	}
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#turnLeft()
	 */
	@Override
	public void turnLeft(){
		cmd.spinLeft(speed).doFor(duration);
		cmd.hover();
	}
	
	/* (non-Javadoc)
	 * @see drone.IDroneControl#turnRight()
	 */
	@Override
	public void turnRight(){
		cmd.spinRight(speed).doFor(duration);
		cmd.hover();
	}


}
