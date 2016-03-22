package drone;

import java.awt.image.BufferedImage;

import de.yadrone.base.command.LEDAnimation;
import javafx.scene.image.Image;

public interface IDroneControl {

	void land();

	void takeoff();

	void hover();

	void up();

	void down();

	void forward();

	void backward();

	void left();

	void right();

	void turnLeft();

	void turnRight();

	Image getImage();

	void toggleCamera();

	boolean isReady();

	int[] getFlightData();

	BufferedImage getbufImg();

	void setFps(int fps);

	void setLedAnim(LEDAnimation anim, int freq, int dur);
}