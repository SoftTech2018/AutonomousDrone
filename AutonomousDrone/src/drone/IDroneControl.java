package drone;

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

}