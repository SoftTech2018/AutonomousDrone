package drone;

import java.awt.image.BufferedImage;

import de.yadrone.base.command.LEDAnimation;
import javafx.scene.image.Image;

public interface IDroneControl {

	void land();

	void takeoff();

	void hover();

	void up() throws InterruptedException;

	void down() throws InterruptedException;
	
	void up2() throws InterruptedException;

	void down2() throws InterruptedException;

	void forward() throws InterruptedException;

	void backward() throws InterruptedException;

	void left() throws InterruptedException;

	void right() throws InterruptedException;

	void turnLeft() throws InterruptedException;

	void turnRight() throws InterruptedException;

//	Image getImage();

	void toggleCamera();

	boolean isReady();

	int[] getFlightData();

	BufferedImage getbufImg();

	void setFps(int fps);

	void setLedAnim(LEDAnimation anim, int freq, int dur);

	void setTimeMode(boolean timeMode);

	void turnDrone(double rotVinkel);

	void flyDrone(double dist);

	void strafeRight(int i);

	void strafeLeft(int i);

	void turnDroneTo(int startYaw);

	void setYawCorrection(double calcYaw);

}