package billedanalyse;

public class Vektor {
	
	private Punkt x;
	private Punkt y;
	
	public Vektor(Punkt x, Punkt y){
		this.x = x;
		this.y = y;
	}

	public Punkt getX() {
		return x;
	}

	public void setX(Punkt x) {
		this.x = x;
	}

	public Punkt getY() {
		return y;
	}

	public void setY(Punkt y) {
		this.y = y;
	}
	
	

}
