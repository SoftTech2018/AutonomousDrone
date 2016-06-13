package diverse.koordinat;

/**
 * Klasse der repræsenterer et koordinat i et OpgaveRum!
 * Hvert koordinat kan indeholde en mængde Genstande
 * 
 */
import java.util.ArrayList;

import diverse.circleCalc.Vector2;

public class Koordinat {

	Vector2 v;
	private int x;
	private int y;
	private Genstand genstand = null;
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
		v = new Vector2(x, y);
	}
	
	/**
	 * 
	 * @return Returnerer x værdien
	 */
	public int getX() {
		return x;
	}
	/**
	 * 
	 * @param x Sætter x værdien af koordinatet
	 */
	public void setX(int x) {
		this.x = x;
	}
	
	/**
	 * 
	 * @return Returnerer y værdien af koordinatet
	 */
	public int getY() {
		return y;
	}
	/**
	 * 
	 * @param x Sætter y værdien af koordinatet
	 */
	public void setY(int y) {
		this.y = y;
	}
	
	/**
	 * 
	 * @return Returnerer en ArrayListe med de genstande der er fundet
	 */
	public Genstand getGenstande() {
		return genstand;
	}
	/**
	 * 
	 * @param genstand TIlføjer en genstand til koordinatet
	 */
	public void addGenstand(Genstand genstand){
		this.genstand = genstand;
	}
	
	
	
	/**
	 * 
	 * @param marking Sætter vægmarkeringen
	 */
	public void setMarking(WallMarking marking){
		this.marking = marking;
	}
	
	/**
	 * 
	 * @return Returnerer en vægmarkering
	 */
	public WallMarking getMarking(){
		return marking;
	}

	public String toString(){
		return "("+x+","+y+")";
	}
	
	public Vector2 getVector(){
		return v;
	}

	public int dist(Koordinat koordinat) {
		int a = this.getX() - koordinat.getX();
		int b = this.getY() - koordinat.getY();
		return (int) Math.abs(Math.sqrt(a*a+b*b)); 
	}
	

}

