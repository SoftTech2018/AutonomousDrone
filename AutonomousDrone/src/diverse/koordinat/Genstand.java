package diverse.koordinat;

public class Genstand {
	public enum GENSTAND_FARVE {GRØN, RØD}
	private GENSTAND_FARVE farve;
	

	public Genstand(GENSTAND_FARVE farve) {
		this.farve = farve;
	
	}

	public GENSTAND_FARVE getFarve() {
		return farve;
	}

	public void setFarve(GENSTAND_FARVE farve) {
		this.farve = farve;
	}
	
}
