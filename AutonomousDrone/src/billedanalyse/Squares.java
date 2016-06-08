package billedanalyse;


public class Squares {
	
	public enum FARVE {GRØN, RØD};
	
	private FARVE farve;
	public int x, y, yaw;
	public long tid;
	
	public Squares(FARVE farve, int x, int y, long tid, int yaw) {
		this.farve = farve;
		this.x = x;
		this.y = y;
		this.tid = tid;
		this.yaw = yaw;
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
	
	public long getTid(){
		return tid;
	}
	public long getYaw(){
		return yaw;
	}
}
