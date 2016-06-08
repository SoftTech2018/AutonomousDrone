package diverse.koordinat;

public class Genstand {
	enum COLOR {GRØN, RØD}
	private COLOR farve;
	

	public Genstand(COLOR farve) {
		this.farve = farve;
	
	}

	public COLOR getFarve() {
		return farve;
	}

	public void setFarve(COLOR farve) {
		this.farve = farve;
	}
	
}
