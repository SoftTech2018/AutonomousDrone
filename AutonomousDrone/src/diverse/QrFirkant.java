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

//		System.err.println("DeltaX: " + (xKor-360));
		return xKor - 360;
	}
	
	public int getHeight(){
		double height1 = Math.abs((p0.y - p3.y + p1.y - p2.y)/2);
		double height2 = Math.abs((p1.y - p0.y + p2.y - p3.y)/2);
		if(height1>height2){
//			System.err.println("Højde: " + Math.abs(height1));
			return (int) Math.abs(height1);
		} else {
//			System.err.println("Højde: " + Math.abs(height2));
			return (int) Math.abs(height2);
		}
	}
	
	public int getWidth(){
		double width1 = (p0.x - p3.x + p1.x - p2.x)/2;
		double width2 = (p1.x - p0.x + p2.x - p3.x)/2;
		if(width1>width2){
			return (int) Math.abs(width1);
		} else {
			return (int) Math.abs(width2);
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
	
	public Koordinat getCentrum(){
		double xKor = (p0.x+p1.x+p2.x+p3.x)/4;
		double yKor = (p0.y+p1.y+p2.y+p3.y)/4;
		return new Koordinat((int) xKor, (int) yKor);
	}
	
	public int getAreal(){
		double l1 = afstand(p0.x,p1.x,p0.y,p1.y);
		double l2 = afstand(p1.x,p2.x,p1.y,p2.y);
		int areal = (int) (l1*l2);
		return areal;
	}
	
	//Hjælpemetode til getAreal
	private double afstand(double x1, double x2, double y1, double y2){
		double result = Math.sqrt(Math.pow((x2-x1),2)+Math.pow((y2-y1),2));
		return result;
	}
	
	public Point getPoint0(){
		return p0;
	}
	
	public Point getPoint1(){
		return p1;
	}
	
	public Point getPoint2(){
		return p2;
	}
	
	public Point getPoint3(){
		return p3;
	}
	
}
