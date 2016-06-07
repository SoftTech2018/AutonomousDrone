package diverse;

import org.opencv.core.Point;

import diverse.koordinat.Koordinat;

public class QrFirkant {

	private Point p0, p1, p2, p3;
	private String text;
	private Koordinat placering;

	public QrFirkant(Point p0, Point p1, Point p2, Point p3){
		this.p0 = p0;
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
	}
	
	public double deltaX(){
		double xKor = (p0.x+p1.x+p2.x+p3.x)/4;
//		double yKor = (p0.y+p1.y+p2.y+p3.y)/4;
		return xKor - 360;
	}
	
	public int getHeight(){
		int height1 = (int) (p0.y - p1.y + p3.y - p2.y)/2;
		int height2 = (int) (p1.y - p2.y + p0.y - p3.y)/2;
		if(height1>height2){
			return height1;
		} else {
			return height2;
		}
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Koordinat getPlacering() {
		return placering;
	}

	public void setPlacering(Koordinat placering) {
		this.placering = placering;
	}
	
	
}
