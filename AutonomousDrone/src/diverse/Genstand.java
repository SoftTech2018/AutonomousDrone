package diverse;

public class Genstand {
	enum FARVE {GRØN, RØD}
	private FARVE farve;
	

	public Genstand(FARVE farve) {
		this.farve = farve;
	
	}

	public FARVE getFarve() {
		return farve;
	}

	public void setFarve(FARVE farve) {
		this.farve = farve;
	}
	
}
