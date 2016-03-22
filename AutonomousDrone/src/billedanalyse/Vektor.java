package billedanalyse;

import org.opencv.core.Point;

public class Vektor {
	
	private Point x;
	private Point y;
	
	public Vektor(Point x, Point y){
		this.x = x;
		this.y = y;
	}

	public Point getX() {
		return x;
	}

	public void setX(Point x) {
		this.x = x;
	}

	public Point getY() {
		return y;
	}

	public void setY(Point y) {
		this.y = y;
	}
	
	public double getLength(){
		return Math.sqrt(Math.pow((x.x - y.x),2) + Math.pow((x.y - y.y),2));
	}
	
	public double distance(double degrees){
		return Math.abs(getLength()/Math.sin(degrees));
	}
	
}
