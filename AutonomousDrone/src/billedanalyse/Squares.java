package billedanalyse;


public class Squares {
	
	public enum FARVE {GRØN, RØD};
	
	private FARVE farve;
	public int x, y;
	
	public Squares(FARVE farve, int x, int y) {
		this.farve = farve;
		this.x = x;
		this.y = y;
	}

	public FARVE getFarve() {
		return farve;
	}

	public void setFarve(FARVE farve) {
		this.farve = farve;
	}

	@Override
	public String toString(){
		return farve.toString() + ": (" + x + "," + y + ")";
	}
}
