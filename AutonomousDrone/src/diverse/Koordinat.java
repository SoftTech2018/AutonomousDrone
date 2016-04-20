package diverse;

/**
 * Klasse der repræsenterer et koordinat i et OpgaveRum!
 * Hvert koordinat kan indeholde en mængde Genstande
 * 
 */
import java.util.ArrayList;

public class Koordinat {

	private int x;
	private int y;
	private ArrayList<Genstand> genstande = new ArrayList<>();
	private WallMarking marking = null;

	/**
	 * Opretter et nyt koordinat
	 * 
	 * @param x  sætter x værdien i det nye koordinat
	 * @param y  sætter y værdien i det nye koordinat
	 */

	public Koordinat(int x, int y){
		this.x = x;
		this.y = y;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public ArrayList<Genstand> getGenstande() {
		return genstande;
	}
	public void addGenstand(Genstand genstand){
		genstande.add(genstand);
	}
	public int getAntalGenstande(){
		int antal = genstande.size();
		return antal;
	}
	public void setMarking(WallMarking marking){
		this.marking = marking;
	}
	public WallMarking getMarking(){
		return marking;
	}

	public String toString(){
		return "("+x+","+y+")" + " indeholder" + getAntalGenstande() + ", " + genstande;
	}
}

