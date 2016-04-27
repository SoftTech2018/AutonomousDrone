package billedanalyse;

import java.io.Serializable;

import org.opencv.core.Scalar;

public class ColorSetting implements Serializable{
	
	private static final long serialVersionUID = 3465766543705382416L;
	
	private double hueMin;
	private double hueMax;
	private double satMin;
	private double satMax;
	private double valMin;
	private double valMax;
	private Color color;
	
	public enum Color{RED, RED2, GREEN};

	public ColorSetting(double hueMin, double hueMax, double satMin, double satMax, double valMin, double valMax,
			Color color) {
		super();
		this.hueMin = hueMin;
		this.hueMax = hueMax;
		this.satMin = satMin;
		this.satMax = satMax;
		this.valMin = valMin;
		this.valMax = valMax;
		this.color = color;
		System.out.println(this.toString());
	}	
	
	public double getHueMin() {
		return hueMin;
	}

	public void setHueMin(double hueMin) {
		this.hueMin = hueMin;
	}



	public double getHueMax() {
		return hueMax;
	}



	public void setHueMax(double hueMax) {
		this.hueMax = hueMax;
	}



	public double getSatMin() {
		return satMin;
	}



	public void setSatMin(double satMin) {
		this.satMin = satMin;
	}



	public double getSatMax() {
		return satMax;
	}



	public void setSatMax(double satMax) {
		this.satMax = satMax;
	}



	public double getValMin() {
		return valMin;
	}



	public void setValMin(double valMin) {
		this.valMin = valMin;
	}



	public double getValMax() {
		return valMax;
	}



	public void setValMax(double valMax) {
		this.valMax = valMax;
	}



	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}
	
	@Override
	public String toString(){
		return "Farve: " + color + "\nMin: [" + hueMin + "," + satMin + "," + valMin + "]" + "\n" + "Max: [" + hueMax + "," + satMax + "," + valMax + "]";
	}
	
}
